package no.nav.dagpenger.opplysning

import mu.KotlinLogging
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

// Regelverksdato: Datoen regelverket gjelder fra. Som hovedregel tidspunktet søknaden ble fremmet.

// Prøvingsdato: Dato som legges til grunn for når opplysninger som brukes av regelkjøringen skal være gyldige

// Virkningsdato: Dato som *behandlingen* finner til slutt

typealias Informasjonsbehov = Map<Opplysningstype<*>, List<Opplysning<*>>>

class Regelkjøring(
    private val regelverksdato: LocalDate,
    private val prøvingsperiode: Periode,
    private val opplysninger: Opplysninger,
    private val forretningsprosess: Forretningsprosess,
) {
    // Brukes kun av tester
    constructor(regelverksdato: LocalDate, opplysninger: Opplysninger, vararg regelsett: Regelsett) : this(
        regelverksdato = regelverksdato,
        prøvingsperiode = Periode(regelverksdato),
        opplysninger = opplysninger,
        forretningsprosess = Regelsettprosess(regelsett.toList(), regelsett.toList().flatMap { it.produserer }),
    )

    // Brukes av hendelser (uten prøvingsdato/periode)
    constructor(regelverksdato: LocalDate, opplysninger: Opplysninger, forretningsprosess: Forretningsprosess) : this(
        regelverksdato = regelverksdato,
        prøvingsperiode = Periode(regelverksdato),
        opplysninger = opplysninger,
        forretningsprosess = forretningsprosess,
    )

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val observers: MutableSet<RegelkjøringObserver> = mutableSetOf()

    private val regelsett get() = forretningsprosess.regelsett()

    private val alleRegler get() = regelsett.flatMap { it.regler(regelverksdato) }
    private val avhengighetsgraf = Avhengighetsgraf(alleRegler)

    private lateinit var opplysningerPåPrøvingsdato: LesbarOpplysninger

    private val ønsketResultat get() = forretningsprosess.ønsketResultat(opplysningerPåPrøvingsdato)

    // Finn bare regler som kreves for ønsket resultat
    // Kjører regler i topologisk rekkefølge
    private val gjeldendeRegler: List<Regel<*>> get() = alleRegler

    private var plan: MutableSet<Regel<*>> = mutableSetOf()
    private val kjørteRegler: MutableList<Regel<*>> = mutableListOf()

    private var trenger = setOf<Regel<*>>()

    init {
        val duplikate = gjeldendeRegler.groupBy { it.produserer }.filter { it.value.size > 1 }

        require(duplikate.isEmpty()) {
            "Regelsett inneholder flere regler som produserer samme opplysningstype. " +
                "Regler: ${duplikate.map { it.key.navn }}."
        }
    }

    fun registrer(observer: RegelkjøringObserver) {
        observers.add(observer)
    }

    fun evaluer(): Regelkjøringsrapport {
        val rapporter =
            prøvingsperiode.map {
                evaluerDag(it)
            }

        return rapporter.reduce { acc, regelkjøringsrapport ->
            Regelkjøringsrapport(
                kjørteRegler = acc.kjørteRegler + regelkjøringsrapport.kjørteRegler,
                mangler = acc.mangler + regelkjøringsrapport.mangler,
                informasjonsbehov = acc.informasjonsbehov + regelkjøringsrapport.informasjonsbehov,
                foreldreløse = acc.foreldreløse + regelkjøringsrapport.foreldreløse,
            )
        }
    }

    private fun evaluerDag(prøvingsdato: LocalDate): Regelkjøringsrapport {
        aktiverRegler(prøvingsdato)
        while (plan.isNotEmpty()) {
            kjørRegelPlan()
            aktiverRegler(prøvingsdato)
        }

        // Fjern opplysninger som ikke brukes for å produsere ønsket resultat
        val brukteOpplysninger = avhengighetsgraf.nødvendigeOpplysninger(opplysninger, ønsketResultat)
        opplysninger.fjernUbrukteOpplysninger(brukteOpplysninger)

        return Regelkjøringsrapport(
            kjørteRegler = kjørteRegler,
            mangler = trenger(),
            informasjonsbehov = informasjonsbehov(),
            foreldreløse = opplysninger.fjernet(),
        ).also { rapport ->
            observers.forEach { observer -> observer.evaluert(rapport, opplysningerPåPrøvingsdato) }
        }
    }

    private fun aktiverRegler(prøvingsdato: LocalDate) {
        opplysningerPåPrøvingsdato = opplysninger.forDato(prøvingsdato)
        val produksjonsplan = mutableSetOf<Regel<*>>()
        val produsenter = gjeldendeRegler.associateBy { it.produserer }
        val besøkt = mutableSetOf<Regel<*>>()

        ønsketResultat.forEach { opplysningstype ->
            val produsent =
                produsenter[opplysningstype]
                    ?: throw IllegalArgumentException("Fant ikke regel som produserer $opplysningstype")
            produsent.lagPlan(opplysningerPåPrøvingsdato, produksjonsplan, produsenter, besøkt)
        }

        val (ekstern, intern) = produksjonsplan.partition { it is Ekstern<*> }
        plan = intern.toMutableSet()
        trenger = ekstern.toSet()
    }

    private fun kjørRegelPlan() {
        while (plan.size > 0) {
            kjør(plan.first())
        }
    }

    private fun kjør(regel: Regel<*>) {
        try {
            val opplysning = regel.lagProdukt(opplysningerPåPrøvingsdato)
            kjørteRegler.add(regel)
            plan.remove(regel)
            opplysninger.leggTilUtledet(opplysning)
        } catch (e: IllegalArgumentException) {
            logger.info {
                """
                Skal kjøre: 
                ${plan.joinToString("\n") { it.produserer.navn }}
                Har kjørt: 
                ${kjørteRegler.joinToString("\n") { it.produserer.navn }}
                """.trimIndent()
            }
            throw e
        }
    }

    private fun trenger(): Set<Opplysningstype<*>> {
        val eksterneOpplysninger = trenger.map { it.produserer }.toSet()
        return eksterneOpplysninger
    }

    private fun informasjonsbehov(): Informasjonsbehov =
        trenger()
            .associateWith {
                // Finn regel som produserer opplysningstype og hent ut avhengigheter
                gjeldendeRegler.find { regel -> regel.produserer(it) }?.avhengerAv ?: emptyList()
            }.filter { (_, avhengigheter) ->
                // Finn bare opplysninger hvor alle avhengigheter er tilfredsstilt
                avhengigheter.all { opplysningerPåPrøvingsdato.har(it) }
            }.mapValues { (_, avhengigheter) ->
                // Finn verdien av avhengighetene
                avhengigheter.map { opplysningerPåPrøvingsdato.finnOpplysning(it) }
            }

    private class Regelsettprosess(
        val regelsett: List<Regelsett>,
        val opplysningstypes: List<Opplysningstype<*>> = regelsett.flatMap { it.produserer },
    ) : Forretningsprosess {
        override val regelverk: Regelverk
            get() = TODO("Not yet implemented")

        override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
            TODO("Not yet implemented")
        }

        override fun kontrollpunkter(): List<IKontrollpunkt> {
            TODO("Not yet implemented")
        }

        override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
            TODO("Not yet implemented")
        }

        override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
            TODO("Not yet implemented")
        }

        override fun regelsett() = regelsett

        override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> = opplysningstypes
    }

    data class Periode(
        private val start: LocalDate,
        private val endInclusive: LocalDate,
    ) : Iterable<LocalDate> {
        constructor(dag: LocalDate) : this(dag, dag)

        override fun iterator() =
            object : Iterator<LocalDate> {
                private var current = start

                override fun hasNext() = current <= endInclusive

                override fun next(): LocalDate = current.apply { current = current.plusDays(1) }
            }
    }
}

data class Regelkjøringsrapport(
    val kjørteRegler: List<Regel<*>>,
    val mangler: Set<Opplysningstype<*>>,
    val informasjonsbehov: Informasjonsbehov,
    val foreldreløse: List<Opplysning<*>>,
) {
    fun manglerOpplysninger(): Boolean = mangler.isNotEmpty()

    fun erFerdig(): Boolean = !manglerOpplysninger()
}

interface RegelkjøringObserver {
    fun evaluert(
        rapport: Regelkjøringsrapport,
        opplysninger: LesbarOpplysninger,
    )
}

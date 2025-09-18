package no.nav.dagpenger.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

// Regelverksdato: Datoen regelverket gjelder fra. Som hovedregel tidspunktet søknaden ble fremmet.

// Prøvingsdato: Dato som legges til grunn for når opplysninger som brukes av regelkjøringen skal være gyldige

// Virkningsdato: Dato som *behandlingen* finner til slutt

typealias Informasjonsbehov = Map<Opplysningstype<*>, Set<Opplysning<*>>>

class Regelkjøring(
    private val regelverksdato: LocalDate,
    private val prøvingsperiode: Periode,
    private val opplysninger: Opplysninger,
    private val forretningsprosess: Forretningsprosess,
    val opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger = { prøvingsdato -> forDato(prøvingsdato) },
) {
    // Brukes kun av tester
    constructor(
        regelverksdato: LocalDate,
        opplysninger: Opplysninger,
        vararg regelsett: Regelsett,
    ) : this(
        regelverksdato = regelverksdato,
        prøvingsperiode = Periode(regelverksdato),
        opplysninger = opplysninger,
        forretningsprosess = Regelsettprosess(regelsett.toList(), regelsett.toList().flatMap { it.produserer }),
    )

    // brukes av tester
    constructor(
        regelverksdato: LocalDate,
        opplysninger: Opplysninger,
        opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger,
        vararg regelsett: Regelsett,
    ) : this(
        regelverksdato = regelverksdato,
        prøvingsperiode = Periode(regelverksdato),
        opplysninger = opplysninger,
        forretningsprosess = Regelsettprosess(regelsett.toList(), regelsett.toList().flatMap { it.produserer }),
        opplysningerTilRegelkjøring,
    )

    // Brukes av hendelser (uten prøvingsdato/periode)
    constructor(
        regelverksdato: LocalDate,
        opplysninger: Opplysninger,
        forretningsprosess: Forretningsprosess,
        opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger,
    ) : this(
        regelverksdato = regelverksdato,
        prøvingsperiode = Periode(regelverksdato),
        opplysninger = opplysninger,
        forretningsprosess = forretningsprosess,
        opplysningerTilRegelkjøring,
    )

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val observatører: MutableSet<RegelkjøringObserver> = mutableSetOf()

    // Setter opp hvilke regler som skal gjelde
    private val gjeldendeRegler get() = forretningsprosess.regelsett().flatMap { it.regler(regelverksdato) }.toSet()
    private val avhengighetsgraf = Avhengighetsgraf(gjeldendeRegler)

    // Setter opp hvilke opplysninger som skal brukes når reglene evalurerer om de skal kjøre
    private lateinit var opplysningerPåPrøvingsdato: LesbarOpplysninger

    // Hvilke opplysninger som skal produseres. Må hentes på nytt hver gang, siden det kan endres etterhvert som nye regler kommer til
    private val ønsketResultat get() = forretningsprosess.ønsketResultat(opplysningerPåPrøvingsdato)

    // Set som brukes til å lage planen, og spore hva som blir gjort
    private var plan: MutableSet<Regel<*>> = mutableSetOf()
    private val kjørteRegler: MutableSet<Regel<*>> = mutableSetOf()
    private var trenger = setOf<Regel<*>>()

    init {
        val duplikate = gjeldendeRegler.groupBy { it.produserer }.filter { it.value.size > 1 }

        require(duplikate.isEmpty()) {
            "Regelsett inneholder flere regler som produserer samme opplysningstype. " +
                "Regler: ${duplikate.map { it.key.navn }}."
        }
    }

    fun leggTilObservatør(observer: RegelkjøringObserver) {
        observatører.add(observer)
    }

    fun evaluer(): Regelkjøringsrapport =
        prøvingsperiode
            .map { evaluerDag(it) }
            .reduce { total, regelkjøringsrapport -> total + regelkjøringsrapport }

    private fun evaluerDag(prøvingsdato: LocalDate): Regelkjøringsrapport {
        aktiverRegler(prøvingsdato)
        while (plan.isNotEmpty()) {
            kjørRegelPlan()
            aktiverRegler(prøvingsdato)
        }

        // Fjern opplysninger som ikke brukes for å produsere ønsket resultat
        val brukteOpplysninger = avhengighetsgraf.nødvendigeOpplysninger(opplysninger, ønsketResultat)
        opplysninger.fjernHvis { it.opplysningstype !in brukteOpplysninger }

        return Regelkjøringsrapport(
            kjørteRegler = kjørteRegler,
            mangler = trenger(),
            informasjonsbehov = informasjonsbehov(),
            foreldreløse = opplysninger.fjernet(),
        ).also { rapport ->
            observatører.forEach { observer ->
                val aktiveOpplysninger = opplysninger.kunEgne.forDato(prøvingsdato)
                observer.evaluert(
                    rapport,
                    opplysninger,
                    aktiveOpplysninger,
                )
            }
        }
    }

    private fun aktiverRegler(prøvingsdato: LocalDate) {
        opplysningerPåPrøvingsdato = opplysninger.opplysningerTilRegelkjøring(prøvingsdato)

        val toomy = avhengighetsgraf.finnAlleProdusenter(ønsketResultat)
        val lolert = toomy.filter { it.skalKjøre(opplysningerPåPrøvingsdato) }

        val (ekstern, intern) = lolert.partition { it is Ekstern<*> }
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
                avhengigheter.map { opplysningerPåPrøvingsdato.finnOpplysning(it) }.toSet()
            }

    private class Regelsettprosess(
        val regelsett: List<Regelsett>,
        val opplysningstypes: List<Opplysningstype<*>> = regelsett.flatMap { it.produserer },
    ) : Forretningsprosess(Regelverk(null, *regelsett.toTypedArray())) {
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
    val kjørteRegler: Set<Regel<*>>,
    val mangler: Set<Opplysningstype<*>>,
    val informasjonsbehov: Informasjonsbehov,
    val foreldreløse: Set<Opplysning<*>>,
) {
    fun manglerOpplysninger(): Boolean = mangler.isNotEmpty()

    fun erFerdig(): Boolean = !manglerOpplysninger()

    operator fun plus(other: Regelkjøringsrapport): Regelkjøringsrapport =
        Regelkjøringsrapport(
            kjørteRegler = this.kjørteRegler + other.kjørteRegler,
            mangler = this.mangler + other.mangler,
            informasjonsbehov = this.informasjonsbehov + other.informasjonsbehov,
            foreldreløse = this.foreldreløse + other.foreldreløse,
        )
}

interface RegelkjøringObserver {
    fun evaluert(
        rapport: Regelkjøringsrapport,
        alleOpplysninger: LesbarOpplysninger,
        aktiveOpplysninger: LesbarOpplysninger,
    )
}

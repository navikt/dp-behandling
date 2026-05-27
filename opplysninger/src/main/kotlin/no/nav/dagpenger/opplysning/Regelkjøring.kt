package no.nav.dagpenger.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

// Regelverksdato: Datoen regelverket gjelder fra. Som hovedregel tidspunktet søknaden ble fremmet.

// Prøvingsdato: Dato som legges til grunn for når opplysninger som brukes av regelkjøringen skal være gyldige

// Virkningsdato: Dato som *behandlingen* finner til slutt

typealias Informasjonsbehov = Map<Opplysningstype<*>, Set<Opplysning<*>>>

typealias Regelkart = Map<Opplysningstype<out Any>, Regel<*>>

typealias Regelkjøringsdato = Iterable<LocalDate>

class Regelkjøring(
    private val regelverksdato: LocalDate,
    private val prøvingsperiode: Regelkjøringsdato,
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
        forretningsprosess = Regelsettprosess(regelsett.toList(), regelsett.flatMapTo(mutableSetOf()) { it.produserer }),
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
        forretningsprosess = Regelsettprosess(regelsett.toList(), regelsett.flatMapTo(mutableSetOf()) { it.produserer }),
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

    // Setter opp hvilke opplysninger som skal brukes når reglene evaluerer om de skal kjøre
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

    fun evaluer(): Regelkjøringsrapport {
        var totalRapport: Regelkjøringsrapport? = null
        for (dato in prøvingsperiode) {
            val rapport = evaluerDag(dato)
            totalRapport = totalRapport?.plus(rapport) ?: rapport

            if (rapport.informasjonsbehov.isNotEmpty()) {
                // Om en dag sier den har behov må de løses før vi kan videre til neste dag
                break
            }
        }

        return totalRapport!!.also {
            if (it.prøvingsdato.size > 365) {
                logger.warn { "Kjørte på mer enn 365 datoer. Antall: ${it.prøvingsdato.size}" }
            }
            logger.info {
                """Kjørte ${it.kjørteRegler.size} regler for følgende datoer: ${it.prøvingsdato.joinToString(", ")}
                        |Regler:
                        |${it.kjørteRegler.joinToString("\n") { "- $it" }}
                """.trimMargin()
            }
        }
    }

    private var gjeldendePrøvingsdato: LocalDate = LocalDate.MIN

    private fun evaluerDag(prøvingsdato: LocalDate): Regelkjøringsrapport {
        gjeldendePrøvingsdato = prøvingsdato
        aktiverRegler(prøvingsdato)
        while (plan.isNotEmpty()) { // && trenger.isEmpty()) {
            kjørRegelPlan()
            aktiverRegler(prøvingsdato)
        }

        // Fjern utledede opplysninger som ikke brukes for å produsere ønsket resultat.
        // Guard: Ikke fjern opplysninger når det finnes uløste informasjonsbehov, fordi
        // ønsketResultat kan være ufullstendig (regelsett med skalKjøres=false pga manglende data).
        if (trenger.isEmpty()) {
            val brukteOpplysninger = avhengighetsgraf.nødvendigeOpplysninger(opplysninger, ønsketResultat)
            opplysninger.fjernHvis {
                val ikkeGjortAvSaksbehandler = it.kilde !is Saksbehandlerkilde
                val trengsIkke = it.opplysningstype !in brukteOpplysninger
                val tilhørerDenneRegelkjøringen = it.gyldighetsperiode.fraOgMed >= prøvingsdato || it.behandletVed == prøvingsdato

                ikkeGjortAvSaksbehandler && trengsIkke && tilhørerDenneRegelkjøringen
            }
        }

        opplysninger.markerBehandlet(prøvingsdato)

        return Regelkjøringsrapport(
            kjørteRegler = kjørteRegler,
            mangler = trenger(),
            informasjonsbehov = informasjonsbehov(),
            foreldreløse = opplysninger.fjernet(),
            prøvingsdato = listOf(prøvingsdato),
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

    private fun aktiverRegler(regelkjøringsdato: LocalDate) {
        opplysningerPåPrøvingsdato = opplysninger.opplysningerTilRegelkjøring(regelkjøringsdato)
        val produksjonsplan = mutableSetOf<Regel<*>>()
        val produsenter = forretningsprosess.produsenter(regelverksdato, opplysningerPåPrøvingsdato)
        val besøkt = mutableSetOf<Regel<*>>()

        // Kjør de reglene som skal kjøres
        ønsketResultat
            .mapNotNull { produsenter[it] }
            .forEach { produsent ->
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
            val opplysning = lagProdukt(regel)
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

    // Produserer en opplysning med riktig gyldighetsperiode basert på hva som allerede finnes.
    private fun <T : Any> lagProdukt(regel: Regel<T>): Opplysning<T> {
        val produkt = regel.lagProdukt(opplysningerPåPrøvingsdato)

        // Sjekk om vi har perioder av denne opplysningstypen i samme behandling fra før
        val eksisterendePerioder = opplysninger.kunEgne.finnAlle(regel.produserer).map { it.gyldighetsperiode }
        if (eksisterendePerioder.isEmpty()) return produkt

        // Regler uten avhengigheter (f.eks. somUtgangspunkt) produserer opplysninger med MIN..MAX periode.
        // Begrens perioden til å starte fra prøvingsdatoen for å unngå å overskrive eksisterende
        // opplysninger med smalere gyldighetsperioder (f.eks. satt av saksbehandler).
        val begrensetProdukt =
            if (produkt.gyldighetsperiode.erUbegrenset) {
                produkt.medGyldighetsperiode(Gyldighetsperiode(gjeldendePrøvingsdato))
            } else {
                produkt
            }

        // Trim den nye perioden slik at den ikke overlapper med eksisterende perioder.
        // Velg segmentet som inneholder prøvingsdatoen — det er den datoen vi evaluerer for.
        val ledigePerioder = begrensetProdukt.gyldighetsperiode.minus(eksisterendePerioder)
        val passendePeriode = ledigePerioder.firstOrNull { it.inneholder(gjeldendePrøvingsdato) }

        return passendePeriode?.let { begrensetProdukt.medGyldighetsperiode(it) } ?: begrensetProdukt
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

    fun kanKjøre(
        opplysningstype: Opplysningstype<*>,
        påDato: LocalDate?,
    ): Boolean {
        if (påDato == null || påDato == LocalDate.MIN) return true
        val regelsett = forretningsprosess.regelsett()
        val produserende = regelsett.single { it.produserer.contains(opplysningstype) }

        return produserende.skalKjøres(opplysninger.forDato(påDato))
    }

    private class Regelsettprosess(
        val regelsett: List<Regelsett>,
        val opplysningstypes: Set<Opplysningstype<*>> = regelsett.flatMapTo(mutableSetOf()) { it.produserer },
    ) : Forretningsprosess(Regelverk(navn = RegelverkType("Regelsettprosess"), regelsett = regelsett.toTypedArray())) {
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

        override fun produsenter(
            regelverksdato: LocalDate,
            opplysningerPåPrøvingsdato: LesbarOpplysninger,
        ) = regelsett.flatMap { it.regler(regelverksdato) }.associateBy { it.produserer }

        override fun ønsketResultat(opplysninger: LesbarOpplysninger): Set<Opplysningstype<*>> = opplysningstypes
    }

    data class Periode(
        private val start: LocalDate,
        private val endInclusive: LocalDate,
    ) : Iterable<LocalDate> {
        constructor(dag: LocalDate) : this(dag, dag)

        init {
            require(start != LocalDate.MIN) { "Periode.start kan ikke være LocalDate.MIN" }
            require(start != LocalDate.MAX) { "Periode.start kan ikke være LocalDate.MAX" }
            require(endInclusive != LocalDate.MIN) { "Periode.endInclusive $endInclusive kan ikke være LocalDate.MIN" }
            require(endInclusive != LocalDate.MAX) { "Periode.endInclusive $endInclusive kan ikke være LocalDate.MAX" }
            require(start <= endInclusive) { "Periode.endInclusive $endInclusive kan ikke være før Periode.start $start" }
        }

        override fun iterator() =
            object : Iterator<LocalDate> {
                private var current = start

                override fun hasNext() = current <= endInclusive

                override fun next(): LocalDate = current.apply { current = current.plusDays(1) }
            }
    }

    class Enkeltdager(
        private val datoer: Collection<LocalDate>,
    ) : Iterable<LocalDate> by datoer {
        constructor(vararg dato: LocalDate) : this(dato.toList())
    }
}

data class Regelkjøringsrapport(
    val kjørteRegler: Set<Regel<*>>,
    val mangler: Set<Opplysningstype<*>>,
    val informasjonsbehov: Informasjonsbehov,
    val foreldreløse: Set<Opplysning<*>>,
    val prøvingsdato: List<LocalDate>,
) {
    fun manglerOpplysninger(): Boolean = mangler.isNotEmpty()

    fun erFerdig(): Boolean = !manglerOpplysninger()

    operator fun plus(other: Regelkjøringsrapport): Regelkjøringsrapport =
        Regelkjøringsrapport(
            kjørteRegler = this.kjørteRegler + other.kjørteRegler,
            mangler = this.mangler + other.mangler,
            informasjonsbehov = this.informasjonsbehov + other.informasjonsbehov,
            foreldreløse = this.foreldreløse + other.foreldreløse,
            prøvingsdato = this.prøvingsdato + other.prøvingsdato,
        )
}

interface RegelkjøringObserver {
    fun evaluert(
        rapport: Regelkjøringsrapport,
        alleOpplysninger: LesbarOpplysninger,
        aktiveOpplysninger: LesbarOpplysninger,
    )
}

fun Regelkart.finn(opplysningstype: Opplysningstype<*>) =
    get(opplysningstype) ?: throw IllegalStateException("Fant ikke produsent for $opplysningstype")

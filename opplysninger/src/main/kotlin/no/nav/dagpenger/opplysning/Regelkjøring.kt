package no.nav.dagpenger.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Regelkjøring.Regelkjøringstilstand.Companion.aktiver
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

// Regelverksdato: Datoen regelverket gjelder fra. Som hovedregel tidspunktet søknaden ble fremmet.

// Prøvingsdato: Dato som legges til grunn for når opplysninger som brukes av regelkjøringen skal være gyldige

// Virkningsdato: Dato som *behandlingen* finner til slutt

typealias Informasjonsbehov = Map<Opplysningstype<*>, Set<Opplysning<*>>>

typealias Regelkart = Map<Opplysningstype<out Any>, Regel<*>>

typealias Regelkjøringsdato = Iterable<LocalDate>

class Regelplanlegger {
    private val regler = mutableSetOf<Regel<*>>()

    fun add(regel: Regel<*>) {
        regler.add(regel)
    }

    fun lagProduksjonsplan(): Produksjonsplan {
        val (ekstern, intern) = regler.partition { it is Ekstern<*> }
        return Produksjonsplan(ekstern = ekstern.toSet(), intern = intern.toSet())
    }
}

data class Produksjonsplan(
    val ekstern: Set<Regel<*>>,
    val intern: Set<Regel<*>>,
)

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

    private fun evaluerDag(prøvingsdato: LocalDate): Regelkjøringsrapport {
        val (kjøreplan, regelresultater) = planleggOgUtfør(prøvingsdato)

        val kjørteRegler = regelresultater.flatten().map { it.regel }.toSet()

        // Fjern utledede opplysninger som ikke brukes for å produsere ønsket resultat.
        // Guard: Ikke fjern opplysninger når det finnes uløste informasjonsbehov, fordi
        // ønsketResultat kan være ufullstendig (regelsett med skalKjøres=false pga manglende data).
        if (kjøreplan.siste.eksterneRegler.isEmpty()) {
            val brukteOpplysninger = avhengighetsgraf.nødvendigeOpplysninger(opplysninger, kjøreplan.siste.ønsketResultat)
            opplysninger.fjernHvis {
                val opplysningFraSaksbehandler = it.kilde is Saksbehandlerkilde
                val opplysningFraHendelse = it.kilde is Systemkilde
                val trengsIkke = it.opplysningstype !in brukteOpplysninger
                val tilhørerDenneRegelkjøringen = it.gyldighetsperiode.fraOgMed >= prøvingsdato || it.behandletVed == prøvingsdato

                if (opplysningFraSaksbehandler || opplysningFraHendelse) return@fjernHvis false

                trengsIkke && tilhørerDenneRegelkjøringen
            }
        }

        opplysninger.markerBehandlet(prøvingsdato)

        return Regelkjøringsrapport(
            kjørteRegler = kjørteRegler,
            mangler = kjøreplan.siste.trenger,
            informasjonsbehov = kjøreplan.siste.informasjonsbehov(gjeldendeRegler),
            fjernet = opplysninger.fjernet(),
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

    private class Kjøreplan(
        val siste: Regelkjøringstilstand,
        val historikk: List<Regelkjøringstilstand> = emptyList(),
    ) {
        fun skalKjøre() = siste.plan.isNotEmpty()

        fun kjørPlan(lesbarOpplysninger: LesbarOpplysninger) = siste.kjørRegelPlan(lesbarOpplysninger)

        fun nyPlan(regelkjøringstilstand: Regelkjøringstilstand): Kjøreplan {
            // loop detection
            if (regelkjøringstilstand.plan == siste.plan) {
                error("Går i loop! Planlegger samme plan vi har fra før")
            }
            return Kjøreplan(siste = regelkjøringstilstand, historikk = historikk.plusElement(siste))
        }
    }

    // Itererer plan -> kjør -> ny plan helt til ingen flere regler står for tur.
    // Den eneste muteringspunktet for `opplysninger` i regelkjøringen.
    private fun planleggOgUtfør(prøvingsdato: LocalDate): Pair<Kjøreplan, List<List<Regelkjøringstilstand.Regelkjøringutfall<*>>>> {
        var kjøreplan =
            Kjøreplan(
                siste = aktiver(prøvingsdato, regelverksdato, opplysninger, forretningsprosess, opplysningerTilRegelkjøring),
            )
        val regelresultater = mutableListOf<List<Regelkjøringstilstand.Regelkjøringutfall<*>>>()
        try {
            while (kjøreplan.skalKjøre()) {
                val resultater = kjøreplan.kjørPlan(opplysninger.kunEgne)
                regelresultater.add(resultater)
                resultater.forEach { (_, opplysning) ->
                    opplysninger.leggTilUtledet(opplysning)
                }
                kjøreplan =
                    kjøreplan.nyPlan(aktiver(prøvingsdato, regelverksdato, opplysninger, forretningsprosess, opplysningerTilRegelkjøring))
            }
            return kjøreplan to regelresultater.toList()
        } catch (err: RegelkjøringException) {
            logger.info {
                """
                Feil for med regel: ${err.regel.produserer.navn}
                Skal kjøre: 
                ${kjøreplan.siste.plan.joinToString("\n") { it.produserer.navn }}
                Har kjørt: 
                ${regelresultater.flatten().joinToString("\n") { it.regel.produserer.navn }}
                """.trimIndent()
            }
            throw err
        }
    }

    fun kanKjøre(
        opplysningstype: Opplysningstype<*>,
        påDato: LocalDate?,
    ): Boolean {
        if (påDato == null || påDato == LocalDate.MIN) return true
        val regelsett = forretningsprosess.regelsett()
        val produserende = regelsett.single { it.produserer.contains(opplysningstype) }
        val forDato = opplysninger.forDato(påDato)

        val regelsettKanKjøres = produserende.skalKjøres(forDato)
        val kanEndreEksisterende = forDato.har(opplysningstype)

        return regelsettKanKjøres || kanEndreEksisterende
    }

    private data class Regelkjøringstilstand(
        val prøvingsdato: LocalDate,
        val opplysningerPåPrøvingsdato: LesbarOpplysninger,
        val ønsketResultat: Set<Opplysningstype<*>>,
        val plan: Set<Regel<*>>,
        val eksterneRegler: Set<Regel<*>>,
    ) {
        val trenger: Set<Opplysningstype<*>> = eksterneRegler.map { it.produserer }.toSet()

        companion object {
            fun aktiver(
                prøvingsdato: LocalDate,
                regelverksdato: LocalDate,
                opplysninger: Opplysninger,
                forretningsprosess: Forretningsprosess,
                opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger,
            ): Regelkjøringstilstand {
                val opplysningerPåPrøvingsdato = opplysninger.opplysningerTilRegelkjøring(prøvingsdato)
                val produsenter = forretningsprosess.produsenter(regelverksdato, opplysningerPåPrøvingsdato)
                val ønsketResultat = forretningsprosess.ønsketResultat(opplysningerPåPrøvingsdato)

                val planlegger = Regelplanlegger()
                val besøkt = mutableSetOf<Regel<*>>()

                // Kjør de reglene som skal kjøres
                ønsketResultat
                    .mapNotNull { produsenter[it] }
                    .forEach { produsent ->
                        produsent.lagPlan(opplysningerPåPrøvingsdato, planlegger, produsenter, besøkt)
                    }

                val (ekstern, intern) = planlegger.lagProduksjonsplan()
                return Regelkjøringstilstand(
                    prøvingsdato = prøvingsdato,
                    opplysningerPåPrøvingsdato = opplysningerPåPrøvingsdato,
                    ønsketResultat = ønsketResultat,
                    plan = intern,
                    eksterneRegler = ekstern,
                )
            }
        }

        fun informasjonsbehov(gjeldendeRegler: Set<Regel<*>>): Informasjonsbehov =
            trenger
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

        fun kjørRegelPlan(egneOpplysninger: LesbarOpplysninger): List<Regelkjøringutfall<*>> =
            plan.map { regel ->
                try {
                    regel.kjørRegel(egneOpplysninger)
                } catch (e: IllegalArgumentException) {
                    throw RegelkjøringException(regel, e)
                }
            }

        private fun <T : Any> Regel<T>.kjørRegel(egneOpplysninger: LesbarOpplysninger): Regelkjøringutfall<T> {
            val produkt =
                this
                    .lagProdukt(opplysningerPåPrøvingsdato)
                    .medGyldighetsperiode(this, egneOpplysninger)

            return Regelkjøringutfall(
                regel = this,
                produkt = produkt,
            )
        }

        // Produserer en opplysning med riktig gyldighetsperiode.
        // Utgangspunkt-regler (og andre uten avhengigheter) produserer ubegrensede MIN..MAX perioder.
        // Disse begrenses alltid til å starte fra prøvingsdatoen — det finnes ingen gyldige caser
        // for at en Utgangspunkt-opplysning skal gjelde fra begynnelsen av tid.
        private fun <T : Any> Opplysning<T>.medGyldighetsperiode(
            regel: Regel<T>,
            egneOpplysninger: LesbarOpplysninger,
        ): Opplysning<T> {
            // Begrens ubegrensede perioder til å starte fra prøvingsdatoen.
            // Gjøres alltid, uavhengig av om det finnes eksisterende perioder.
            val begrensetProdukt =
                if (this.gyldighetsperiode.erUbegrenset) {
                    this.medGyldighetsperiode(Gyldighetsperiode(prøvingsdato))
                } else {
                    this
                }

            // Trim den nye perioden slik at den ikke overlapper med perioder i samme behandling.
            // Velg segmentet som inneholder prøvingsdatoen — det er den datoen vi evaluerer for.
            val eksisterendePerioder = egneOpplysninger.finnAlle(regel.produserer).map { it.gyldighetsperiode }
            if (eksisterendePerioder.isEmpty()) return begrensetProdukt

            val ledigePerioder =
                begrensetProdukt.gyldighetsperiode.minus(eksisterendePerioder).filter {
                    // Vi ønsker kun å fylle hull når perioden er etter prøvingsdato
                    it.fraOgMed.isAfter(begrensetProdukt.gyldighetsperiode.tilOgMed)
                }
            val passendePeriode = ledigePerioder.firstOrNull { it.inneholder(prøvingsdato) }

            return passendePeriode?.let { begrensetProdukt.medGyldighetsperiode(it) } ?: begrensetProdukt
        }

        data class Regelkjøringutfall<T : Any>(
            val regel: Regel<T>,
            val produkt: Opplysning<T>,
        )
    }

    private class RegelkjøringException(
        val regel: Regel<*>,
        override val cause: Throwable?,
    ) : RuntimeException(cause)

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
    val fjernet: Set<Opplysning<*>>,
    val prøvingsdato: List<LocalDate>,
) {
    fun manglerOpplysninger(): Boolean = mangler.isNotEmpty()

    fun erFerdig(): Boolean = !manglerOpplysninger()

    operator fun plus(other: Regelkjøringsrapport): Regelkjøringsrapport =
        Regelkjøringsrapport(
            kjørteRegler = this.kjørteRegler + other.kjørteRegler,
            mangler = this.mangler + other.mangler,
            informasjonsbehov = this.informasjonsbehov + other.informasjonsbehov,
            fjernet = this.fjernet + other.fjernet,
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

package no.nav.dagpenger.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Regelkjøring.Regelkjøringstilstand.Companion.lagKjøreplan
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.regel.TomRegel
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
        if (kjøreplan.siste.trenger.isEmpty()) {
            val brukteOpplysninger = avhengighetsgraf.nødvendigeOpplysninger(opplysninger, kjøreplan.siste.ønsketResultat)
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
            mangler = kjøreplan.siste.trenger,
            informasjonsbehov = kjøreplan.siste.informasjonsbehov(gjeldendeRegler),
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

    private data class Kjøreplan(
        val siste: Regelkjøringstilstand,
        val historikk: List<Regelkjøringstilstand> = emptyList(),
    ) {
        fun skalKjøre() = siste.kjørbarPlan.isNotEmpty()

        fun kjørPlan(
            opplysninger: Opplysninger,
            lesbarOpplysninger: LesbarOpplysninger,
        ): List<Regelkjøringstilstand.Regelkjøringutfall<*>> = siste.kjørRegelPlan(opplysninger, lesbarOpplysninger)

        fun nyPlan(regelkjøringstilstand: Regelkjøringstilstand): Kjøreplan {
            // loop detection
            if (regelkjøringstilstand.kjørbarPlan == siste.kjørbarPlan) {
                error("Går i loop! Planlegger samme plan vi har fra før")
            }
            return copy(siste = regelkjøringstilstand, historikk = historikk.plusElement(siste))
        }
    }

    // Itererer plan -> kjør -> ny plan helt til ingen flere regler står for tur.
    // Den eneste muteringspunktet for `opplysninger` i regelkjøringen.
    private fun planleggOgUtfør(prøvingsdato: LocalDate): Pair<Kjøreplan, List<List<Regelkjøringstilstand.Regelkjøringutfall<*>>>> {
        var kjøreplan = lagKjøreplan(prøvingsdato, regelverksdato, opplysninger, forretningsprosess, opplysningerTilRegelkjøring)
        val regelresultater = mutableListOf<List<Regelkjøringstilstand.Regelkjøringutfall<*>>>()
        try {
            while (kjøreplan.skalKjøre()) {
                val resultater = kjøreplan.kjørPlan(opplysninger, opplysninger.kunEgne)
                regelresultater.add(resultater)

                val opplysningerPåPrøvingsdato = opplysninger.opplysningerTilRegelkjøring(prøvingsdato)
                val reglerSomIkkeSkalKjøres = forretningsprosess.reglerSomIkkeSkalKjøres(opplysningerPåPrøvingsdato)

                val nyPlan =
                    kjøreplan.siste.copy(
                        opplysningerPåPrøvingsdato = opplysningerPåPrøvingsdato,
                        blokkerteRegler = reglerSomIkkeSkalKjøres,
                    )

                kjøreplan = kjøreplan.nyPlan(nyPlan)
            }
            return kjøreplan to regelresultater.toList()
        } catch (err: RegelkjøringException) {
            logger.info {
                """
                Feil for med regel: ${err.regel.produserer.navn}
                Skal kjøre: 
                ${kjøreplan.siste.kjørbarPlan.joinToString("\n") { it.produserer.navn }}
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

        return produserende.skalKjøres(opplysninger.forDato(påDato))
    }

    private data class Regelkjøringstilstand(
        val prøvingsdato: LocalDate,
        val opplysningerPåPrøvingsdato: LesbarOpplysninger,
        val ønsketResultat: Set<Opplysningstype<*>>,
        val regeltre: Set<TreNode<Regel<*>>>,
        val blokkerteRegler: Set<Regel<*>>,
    ) {
        /**
         * gitt at en regel skal kjøre, da må vi først kjøre alle avhengighetene dens.
         * planen er derfor et supersett av alle mulige regler som kan kjøre.
         */
        val plan =
            regeltre
                .map { regeltre -> regeltre.lagPlan(opplysningerPåPrøvingsdato, blokkerteRegler) }

        val kjørbarPlan = plan.flatMap { it.kjørbareRegler }.toSet()
        val trenger =
            plan
                .flatMap { it.reglerAvventendeData }
                .filterIsInstance<Ekstern<*>>()
                .map { it.produserer }
                .toSet()

        companion object {
            fun lagKjøreplan(
                prøvingsdato: LocalDate,
                regelverksdato: LocalDate,
                opplysninger: Opplysninger,
                forretningsprosess: Forretningsprosess,
                opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger,
            ): Kjøreplan {
                val produsenter = forretningsprosess.produsenter(regelverksdato)
                val opplysningerPåPrøvingsdato = opplysninger.opplysningerTilRegelkjøring(prøvingsdato)

                val ønsketResultat = forretningsprosess.ønsketResultat(opplysningerPåPrøvingsdato)

                val regeltre =
                    ønsketResultat
                        .map { produsenter[it] ?: error("Har ikke produsent for ønsket resultat: $it") }
                        .map { it.regeltre(produsenter) }
                        .toSet()

                val reglerSomIkkeSkalKjøres = forretningsprosess.reglerSomIkkeSkalKjøres(opplysningerPåPrøvingsdato)

                return Kjøreplan(
                    siste =
                        Regelkjøringstilstand(
                            prøvingsdato = prøvingsdato,
                            opplysningerPåPrøvingsdato = opplysningerPåPrøvingsdato,
                            ønsketResultat = ønsketResultat,
                            regeltre = regeltre,
                            blokkerteRegler = reglerSomIkkeSkalKjøres,
                        ),
                )
            }

            /**
             * trimmer avhengighetsgreiner som ikke trengs (HvisSannMedResultat)
             */
            private fun TreNode<Regel<*>>.medFaktiskeAvhengigheter(opplysninger: LesbarOpplysninger): TreNode<Regel<*>> {
                val effektive = verdi.effektiveAvhengigheter(opplysninger).toSet()
                val filtrerteBarn =
                    avhengigheter
                        .filter { it.verdi.produserer in effektive }
                        .map { it.medFaktiskeAvhengigheter(opplysninger) }
                return copy(avhengigheter = filtrerteBarn)
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

        fun kjørRegelPlan(
            opplysninger: Opplysninger,
            egneOpplysninger: LesbarOpplysninger,
        ): List<Regelkjøringutfall<*>> =
            kjørbarPlan.map { regel ->
                try {
                    regel.kjørRegel(egneOpplysninger).also {
                        // XXX: MUTERING PÅGÅR
                        opplysninger.leggTilUtledet(it.produkt)
                        // XXX: MUTERING FERDIG
                    }
                } catch (e: RuntimeException) {
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

        // Produserer en opplysning med riktig gyldighetsperiode basert på hva som allerede finnes.
        private fun <T : Any> Opplysning<T>.medGyldighetsperiode(
            regel: Regel<T>,
            egneOpplysninger: LesbarOpplysninger,
        ): Opplysning<T> {
            // Sjekk om vi har perioder av denne opplysningstypen i samme behandling fra før
            val eksisterendePerioder = egneOpplysninger.finnAlle(regel.produserer).map { it.gyldighetsperiode }
            if (eksisterendePerioder.isEmpty()) return this

            // Regler uten avhengigheter (f.eks. somUtgangspunkt) produserer opplysninger med MIN..MAX periode.
            // Begrens perioden til å starte fra prøvingsdatoen for å unngå å overskrive eksisterende
            // opplysninger med smalere gyldighetsperioder (f.eks. satt av saksbehandler).
            val begrensetProdukt =
                if (this.gyldighetsperiode.erUbegrenset) {
                    this.medGyldighetsperiode(Gyldighetsperiode(prøvingsdato))
                } else {
                    this
                }

            // Trim den nye perioden slik at den ikke overlapper med eksisterende perioder.
            // Velg segmentet som inneholder prøvingsdatoen — det er den datoen vi evaluerer for.
            val ledigePerioder = begrensetProdukt.gyldighetsperiode.minus(eksisterendePerioder)
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

internal fun TreNode<Regel<*>>.lagPlan(
    opplysninger: LesbarOpplysninger,
    blokkerteRegler: Collection<Regel<*>> = emptyList(),
): Kjøreplanresultat =
    this
        .somRegelnode()
        .flaggReglerSomMåKjøres(opplysninger)
        .flaggReglerSomErBlokkert(blokkerteRegler)
        // trenger kun kjøre hele treet hvis roten må kjøre, ellers er det ikke vits
        .takeIf { it.verdi.kanKjøre }
        ?.topologisk()
        // kun de som må kjøres, er ikke gitt at hele treet skal kjøres
        ?.filter { it.verdi.kanKjøre }
        ?.let {
            val (kjørbareRegler, reglerAvventendeData) = it.partition { it.verdi.kanKjøringGjennomføres }
            Kjøreplanresultat(
                kjørbareRegler = kjørbareRegler.map { it.verdi.regel }.toSet(),
                reglerAvventendeData = reglerAvventendeData.map { it.verdi.regel }.toSet(),
            )
        }
        ?: Kjøreplanresultat(emptySet(), emptySet())

data class Kjøreplanresultat(
    val kjørbareRegler: Set<Regel<*>>,
    val reglerAvventendeData: Set<Regel<*>>,
)

private fun TreNode<Regel<*>>.somRegelnode(): TreNode<Regelnode> =
    TreNode(
        Regelnode(verdi),
        avhengigheter =
            avhengigheter.map {
                it.somRegelnode()
            },
    )

private fun TreNode<Regelnode>.flaggReglerSomErBlokkert(blokkerteRegler: Collection<Regel<*>>): TreNode<Regelnode> {
    fun TreNode<Regelnode>.erBlokkert(): Boolean {
        if (this.verdi.erBlokkert) return true
        return this.avhengigheter.any { it.erBlokkert() }
    }

    val avhengigheter = avhengigheter.map { it.flaggReglerSomErBlokkert(blokkerteRegler) }
    val harBlokkertAvhengighet = avhengigheter.any { it.erBlokkert() }
    return copy(
        verdi = verdi.copy(erBlokkert = blokkerteRegler.contains(verdi.regel) || harBlokkertAvhengighet),
        avhengigheter = avhengigheter,
    )
}

private fun TreNode<Regelnode>.flaggReglerSomMåKjøres(opplysninger: LesbarOpplysninger): TreNode<Regelnode> {
    val faktiskeAvhengigheter = verdi.regel.effektiveAvhengigheter(opplysninger).toSet()
    val uavklarteTyper = verdi.regel.uavklarteAvhengigheter(opplysninger).toSet()

    val uavklarteAvhengigheter = avhengigheter.filter { it.verdi.regel.produserer in uavklarteTyper }
    val avhengigheter =
        avhengigheter
            .filter { it.verdi.regel.produserer in faktiskeAvhengigheter }
            .map { it.flaggReglerSomMåKjøres(opplysninger) }

    val produkt = opplysninger.finnNullableOpplysning(verdi.regel.produserer)
    val opplysningerUtledetAv = produkt?.utledetAv?.opplysninger
    // sjekker ikke om regelen selv sin opplysning er utdatert 🤔
    val harUtdaterteAvhengigheter = opplysningerUtledetAv?.any { it.erUtdatert } == true
    val harErstattetAvhengighet = opplysninger.erErstattet(opplysningerUtledetAv ?: emptyList())

    fun TreNode<Regelnode>.måKjøre(): Boolean {
        if (this.verdi.kjøreflagg.måKjøres()) return true
        return this.avhengigheter.any { it.måKjøre() }
    }

    // hvis en avhengighet tidligere i kjeden er planlagt skal vi også kjøre
    val avhengighetSkalKjøre = avhengigheter.any { it.måKjøre() }
    val avhengighetAvventerData = avhengigheter.any { it.verdi.avventerData }

    val harFåttNyeAvhengigheterIKode =
        opplysningerUtledetAv != null &&
            this.verdi.regel.avhengerAv
                .toSet() != opplysningerUtledetAv.map { it.opplysningstype }.toSet()

    val kjøreflagg =
        when {
            verdi.regel is TomRegel -> Regelnode.Kjøreflagg.INGEN_KJØRING_NØDVENDIG
            produkt == null -> Regelnode.Kjøreflagg.MANGLER_PRODUKT
            harUtdaterteAvhengigheter -> Regelnode.Kjøreflagg.HAR_UTDATERT_AVHENGIGHET
            harErstattetAvhengighet -> Regelnode.Kjøreflagg.HAR_ERSTATTET_AVHENGIGHET
            avhengighetSkalKjøre -> Regelnode.Kjøreflagg.AVHENGIGHET_MÅ_KJØRE
            // harFåttNyeAvhengigheterIKode -> Regelnode.Kjøreflagg.HAR_FÅTT_ENDRET_AVHENGIGHETER_I_KODE
            else -> Regelnode.Kjøreflagg.INGEN_KJØRING_NØDVENDIG
        }
    val avventerData =
        when (verdi.regel) {
            is Ekstern<*> -> kjøreflagg.måKjøres()
            else -> avhengighetAvventerData || uavklarteAvhengigheter.isNotEmpty()
        }
    return copy(
        verdi =
            verdi.copy(
                avventerData = avventerData,
                kjøreflagg = kjøreflagg,
                uavklarteAvhengigheter = uavklarteAvhengigheter.map { it.verdi.regel }.toSet(),
            ),
        avhengigheter = avhengigheter,
    )
}

private data class Regelnode(
    val regel: Regel<*>,
    val erBlokkert: Boolean = false,
    val avventerData: Boolean = false,
    val kjøreflagg: Kjøreflagg = Kjøreflagg.INGEN_KJØRING_NØDVENDIG,
    val uavklarteAvhengigheter: Set<Regel<*>> = emptySet(),
) {
    val kanKjøre = kjøreflagg.måKjøres() && !erBlokkert
    val kanKjøringGjennomføres = kanKjøre && !avventerData

    enum class Kjøreflagg {
        INGEN_KJØRING_NØDVENDIG,
        MANGLER_PRODUKT,
        HAR_UTDATERT_AVHENGIGHET,
        HAR_ERSTATTET_AVHENGIGHET,
        AVHENGIGHET_MÅ_KJØRE,
        HAR_FÅTT_ENDRET_AVHENGIGHETER_I_KODE,
        ;

        fun måKjøres() =
            when (this) {
                INGEN_KJØRING_NØDVENDIG -> false

                MANGLER_PRODUKT,
                HAR_UTDATERT_AVHENGIGHET,
                HAR_ERSTATTET_AVHENGIGHET,
                AVHENGIGHET_MÅ_KJØRE,
                HAR_FÅTT_ENDRET_AVHENGIGHETER_I_KODE,
                -> true
            }
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

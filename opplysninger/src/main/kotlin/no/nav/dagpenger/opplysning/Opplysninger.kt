package no.nav.dagpenger.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.util.UUID

class Opplysninger private constructor(
    override val id: UUID,
    initielleOpplysninger: List<Opplysning<*>>,
    basertPå: Opplysninger? = null,
) : LesbarOpplysninger {
    constructor() : this(UUIDv7.ny(), emptyList(), null)
    private constructor(id: UUID, opplysninger: List<Opplysning<*>>) : this(id, opplysninger, null)

    // Rekkefølgen er viktig, så vi sorterer på id for å få en konsistent rekkefølge
    private val egne: MutableList<Opplysning<*>> = initielleOpplysninger.sortedBy { it.id }.toMutableList()
    private val fjernet: MutableList<Opplysning<*>> = mutableListOf()
    private val erstattet: MutableSet<UUID> get() = alleOpplysninger.mapNotNull { it.erstatter }.map { it.id }.toMutableSet()

    // Id-er på opplysninger som er "tombstonet" via lagTombstone(): de finnes fortsatt i
    // alleOpplysninger (for sporbarhet/erstatter-kjeden), men skal ikke telle som en gyldig verdi
    // for regelmotoren. Med vilje IKKE persistert - om behandlingen rehydreres fra database
    // midt i en rekjør-flyt (før ny opplysning er lagt til), mister vi denne markeringen, men det
    // er greit: regelmotoren vil da bare be om opplysningen på nytt, som er idempotent.
    private val tombstonet: MutableSet<UUID> = mutableSetOf()

    private val basertPåOpplysninger: List<Opplysning<*>> =
        basertPå?.let { (it.basertPåOpplysninger + it.egne).filter { opplysning -> opplysning.skalArves } } ?: emptyList()

    private val alleOpplysninger = CachedList { (basertPåOpplysninger + egne).utenErstattet() }

    private var alleOpplysningerMap = alleOpplysninger.groupBy { it.opplysningstype }

    override val kunEgne: LesbarOpplysninger get() = OpplysningerView(this, bareEgne = true)

    private fun refreshOpplysninger() {
        val oppfrisketOpplysninger = alleOpplysninger.refresh()
        alleOpplysningerMap = oppfrisketOpplysninger.groupBy { it.opplysningstype }
    }

    fun <T : Any> leggTil(opplysning: Opplysning<T>) {
        leggTilIntern(opplysning)
        refreshOpplysninger()
    }

    fun leggTilAlle(opplysninger: List<Opplysning<*>>) {
        opplysninger.forEach { leggTilIntern(it) }
        refreshOpplysninger()
    }

    private fun <T : Any> leggTilIntern(opplysning: Opplysning<T>) {
        opplysning.behandlet = false
        opplysning.behandletVed = null
        val eksisterende = finnNullableOpplysning(opplysning.opplysningstype, opplysning.gyldighetsperiode)

        if (eksisterende != null) {
            if (egne.contains(eksisterende)) {
                // Erstatt hele opplysningen
                fjern(eksisterende)

                eksisterende.erstatter?.let {
                    // Om den eksisterende opplysningen erstatter noe, så må den nye også erstatte den samme
                    opplysning.erstatter(it)

                    // Marker alle opplysninger som er utledet av den erstattede som utdaterte
                    markerUtledningerSomUtdatert(it)
                }
            }

            if (basertPåOpplysninger.contains(eksisterende)) {
                opplysning.erstatter(eksisterende)
                markerUtledningerSomUtdatert(eksisterende)
            }
        }

        sjekkAtUtledetAvFinnes(opplysning)

        egne.add(opplysning)
    }

    // Invariant: en opplysning kan ikke være utledetAv noe som ikke finnes (lenger) i denne
    // Opplysninger-instansen. Dette kan skje om en avhengighet blir fjernet (f.eks. fordi den selv
    // ble erstattet) etter at denne opplysningen ble beregnet, men før den blir lagt til. En slik
    // opplysning peker da på en «foreldreløs» avhengighet, som til syvende og sist gir et
    // brudd på fremmednøkkelen mot opplysning-tabellen når vi lagrer til database.
    private fun sjekkAtUtledetAvFinnes(opplysning: Opplysning<*>) {
        opplysning.utledetAv?.opplysninger?.forEach { avhengighet ->
            check(alleOpplysninger.contains(avhengighet)) {
                "Opplysning ${opplysning.id} (${opplysning.opplysningstype.navn}) er utledetAv " +
                    "${avhengighet.id} (${avhengighet.opplysningstype.navn}), men denne finnes ikke " +
                    "(lenger) i opplysningene. Avhengigheten kan ha blitt fjernet fordi den selv ble " +
                    "erstattet før denne opplysningen ble lagt til."
            }
        }
    }

    private fun markerUtledningerSomUtdatert(eksisterende: Opplysning<*>) {
        val graf = OpplysningGraf(alleOpplysninger)
        val avhengigheter = graf.hentAlleUtledetAv(eksisterende)

        avhengigheter.forEach { it.erUtdatert = true }
    }

    override fun erErstattet(opplysninger: List<Opplysning<*>>) = opplysninger.any { it.id in erstattet }

    fun markerBehandlet(dato: LocalDate) {
        egne
            .filter { it.gyldighetsperiode.inneholder(dato) }
            .forEach {
                it.behandlet = true
                // Kun utledede opplysninger trenger behandletVed-sporing for cleanup.
                // Input-opplysninger (fra behov) skal ikke fjernes av behandletVed-sjekken.
                if (it.utledetAv != null && it.behandletVed == null) it.behandletVed = dato
            }
    }

    fun ubehandledeDatoer(): List<LocalDate> =
        egne
            .filter { !it.behandlet && !it.gyldighetsperiode.fraOgMed.isEqual(LocalDate.MIN) }
            .map { it.gyldighetsperiode.fraOgMed }
            .distinct()
            .sorted()

    override fun <T : Any> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> =
        finnNullableOpplysning(opplysningstype) ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig")

    override fun <T : Any> finnOpplysning(
        opplysningstype: Opplysningstype<T>,
        gjelderFor: LocalDate,
    ): Opplysning<T> =
        finnNullableOpplysningMedFiltre(opplysningstype, gjelderFor, false)
            ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig for $gjelderFor")

    override fun <T : Any> finnNullableOpplysning(opplysningstype: Opplysningstype<T>) =
        alleOpplysningerMap[opplysningstype]
            ?.filterIsInstance<Opplysning<T>>()
            ?.lastOrNull { !erTombstonet(it.id) }

    override fun finnOpplysning(opplysningId: UUID) =
        alleOpplysninger.lastOrNull { it.id == opplysningId }
            ?: throw OpplysningIkkeFunnetException("Har ikke opplysning med id=$opplysningId")

    override fun <T : Any> har(opplysningstype: Opplysningstype<T>) =
        alleOpplysninger.any { it.er(opplysningstype) && !erTombstonet(it.id) }

    override fun <T : Any> har(
        opplysningstype: Opplysningstype<T>,
        gjelderFor: LocalDate,
    ) = finnNullableOpplysningMedFiltre(opplysningstype, gjelderFor, false) != null

    override fun finnFlere(opplysningstyper: List<Opplysningstype<*>>) =
        opplysningstyper.mapNotNull { type -> alleOpplysninger.lastOrNull { it.er(type) && !erTombstonet(it.id) } }

    override fun <T : Any> finnAlle(opplysningstyper: List<Opplysningstype<T>>) = opplysningstyper.flatMap { type -> finnAlle(type) }

    override fun <T : Any> finnAlle(opplysningstype: Opplysningstype<T>) =
        alleOpplysninger.filter { it.er(opplysningstype) && !erTombstonet(it.id) }.filterIsInstance<Opplysning<T>>()

    override fun forDato(gjelderFor: LocalDate): LesbarOpplysninger = OpplysningerView(this, gjelderFor = gjelderFor)

    override fun somListe(filter: Filter) =
        when (filter) {
            Filter.Alle -> alleOpplysninger
            Filter.Egne -> egne
        }.utenErstattet()

    fun baserPå(tidligereOpplysninger: Opplysninger?) = Opplysninger(id, egne, tidligereOpplysninger)

    fun fjernet(): Set<Opplysning<*>> = fjernet.toSet()

    fun fjernHvis(block: (Opplysning<*>) -> Boolean) =
        egne.filter { block(it) }.forEach { fjern(it, false) }.also {
            // Oppdaterer alleOpplysninger etter at opplysninger er fjernet
            refreshOpplysninger()
        }

    fun fjern(opplysningId: UUID) =
        fjern(
            egne.lastOrNull { it.id == opplysningId }
                ?: throw OpplysningIkkeFunnetException("Har ikke egen opplysning med id=$opplysningId"),
        )

    fun fjern(opplysningTypeId: Opplysningstype<*>) =
        fjern(
            egne.lastOrNull { it.opplysningstype.id.uuid == opplysningTypeId.id.uuid }
                ?: throw OpplysningIkkeFunnetException("Har ingen opplysning med opplysningTypeId=${opplysningTypeId.id.uuid}"),
        )

    private fun fjern(
        opplysning: Opplysning<*>,
        skalOppfriske: Boolean = true,
    ) {
        // Fjern alle opplysninger som er utledet av opplysningen som fjernes
        fjernAvhengigheter(opplysning)

        if (egne.remove(opplysning)) {
            fjernet.add(opplysning)
        }

        if (skalOppfriske) refreshOpplysninger()
    }

    private fun fjernAvhengigheter(eksisterende: Opplysning<*>) {
        val graf = OpplysningGraf(egne.toList())
        val avhengigheter = graf.hentAlleUtledetAv(eksisterende)
        avhengigheter.forEach { avhengighet -> fjern(avhengighet, false) }
    }

    private fun <T : Any> finnNullableOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    ): Opplysning<T>? {
        val opplysninger =
            alleOpplysningerMap[opplysningstype]
                ?.filterIsInstance<Opplysning<T>>()
                ?.filter { it.gyldighetsperiode.overlapper(gyldighetsperiode) }

        return opplysninger?.lastOrNull()
    }

    private fun Collection<Opplysning<*>>.utenErstattet(): List<Opplysning<*>> {
        val bearbeidet =
            this
                .groupBy { it.opplysningstype }
                .mapValues { (_, perioder) ->
                    val sortert = perioder.sortedBy { it.gyldighetsperiode.fraOgMed }
                    val resultat = mutableListOf<Opplysning<*>>()

                    sortert.forEach { utfordrer ->
                        val forrige = resultat.lastOrNull()

                        when {
                            forrige == null -> {
                                resultat.add(utfordrer)
                            }

                            forrige.gyldighetsperiode.erFør(utfordrer.gyldighetsperiode) ||
                                forrige.gyldighetsperiode.tilstøter(utfordrer.gyldighetsperiode) -> {
                                resultat.add(utfordrer)
                            }

                            // det er overlapp, men forrige er nyest
                            forrige.id > utfordrer.id -> {}

                            // utfordrer er nyest, og overskriver hele forrige
                            forrige.gyldighetsperiode.fraOgMed == utfordrer.gyldighetsperiode.fraOgMed -> {
                                resultat[resultat.lastIndex] = utfordrer
                            }

                            else -> {
                                val forkortet = forrige.forkortetTil(utfordrer)
                                logger.debug {
                                    """
                                        |Kant-i-kant overlapper opplysning ${forrige.id} og ${utfordrer.id} for type ${forrige.opplysningstype.navn}. Lager forkortet opplysning.
                                        |Venstre: ${forrige.gyldighetsperiode}
                                        |Høyre: ${utfordrer.gyldighetsperiode}
                                        |Forkortet: ${forkortet.gyldighetsperiode}
                                    """.trimMargin()
                                }
                                resultat[resultat.lastIndex] = forkortet
                                resultat.add(utfordrer)
                            }
                        }
                    }
                    resultat
                }

        // Sorter opplysningene i samme rekkefølge som de var i før bearbeiding
        return this
            .mapNotNull { opplysning ->
                bearbeidet[opplysning.opplysningstype]?.takeIf { it.isNotEmpty() }?.removeFirst()
            }.sortedBy { it.id }
    }

    fun inneholder(opplysning: Opplysning<*>): Boolean = alleOpplysninger.contains(opplysning)

    // Interne hjelpemetoder for OpplysningerView
    internal fun hentOpplysninger(bareEgne: Boolean): List<Opplysning<*>> =
        if (bareEgne) egne.utenErstattet() else alleOpplysninger.toList()

    internal fun <T : Any> finnNullableOpplysningMedFiltre(
        opplysningstype: Opplysningstype<T>,
        gjelderFor: LocalDate?,
        bareEgne: Boolean,
    ): Opplysning<T>? {
        val kandidater =
            if (bareEgne) {
                egne
                    .filter { it.er(opplysningstype) && !erTombstonet(it.id) }
                    .filterIsInstance<Opplysning<T>>()
            } else {
                alleOpplysningerMap[opplysningstype]
                    ?.filterIsInstance<Opplysning<T>>()
                    ?.filterNot { erTombstonet(it.id) }
                    ?: emptyList()
            }
        return if (gjelderFor != null) {
            kandidater.lastOrNull { it.gyldighetsperiode.inneholder(gjelderFor) }
        } else {
            kandidater.lastOrNull()
        }
    }

    // Fast-path som unngår et hash-oppslag mot `tombstonet` i det normale tilfellet der
    // ingenting er tombstonet (dvs. nesten alltid) - `isEmpty()` er billigere enn et
    // sett-oppslag, og dette kalles for hver opplysning i hvert kall til
    // finnNullableOpplysning/har/finnAlle m.fl. under regelkjøring.
    internal fun erTombstonet(opplysningId: UUID): Boolean = tombstonet.isNotEmpty() && opplysningId in tombstonet

    fun erArvet(opplysning: Opplysning<*>): Boolean = basertPåOpplysninger.contains(opplysning)

    /**
     * Oppfrisker [opplysningstype]: sørger for at opplysningstypen fremstår som manglende for
     * regelmotoren, slik at en `Ekstern`-regel (f.eks. `innhentMed`) naturlig blir planlagt på
     * nytt og ber om et nytt behov.
     *
     * - Egne opplysninger av typen (lagt til i *denne* behandlingen, f.eks. av saksbehandler
     *   eller et tidligere gjenopptak) kan fjernes reelt med [fjern] - de eies av denne
     *   behandlingen og kan trygt fjernes.
     * - Arvede opplysninger (fra en tidligere behandling i kjeden) kan vi derimot aldri fjerne -
     *   en revurdering kan bare legge til nye opplysninger. Vi legger derfor ikke til noe nytt her
     *   heller: id-en til den arvede opplysningen registreres direkte i [tombstonet], slik at
     *   verdi-oppslag (se [finnNullableOpplysning], [har], [finnAlle] m.fl.) hopper over den, uten
     *   å bryte regelen om at en revurdering bare kan legge til nye opplysninger.
     *
     * Merk at [tombstonet] bevisst ikke er persistert: om behandlingen rehydreres fra database
     * midt i en rekjør-flyt, mister vi markeringen og opplysningen blir synlig igjen - men
     * regelmotoren vil da bare be om den på nytt, noe som er trygt og idempotent.
     */
    fun lagTombstone(opplysningstype: Opplysningstype<*>) {
        egne
            .filter { it.er(opplysningstype) }
            .forEach { fjern(it, false) }

        basertPåOpplysninger
            .filter { it.er(opplysningstype) }
            .forEach { tombstonet.add(it.id) }

        refreshOpplysninger()
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        fun med(opplysninger: Collection<Opplysning<*>>) = Opplysninger(UUIDv7.ny(), opplysninger.toList())

        fun med(vararg opplysning: Opplysning<*>) = Opplysninger(UUIDv7.ny(), opplysning.toList())

        fun basertPå(andre: Opplysninger) = Opplysninger(UUIDv7.ny(), emptyList(), andre)

        fun rehydrer(
            id: UUID,
            opplysninger: List<Opplysning<*>>,
        ) = Opplysninger(id, opplysninger, null)

        fun Opplysning<*>.forkortetTil(utfordrer: Opplysning<*>): Opplysning<*> {
            val segmenter = gyldighetsperiode - utfordrer.gyldighetsperiode
            val forkortetPeriode =
                segmenter.firstOrNull { it.erFør(utfordrer.gyldighetsperiode) }
                    ?: throw IllegalArgumentException(
                        "Kan ikke forkorte $gyldighetsperiode fram til ${utfordrer.gyldighetsperiode}",
                    )
            return medGyldighetsperiode(forkortetPeriode).apply {
                erUtdatert = utfordrer.erUtdatert
            }
        }

        fun Collection<Opplysning<*>>.sisteEndring() =
            this
                .flatMap { listOf(it.gyldighetsperiode.fraOgMed, it.gyldighetsperiode.tilOgMed) }
                .filterNot { it == LocalDate.MIN || it == LocalDate.MAX }
                .max()
    }
}

class OpplysningIkkeFunnetException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)

class DuplikateOpplysningerException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)

internal inline fun <T, K> Iterable<T>.distinctByLast(selector: (T) -> K): List<T> {
    val map = LinkedHashMap<K, T>()
    for (element in this) {
        val key = selector(element)
        map.remove(key) // Fjern først for å oppdatere posisjon
        map[key] = element
    }
    return map.values.toList()
}

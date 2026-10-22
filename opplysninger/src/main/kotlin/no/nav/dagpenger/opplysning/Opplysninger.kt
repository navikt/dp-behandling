package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.Gyldighetsperiode.Companion.overlappendePerioder
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

    private val basertPåOpplysninger: List<Opplysning<*>> =
        basertPå?.let { (it.basertPåOpplysninger + it.egne).filter { opplysning -> opplysning.skalArves } } ?: emptyList()

    private val indeks =
        Opplysningsindeks(
            arvedePerType = basertPåOpplysninger.groupBy { it.opplysningstype },
            egneVedOppstart = egne,
        )

    private val alleOpplysninger: List<Opplysning<*>> get() = indeks.alle

    override val kunEgne: LesbarOpplysninger get() = OpplysningerView(this, bareEgne = true)

    fun <T : Any> leggTil(opplysning: Opplysning<T>) {
        settInnOpplysning(opplysning)
    }

    /**
     * Endrer verdi og/eller gyldighetsperiode på en eksisterende opplysning. Endringen legges til som en ny
     * revisjon med samme identitet, og erstatter hele opplysningen uavhengig av periode.
     */
    fun <T : Any> endre(
        opplysning: Opplysning<T>,
        verdi: T = opplysning.verdi,
        gyldighetsperiode: Gyldighetsperiode = opplysning.gyldighetsperiode,
        kilde: Kilde? = opplysning.kilde,
    ): Opplysning<T> = opplysning.revider(verdi, gyldighetsperiode, kilde).also { settInnOpplysning(it) }

    fun leggTil(block: (MutableList<Opplysning<*>>) -> Unit) {
        val nye = mutableListOf<Opplysning<*>>().apply(block)
        krevIngenOverlappInnadIBatch(nye)
        nye.forEach { settInnOpplysning(it) }
    }

    private fun krevIngenOverlappInnadIBatch(nye: List<Opplysning<*>>) {
        nye
            .groupBy { it.opplysningstype }
            .forEach { (type, opplysninger) ->
                require(!opplysninger.map { it.gyldighetsperiode }.overlappendePerioder()) {
                    "leggTil (batch) støtter ikke opplysninger som overlapper hverandre i samme batch: " +
                        "${type.navn} har overlappende gyldighetsperioder"
                }
            }
    }

    private fun <T : Any> settInnOpplysning(opplysning: Opplysning<T>) {
        opplysning.behandlet = false
        opplysning.behandletVed = null

        if (opplysning.opplysningstype.overlappStrategi == OverlappStrategi.KUN_SISTE) tombstoneAvhengigeAvForrige(opplysning)

        // Sporbarhet: peker på det den overstyrer. Selve oppslaget gjøres av indeksen, ikke av denne lenken.
        if (!opplysning.erRevisjon) {
            finnNullableOpplysning(opplysning.opplysningstype, opplysning.gyldighetsperiode)?.let { eksisterende ->
                when {
                    erArvet(eksisterende) -> opplysning.erstatter(eksisterende)
                    else -> eksisterende.erstatter?.let { opplysning.erstatter(it) }
                }
            }
        }
        sjekkAtUtledetAvFinnes(opplysning)

        egne.add(opplysning)
        indeks.leggTilEgen(opplysning)
    }

    private fun tombstoneAvhengigeAvForrige(ny: Opplysning<*>) {
        val forrige = indeks.gjeldende(ny.opplysningstype).maxByOrNull { it.id } ?: return
        if (forrige.verdi == ny.verdi) return
        OpplysningGraf(egne.toList())
            .hentAlleUtledetAv(forrige)
            .filterNot { it is Tombstone<*> }
            .forEach { tombstone(it.opplysningstype, it.gyldighetsperiode) }
    }

    // Invariant: en opplysning kan ikke være utledetAv noe som ikke finnes (lenger) i denne
    // Opplysninger-instansen. Dette kan skje om en avhengighet blir fjernet (f.eks. fordi den selv
    // ble erstattet) etter at denne opplysningen ble beregnet, men før den blir lagt til. En slik
    // opplysning peker da på en «foreldreløs» avhengighet, som til syvende og sist gir et
    // brudd på fremmednøkkelen mot opplysning-tabellen når vi lagrer til database.
    private fun sjekkAtUtledetAvFinnes(opplysning: Opplysning<*>) {
        opplysning.utledetAv?.opplysninger?.forEach { avhengighet ->
            check(egne.contains(avhengighet) || basertPåOpplysninger.contains(avhengighet)) {
                "Opplysning ${opplysning.id} (${opplysning.opplysningstype.navn}) er utledetAv " +
                    "${avhengighet.id} (${avhengighet.opplysningstype.navn}), men denne finnes ikke " +
                    "(lenger) i opplysningene. Avhengigheten kan ha blitt fjernet fordi den selv ble " +
                    "erstattet før denne opplysningen ble lagt til."
            }
        }
    }

    override fun erErstattet(opplysninger: List<Opplysning<*>>): Boolean {
        val erstattedeIder = alleOpplysninger.mapNotNullTo(mutableSetOf()) { it.erstatter?.id }
        return opplysninger.any { it.id in erstattedeIder }
    }

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
        indeks
            .gjeldende(opplysningstype)
            .filterIsInstance<Opplysning<T>>()
            .lastOrNull()

    override fun finnOpplysning(opplysningId: UUID) =
        alleOpplysninger.lastOrNull { it.id == opplysningId }
            ?: throw OpplysningIkkeFunnetException("Har ikke opplysning med id=$opplysningId")

    override fun <T : Any> har(opplysningstype: Opplysningstype<T>) = indeks.gjeldende(opplysningstype).isNotEmpty()

    override fun <T : Any> har(
        opplysningstype: Opplysningstype<T>,
        gjelderFor: LocalDate,
    ) = finnNullableOpplysningMedFiltre(opplysningstype, gjelderFor, false) != null

    override fun finnFlere(opplysningstyper: List<Opplysningstype<*>>) =
        opplysningstyper.mapNotNull { type -> indeks.gjeldende(type).lastOrNull() }

    override fun <T : Any> finnAlle(opplysningstyper: List<Opplysningstype<T>>) = opplysningstyper.flatMap { type -> finnAlle(type) }

    override fun <T : Any> finnAlle(opplysningstype: Opplysningstype<T>) =
        indeks.gjeldende(opplysningstype).filterIsInstance<Opplysning<T>>()

    override fun forDato(gjelderFor: LocalDate): LesbarOpplysninger = OpplysningerView(this, gjelderFor = gjelderFor)

    override fun somListe(filter: Filter) =
        when (filter) {
            Filter.Alle -> alleOpplysninger
            Filter.Egne -> indeks.kunEgne
        }

    fun baserPå(tidligereOpplysninger: Opplysninger?) = Opplysninger(id, egne, tidligereOpplysninger)

    // Rådata slik de er lagt til, uten utflating. Det er dette som skal persisteres.
    fun rå(): List<Opplysning<*>> = egne.toList()

    fun fjernet(): Set<Opplysning<*>> = fjernet.toSet()

    fun fjernHvis(block: (Opplysning<*>) -> Boolean) = egne.filter { block(it) }.forEach { fjern(it) }

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

    private fun fjern(opplysning: Opplysning<*>) {
        // Fjern alle opplysninger som er utledet av opplysningen som fjernes
        fjernAvhengigheter(opplysning)

        if (egne.remove(opplysning)) {
            fjernet.add(opplysning)
            indeks.fjernEgen(opplysning)
        }
    }

    private fun fjernAvhengigheter(eksisterende: Opplysning<*>) {
        val graf = OpplysningGraf(egne.toList())
        val avhengigheter = graf.hentAlleUtledetAv(eksisterende)
        avhengigheter.forEach { avhengighet -> fjern(avhengighet) }
    }

    private fun <T : Any> finnNullableOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    ): Opplysning<T>? =
        indeks
            .gjeldende(opplysningstype)
            .filterIsInstance<Opplysning<T>>()
            .filter { it.gyldighetsperiode.overlapper(gyldighetsperiode) }
            .lastOrNull()

    fun inneholder(opplysning: Opplysning<*>): Boolean = alleOpplysninger.contains(opplysning)

    // Interne hjelpemetoder for OpplysningerView
    internal fun hentOpplysninger(bareEgne: Boolean): List<Opplysning<*>> = if (bareEgne) indeks.kunEgne else alleOpplysninger

    internal fun <T : Any> finnAlleMedFiltre(
        opplysningstype: Opplysningstype<T>,
        gjelderFor: LocalDate?,
        bareEgne: Boolean,
    ): List<Opplysning<T>> =
        (if (bareEgne) indeks.gjeldendeEgne(opplysningstype) else indeks.gjeldende(opplysningstype))
            .filterIsInstance<Opplysning<T>>()
            .filter { gjelderFor == null || it.gyldighetsperiode.inneholder(gjelderFor) }

    internal fun <T : Any> finnNullableOpplysningMedFiltre(
        opplysningstype: Opplysningstype<T>,
        gjelderFor: LocalDate?,
        bareEgne: Boolean,
    ): Opplysning<T>? {
        val kandidater =
            if (bareEgne) {
                indeks
                    .gjeldendeEgne(opplysningstype)
                    .filterIsInstance<Opplysning<T>>()
            } else {
                indeks
                    .gjeldende(opplysningstype)
                    .filterIsInstance<Opplysning<T>>()
            }
        return if (gjelderFor != null) {
            kandidater.lastOrNull { it.gyldighetsperiode.inneholder(gjelderFor) }
        } else {
            kandidater.lastOrNull()
        }
    }

    fun erArvet(opplysning: Opplysning<*>): Boolean = basertPåOpplysninger.contains(opplysning)

    /**
     * Skjuler gjeldende opplysninger av [opplysningstype] i [periode] ved å legge til tombstones.
     * Ingenting slettes, og senere opplysninger av typen vinner over tombstonen.
     */
    fun tombstone(
        opplysningstype: Opplysningstype<*>,
        periode: Gyldighetsperiode = Gyldighetsperiode(),
        kilde: Kilde? = null,
    ) {
        indeks
            .gjeldende(opplysningstype)
            .mapNotNull { synlig -> synlig.gyldighetsperiode.overlapp(periode)?.let { synlig to it } }
            .forEach { (synlig, skjultPeriode) ->
                settInnOpplysning(Tombstone(synlig, skjultPeriode, kilde))
            }
    }

    companion object {
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

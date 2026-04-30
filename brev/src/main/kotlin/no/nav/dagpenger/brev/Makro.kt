package no.nav.dagpenger.brev

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * En makro som kan transformere en opplysningsverdi i en brevtekst.
 *
 * Makroer brukes i template-syntaks med pipe:
 *   {{opplysningsnavn | makroNavn(argument)}}
 *
 * Makroer kan kjedes:
 *   {{opplysningsnavn | månedÅr(0) | storFørsteBokstav()}}
 */
interface Makro {
    /** Navnene denne makroen kan kalles med (lowercase) */
    val navn: Set<String>

    /** Parser argumentstrengen (det som er inni parentesene) og returnerer null om den ikke matcher */
    fun parser(argumentStreng: String): MakroKall?

    /**
     * Utfører makroen på en opplysningsverdi (første i kjeden).
     * [verdi] er råverdien fra opplysningen.
     */
    fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String?

    /**
     * Utfører makroen på en streng (ved kjeding).
     * Default-implementasjon delegerer til [utfør] med strengen som verdi.
     */
    fun utførPåStreng(
        verdi: String,
        kall: MakroKall,
    ): String? = utfør(verdi, kall)
}

/** Representerer et parset makrokall med eventuelle argumenter */
sealed interface MakroKall

// --- Dato-makroer ---

private data class MånedOffset(
    val offset: Long,
) : MakroKall

object MånedÅrMakro : Makro {
    override val navn = setOf("månedår", "måned_år", "maanedaar")

    override fun parser(argumentStreng: String): MakroKall? {
        val offset = argumentStreng.toLongOrNull() ?: return null
        return MånedOffset(offset)
    }

    override fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String? {
        val dato = verdi as? LocalDate ?: return null
        val offset = (kall as MånedOffset).offset
        return dato.plusMonths(offset).format(NORSK_MÅNED_ÅR)
    }

    private val NORSK_MÅNED_ÅR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("nb", "NO"))
}

object DatoMakro : Makro {
    override val navn = setOf("dato")

    override fun parser(argumentStreng: String): MakroKall? {
        val offset = argumentStreng.toLongOrNull() ?: return null
        return MånedOffset(offset)
    }

    override fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String? {
        val dato = verdi as? LocalDate ?: return null
        val offset = (kall as MånedOffset).offset
        return dato.plusMonths(offset).format(NORSK_DATO)
    }

    private val NORSK_DATO = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("nb", "NO"))
}

object PlussDagerMakro : Makro {
    override val navn = setOf("plussdager", "dager")

    override fun parser(argumentStreng: String): MakroKall? {
        val dager = argumentStreng.toLongOrNull() ?: return null
        return DagerOffset(dager)
    }

    override fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String? {
        val dato = verdi as? LocalDate ?: return null
        val dager = (kall as DagerOffset).dager
        return dato.plusDays(dager).format(NORSK_DATO)
    }

    private data class DagerOffset(
        val dager: Long,
    ) : MakroKall

    private val NORSK_DATO = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("nb", "NO"))
}

object PlussUkerMakro : Makro {
    override val navn = setOf("plussuker", "uker")

    override fun parser(argumentStreng: String): MakroKall? {
        val uker = argumentStreng.toLongOrNull() ?: return null
        return UkerOffset(uker)
    }

    override fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String? {
        val dato = verdi as? LocalDate ?: return null
        val uker = (kall as UkerOffset).uker
        return dato.plusWeeks(uker).format(NORSK_DATO)
    }

    private data class UkerOffset(
        val uker: Long,
    ) : MakroKall

    private val NORSK_DATO = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("nb", "NO"))
}

// --- Tekst-makroer ---

private data object IngenArgument : MakroKall

object StorFørsteBokstavMakro : Makro {
    override val navn = setOf("storførstebokstav", "capitalize")

    override fun parser(argumentStreng: String): MakroKall? = if (argumentStreng.isBlank()) IngenArgument else null

    override fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String? = verdi.toString().replaceFirstChar { it.uppercase(NB_NO) }

    override fun utførPåStreng(
        verdi: String,
        kall: MakroKall,
    ): String = verdi.replaceFirstChar { it.uppercase(NB_NO) }
}

object StorBokstavMakro : Makro {
    override val navn = setOf("stor", "uppercase")

    override fun parser(argumentStreng: String): MakroKall? = if (argumentStreng.isBlank()) IngenArgument else null

    override fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String? = verdi.toString().uppercase(NB_NO)

    override fun utførPåStreng(
        verdi: String,
        kall: MakroKall,
    ): String = verdi.uppercase(NB_NO)
}

object LitenBokstavMakro : Makro {
    override val navn = setOf("liten", "lowercase")

    override fun parser(argumentStreng: String): MakroKall? = if (argumentStreng.isBlank()) IngenArgument else null

    override fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String? = verdi.toString().lowercase(NB_NO)

    override fun utførPåStreng(
        verdi: String,
        kall: MakroKall,
    ): String = verdi.lowercase(NB_NO)
}

// --- Verdi-mapping ---

private data class VelgKall(
    val mappinger: Map<String, String>,
) : MakroKall

object VelgMakro : Makro {
    override val navn = setOf("velg")

    override fun parser(argumentStreng: String): MakroKall? {
        if (argumentStreng.isBlank()) return null
        val map =
            argumentStreng
                .split(",")
                .associate { del ->
                    val deler = del.split("=", limit = 2).map { it.trim() }
                    if (deler.size != 2) return null
                    deler[0] to deler[1]
                }
        return VelgKall(map)
    }

    override fun utfør(
        verdi: Any,
        kall: MakroKall,
    ): String? {
        val mappinger = (kall as VelgKall).mappinger
        return mappinger[verdi.toString()]
    }

    override fun utførPåStreng(
        verdi: String,
        kall: MakroKall,
    ): String? {
        val mappinger = (kall as VelgKall).mappinger
        return mappinger[verdi]
    }
}

// --- Registry ---

private val NB_NO = Locale("nb", "NO")

/**
 * Registry over alle tilgjengelige makroer.
 * Nye makroer registreres ved å legge dem til i denne listen.
 */
val standardMakroer: List<Makro> =
    listOf(
        MånedÅrMakro,
        DatoMakro,
        PlussDagerMakro,
        PlussUkerMakro,
        StorFørsteBokstavMakro,
        StorBokstavMakro,
        LitenBokstavMakro,
        VelgMakro,
    )

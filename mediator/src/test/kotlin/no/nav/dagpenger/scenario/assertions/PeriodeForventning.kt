package no.nav.dagpenger.scenario.assertions

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.scenario.assertions.Opplysningsperiode.Periodestatus
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal data class ForventetPeriode<T : Any>(
    val fra: LocalDate?,
    val til: LocalDate?,
    val verdi: T,
    var opprinnelse: Periodestatus? = null,
)

internal data class PeriodeSegment(
    val fra: LocalDate?,
    val til: LocalDate?,
    val verdi: JsonNode,
    val opprinnelse: Periodestatus? = null,
)

internal data class PeriodeUtenVerdi<T : Any>(
    val fra: LocalDate?,
    val til: LocalDate?,
) {
    infix fun til(dato: LocalDate): PeriodeUtenVerdi<T> = copy(til = dato)
}

internal open class PeriodeForventning<T : Any> {
    protected val forventedePerioder = mutableListOf<ForventetPeriode<T>>()
    private var forventerTomListe = false

    fun erIkkeVurdert() {
        forventerTomListe = true
    }

    infix fun fra(dato: LocalDate): PeriodeUtenVerdi<T> = PeriodeUtenVerdi(dato, null)

    fun periode(
        fra: LocalDate?,
        til: LocalDate?,
    ): PeriodeUtenVerdi<T> = PeriodeUtenVerdi(fra, til)

    infix fun PeriodeUtenVerdi<T>.verdi(verdi: T): ForventetPeriode<T> =
        ForventetPeriode(fra, til, verdi).also { forventedePerioder.add(it) }

    infix fun ForventetPeriode<T>.opprinnelse(opprinnelse: Periodestatus): ForventetPeriode<T> {
        this.opprinnelse = opprinnelse
        return this
    }

    internal fun perioder(): List<ForventetPeriode<T>> {
        check(!forventerTomListe || forventedePerioder.isEmpty()) {
            "erIkkeVurdert() kan ikke kombineres med forventede perioder"
        }
        return forventedePerioder
    }
}

internal class RettighetsperiodeForventning : PeriodeForventning<Boolean>() {
    infix fun PeriodeUtenVerdi<Boolean>.harRett(verdi: Boolean) {
        forventedePerioder.add(ForventetPeriode(fra, til, verdi))
    }
}

internal fun <T : Any> assertPerioder(
    label: String,
    forventet: List<ForventetPeriode<T>>,
    faktisk: List<PeriodeSegment>,
) {
    val sortertForventet =
        forventet
            .map {
                val verdi =
                    when (val verdi = it.verdi) {
                        is Beløp -> objectMapper.readTree(objectMapper.writeValueAsString(verdi.verdien))
                        else -> objectMapper.valueToTree(verdi)
                    }
                PeriodeSegment(it.fra, it.til, verdi, it.opprinnelse)
            }.sortedWith(periodeComparator)
    faktisk should haPerioder(label, sortertForventet)
}

private fun haPerioder(
    label: String,
    sortertForventet: List<PeriodeSegment>,
): Matcher<List<PeriodeSegment>> =
    Matcher { faktisk ->
        val sortertFaktisk = faktisk.sortedWith(periodeComparator)
        MatcherResult(
            sortertForventet.size == sortertFaktisk.size &&
                sortertForventet.indices.all { sortertForventet[it].samsvarerMed(sortertFaktisk[it]) },
            {
                buildString {
                    appendLine()
                    appendLine("Perioder for '$label' avviker")
                    repeat(maxOf(sortertForventet.size, sortertFaktisk.size)) { index ->
                        val forventetPeriode = sortertForventet.getOrNull(index)
                        val faktiskPeriode = sortertFaktisk.getOrNull(index)
                        val perioder = listOfNotNull(forventetPeriode, faktiskPeriode)
                        if (forventetPeriode.samsvarerMed(faktiskPeriode)) {
                            appendLine("OK periode #${index + 1}: ${faktiskPeriode.beskrivelse()}")
                        } else {
                            appendLine("Feil i periode #${index + 1}")
                            appendLine("forventet -> ${forventetPeriode.beskrivelse()}")
                            appendLine("faktisk   -> ${faktiskPeriode.beskrivelse()}")
                            appendLine(renderTimelineHeader(perioder))
                            appendLine(renderTimeline("", listOfNotNull(forventetPeriode), perioder))
                            appendLine(renderTimeline("", listOfNotNull(faktiskPeriode), perioder))
                        }
                        appendLine()
                    }
                }
            },
            { "Perioder for '$label' skulle ikke samsvare med forventede perioder" },
        )
    }

private fun PeriodeSegment?.samsvarerMed(faktisk: PeriodeSegment?): Boolean =
    when {
        this == null -> {
            faktisk == null
        }

        faktisk == null -> {
            false
        }

        else -> {
            fra == faktisk.fra &&
                til == faktisk.til &&
                verdi == faktisk.verdi &&
                (opprinnelse == null || opprinnelse == faktisk.opprinnelse)
        }
    }

private fun PeriodeSegment?.beskrivelse(): String =
    if (this == null) {
        "(mangler)"
    } else {
        "${fra?.format(datoformat) ?: "−∞"}..${til?.format(datoformat) ?: "+∞"} med verdi $verdi" +
            (opprinnelse?.let { ", opprinnelse: $it" } ?: "")
    }

private val datoformat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private val periodeComparator =
    Comparator<PeriodeSegment> { første, andre ->
        sammenlignStart(første.fra, andre.fra).takeIf { it != 0 }
            ?: sammenlignSlutt(første.til, andre.til)
    }

private fun sammenlignStart(
    første: LocalDate?,
    andre: LocalDate?,
): Int =
    when {
        første == null && andre == null -> 0
        første == null -> -1
        andre == null -> 1
        else -> første.compareTo(andre)
    }

private fun sammenlignSlutt(
    første: LocalDate?,
    andre: LocalDate?,
): Int =
    when {
        første == null && andre == null -> 0
        første == null -> 1
        andre == null -> -1
        else -> første.compareTo(andre)
    }

private const val AXIS_CELL_WIDTH = 12
private const val AXIS_PREFIX_WIDTH = 14

private fun renderTimelineHeader(perioder: List<PeriodeSegment>): String {
    val grenser = perioder.flatMap { listOfNotNull(it.fra, it.til) }.distinct().sorted()
    val etiketter = listOf("−∞") + grenser.map { it.format(datoformat) } + "+∞"
    return " ".repeat(AXIS_PREFIX_WIDTH) + tegnAkse(etiketter).trimEnd()
}

private fun renderTimeline(
    label: String,
    perioder: List<PeriodeSegment>,
    fellesPerioder: List<PeriodeSegment>,
): String {
    val grenser = fellesPerioder.flatMap { listOfNotNull(it.fra, it.til) }.distinct().sorted()
    if (perioder.isEmpty()) return "$label: (mangler)"

    return buildString {
        perioder.forEach { periode ->
            val akse = listOf("−∞") + grenser.map(LocalDate::toString) + "+∞"
            val start = periode.fra?.let { grenser.indexOf(it) + 1 } ?: 0
            val slutt = periode.til?.let { grenser.indexOf(it) + 1 } ?: (akse.lastIndex)
            append(label.padEnd(AXIS_PREFIX_WIDTH))
            append(tegnPølse(akse.size, start, slutt, periode.fra == null, periode.til == null).trimEnd())
        }
    }
}

private fun tegnAkse(etiketter: List<String>): String {
    val rutenett = CharArray(etiketter.size * AXIS_CELL_WIDTH) { ' ' }
    etiketter.forEachIndexed { index, etikett ->
        val start = index * AXIS_CELL_WIDTH + (AXIS_CELL_WIDTH - etikett.length) / 2
        etikett.forEachIndexed { offset, tegn -> rutenett[start + offset] = tegn }
    }
    return rutenett.concatToString()
}

private fun tegnPølse(
    antallGrenser: Int,
    start: Int,
    slutt: Int,
    åpenStart: Boolean,
    åpenSlutt: Boolean,
): String {
    val rutenett = CharArray(antallGrenser * AXIS_CELL_WIDTH) { ' ' }
    val startPosisjon = start * AXIS_CELL_WIDTH + AXIS_CELL_WIDTH / 2
    val sluttPosisjon = slutt * AXIS_CELL_WIDTH + AXIS_CELL_WIDTH / 2
    for (posisjon in startPosisjon..sluttPosisjon) {
        rutenett[posisjon] = '─'
    }
    rutenett[startPosisjon] =
        if (åpenStart) {
            '<'
        } else if (start == slutt) {
            '●'
        } else {
            '['
        }
    rutenett[sluttPosisjon] =
        if (åpenSlutt) {
            '>'
        } else if (start == slutt) {
            '●'
        } else {
            ']'
        }
    return rutenett.concatToString()
}

package no.nav.dagpenger.opplysning

import java.time.LocalDate
import kotlin.comparisons.nullsLast

enum class Forbruksrekkefølge {
    ETTERFØLGENDE,
    PARALLELL,
}

enum class Forbrukstype {
    ORDINÆR,
    BORTFALL,
}

data class KvoteDefinisjon(
    val hjemmel: Hjemmel,
    val kilder: List<KvoteKilde>,
    val forbrukKriterium: Opplysningstype<Boolean>,
    val forbruktTeller: Opplysningstype<Int>,
    val gjenstående: Opplysningstype<Int>,
    val sisteDagMedForbruk: Opplysningstype<LocalDate>,
    val sisteGjenstående: Opplysningstype<Int>,
    val forbrukstype: Forbrukstype = Forbrukstype.ORDINÆR,
    val forbruksrekkefølge: Forbruksrekkefølge = Forbruksrekkefølge.PARALLELL,
) {
    val navn get() = hjemmel.kortnavn
}

data class KvoteKilde(
    val kapasitet: Opplysningstype<Int>,
    val aktiveresAv: Opplysningstype<Boolean>? = null,
)

fun KvoteDefinisjon.totalKapasitet(opplysninger: LesbarOpplysninger): Int =
    kilder.sumOf { kilde ->
        val aktiveresAv = kilde.aktiveresAv
        when {
            aktiveresAv != null && (!opplysninger.har(aktiveresAv) || !opplysninger.erSann(aktiveresAv)) -> 0
            !opplysninger.har(kilde.kapasitet) -> 0
            else -> opplysninger.finnOpplysning(kilde.kapasitet).verdi
        }
    }

fun KvoteDefinisjon.gjenståendeVed(
    opplysninger: LesbarOpplysninger,
    førsteDag: LocalDate,
): Int {
    val sisteGjenstående = opplysninger.sisteVerdiFør(gjenstående, førsteDag)
    return sisteGjenstående ?: totalKapasitet(opplysninger)
}

fun KvoteDefinisjon.erEtterfølgendeForbruk(): Boolean = forbruksrekkefølge == Forbruksrekkefølge.ETTERFØLGENDE

fun KvoteDefinisjon.erBortfallsforbruk(): Boolean = forbrukstype == Forbrukstype.BORTFALL

fun List<KvoteDefinisjon>.allokeringskjede(opplysninger: LesbarOpplysninger): List<KvoteDefinisjon> =
    filter { it.erEtterfølgendeForbruk() }.sortertEtterIlagtDato(opplysninger)

/** Første ilagte dato for en kvote, eller null når kvoten ikke har noen aktiveringskilde. */
fun KvoteDefinisjon.ilagtDato(opplysninger: LesbarOpplysninger): LocalDate? =
    kilder
        .asSequence()
        .mapNotNull { kilde -> kilde.aktiveresAv }
        .flatMap { aktiveresAv -> opplysninger.finnAlle(aktiveresAv).asSequence() }
        .filter { it.verdi }
        .map { it.gyldighetsperiode.fraOgMed }
        .minOrNull()

fun List<KvoteDefinisjon>.sortertEtterIlagtDato(opplysninger: LesbarOpplysninger): List<KvoteDefinisjon> =
    sortedWith(compareBy(nullsLast<LocalDate>()) { kvote -> kvote.ilagtDato(opplysninger) })

private fun LesbarOpplysninger.sisteVerdiFør(
    opplysningstype: Opplysningstype<Int>,
    førsteDag: LocalDate,
): Int? =
    finnAlle(opplysningstype)
        .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(førsteDag) }
        ?.verdi

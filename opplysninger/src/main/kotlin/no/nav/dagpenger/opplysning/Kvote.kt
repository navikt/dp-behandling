package no.nav.dagpenger.opplysning

import java.time.LocalDate

data class KvoteDefinisjon(
    val hjemmel: Hjemmel,
    val kilder: List<KvoteKilde>,
    val forbrukKriterium: Opplysningstype<Boolean>,
    val forbruktTeller: Opplysningstype<Int>,
    val gjenstående: Opplysningstype<Int>,
    val sisteDagMedForbruk: Opplysningstype<LocalDate>? = null,
    val sisteGjenstående: Opplysningstype<Int>? = null,
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

private fun LesbarOpplysninger.sisteVerdiFør(
    opplysningstype: Opplysningstype<Int>,
    førsteDag: LocalDate,
): Int? =
    finnAlle(opplysningstype)
        .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(førsteDag) }
        ?.verdi

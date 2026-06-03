package no.nav.dagpenger.opplysning

import java.time.LocalDate

enum class Forbrukstype {
    Rettighet,
    Bortfall,
}

data class KvoteDefinisjon(
    val hjemmel: Hjemmel,
    val tildelingsgrunnlag: Tildelingsgrunnlag,
    val tellesNår: Opplysningstype<Boolean>,
    val forbruksteller: Opplysningstype<Int>,
    val gjenstående: Opplysningstype<Int>,
    val sisteForbruk: Opplysningstype<LocalDate>,
    val sisteGjenstående: Opplysningstype<Int>,
    val forbrukstype: Forbrukstype = Forbrukstype.Rettighet,
) {
    val navn get() = hjemmel.kortnavn
}

data class Tildelingsgrunnlag(
    val kapasitet: Opplysningstype<Int>,
    private val aktiveresAv: Opplysningstype<Boolean>? = null,
) {
    fun erAktiv(opplysninger: LesbarOpplysninger): Boolean =
        aktiveresAv == null || (opplysninger.har(aktiveresAv) && opplysninger.erSann(aktiveresAv))

    fun harAktiveringskilde(): Boolean = aktiveresAv != null

    fun ilagtDato(opplysninger: LesbarOpplysninger): LocalDate? =
        aktiveresAv
            ?.let { opplysninger.finnAlle(it) }
            ?.filter { it.verdi }
            ?.minOfOrNull { it.gyldighetsperiode.fraOgMed }
}

fun KvoteDefinisjon.totalKapasitet(opplysninger: LesbarOpplysninger): Int {
    if (!tildelingsgrunnlag.erAktiv(opplysninger)) return 0
    if (!opplysninger.har(tildelingsgrunnlag.kapasitet)) return 0
    return opplysninger.finnOpplysning(tildelingsgrunnlag.kapasitet).verdi
}

fun KvoteDefinisjon.gjenståendeVed(
    opplysninger: LesbarOpplysninger,
    førsteDag: LocalDate,
): Int {
    val sisteGjenstående = opplysninger.sisteVerdiFør(gjenstående, førsteDag)
    return sisteGjenstående ?: totalKapasitet(opplysninger)
}

fun KvoteDefinisjon.erEksklusivt(): Boolean = forbrukstype == Forbrukstype.Bortfall

fun List<KvoteDefinisjon>.allokeringskjede(opplysninger: LesbarOpplysninger): List<KvoteDefinisjon> =
    filter { it.erEksklusivt() }.sortertEtterIlagtDato(opplysninger)

/** Første ilagte dato for en kvote, basert på aktiveringsflaggets første sanne verdi. */
fun KvoteDefinisjon.ilagtDato(opplysninger: LesbarOpplysninger): LocalDate? = tildelingsgrunnlag.ilagtDato(opplysninger)

fun List<KvoteDefinisjon>.sortertEtterIlagtDato(opplysninger: LesbarOpplysninger): List<KvoteDefinisjon> =
    sortedWith(compareBy(nullsLast<LocalDate>()) { kvote -> kvote.ilagtDato(opplysninger) })

private fun LesbarOpplysninger.sisteVerdiFør(
    opplysningstype: Opplysningstype<Int>,
    førsteDag: LocalDate,
): Int? =
    finnAlle(opplysningstype)
        .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(førsteDag) }
        ?.verdi

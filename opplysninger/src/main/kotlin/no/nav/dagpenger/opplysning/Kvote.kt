package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.Forbrukstype.Bortfall
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

    fun teller(type: Forbrukstype) = type == forbrukstype

    fun tildeltKapasitet(opplysninger: LesbarOpplysninger): Int {
        if (!opplysninger.har(tildelingsgrunnlag.kapasitet)) return 0
        return opplysninger.finnOpplysning(tildelingsgrunnlag.kapasitet).verdi
    }

    fun gjenståendeVed(
        opplysninger: LesbarOpplysninger,
        førsteDag: LocalDate,
    ): Int {
        val sisteGjenståendeVerdi =
            opplysninger
                .finnAlle(gjenstående)
                .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(førsteDag) }
                ?.verdi
        return sisteGjenståendeVerdi ?: tildeltKapasitet(opplysninger)
    }

    fun forrigeForbruk(
        opplysninger: LesbarOpplysninger,
        før: LocalDate,
    ): Int =
        opplysninger
            .finnAlle(forbruksteller)
            .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(før) }
            ?.verdi ?: 0
}

data class Tildelingsgrunnlag(
    val kapasitet: Opplysningstype<Int>,
) {
    fun ilagtDato(opplysninger: LesbarOpplysninger): LocalDate? =
        opplysninger
            .finnAlle(kapasitet)
            .filter { it.verdi > 0 }
            .minOfOrNull { it.gyldighetsperiode.fraOgMed }
}

fun List<KvoteDefinisjon>.sanksjonerSortert(opplysninger: LesbarOpplysninger): List<KvoteDefinisjon> =
    filter { it.teller(Bortfall) }.sortertEtterIlagtDato(opplysninger)

fun List<KvoteDefinisjon>.sortertEtterIlagtDato(opplysninger: LesbarOpplysninger): List<KvoteDefinisjon> =
    sortedWith(compareBy(nullsLast()) { kvote -> kvote.tildelingsgrunnlag.ilagtDato(opplysninger) })

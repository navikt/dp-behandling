package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class FraOgMedForOpplysning internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val opplysningstype: Opplysningstype<Boolean>,
) : Regel<LocalDate>(produserer, listOf(opplysningstype)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): LocalDate {
        val opplysning = opplysninger.finnOpplysning(opplysningstype)
        return opplysning.gyldighetsperiode.fom
    }

    override fun toString() = "Opplysning $opplysningstype er sann"
}

fun Opplysningstype<LocalDate>.fraOgMed(opplysningstype: Opplysningstype<Boolean>) = FraOgMedForOpplysning(this, opplysningstype)

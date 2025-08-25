package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class ErSann internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val opplysningstype: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, listOf(opplysningstype)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Boolean = opplysninger.finnOpplysning(opplysningstype).verdi

    override fun toString() = "Sjekket om opplysning $opplysningstype er sann"
}

fun Opplysningstype<Boolean>.erSann(opplysningstype: Opplysningstype<Boolean>) = ErSann(this, opplysningstype)

fun Opplysningstype<Boolean>.oppfylt(opplysningstype: Opplysningstype<Boolean>) = ErSann(this, opplysningstype)

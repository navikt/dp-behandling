package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class ErUsann internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val opplysningstype: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, listOf(opplysningstype)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Boolean = opplysninger.finnOpplysning(opplysningstype).verdi.not()

    override fun toString() = "Sjekket om opplysning $opplysningstype er usann"
}

fun Opplysningstype<Boolean>.erUsann(opplysningstype: Opplysningstype<Boolean>) = ErUsann(this, opplysningstype)

fun Opplysningstype<Boolean>.ikkeOppfylt(opplysningstype: Opplysningstype<Boolean>) = this.erUsann(opplysningstype)

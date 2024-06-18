package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class ErIkkeSann internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val opplysningstype: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, listOf(opplysningstype)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean {
        return opplysninger.finnOpplysning(opplysningstype).verdi.not()
    }

    override fun toString() = "Opplysning $opplysningstype er ikke sann"
}

fun Opplysningstype<Boolean>.erIkkeSann(opplysningstype: Opplysningstype<Boolean>) = ErIkkeSann(this, opplysningstype)

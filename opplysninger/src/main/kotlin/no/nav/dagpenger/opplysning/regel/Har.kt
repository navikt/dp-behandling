package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class Har internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val opplysningstype: Opplysningstype<out Any>,
) : Regel<Boolean>(produserer, listOf(opplysningstype)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean = opplysninger.har(opplysningstype)

    override fun toString() = "Sjekker om vi har $opplysningstype"
}

fun Opplysningstype<Boolean>.har(opplysningstype: Opplysningstype<out Any>) = Har(this, opplysningstype)

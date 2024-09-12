package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class EnAv internal constructor(
    produserer: Opplysningstype<Boolean>,
    private vararg val opplysningstyper: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.finnAlle(opplysningstyper.toList()).any { it.verdi as Boolean }

    override fun toString() = "Sjekker om minst en av ${opplysningstyper.joinToString(", ")} er sanne"
}

fun Opplysningstype<Boolean>.enAv(vararg opplysningstype: Opplysningstype<Boolean>) = EnAv(this, *opplysningstype)

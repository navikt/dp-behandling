package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class IngenAv internal constructor(
    produserer: Opplysningstype<Boolean>,
    private vararg val opplysningstyper: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean = opplysninger.finnAlle(opplysningstyper.toList()).none { it.verdi }

    override fun toString() = "Sjekker at ingen av ${opplysningstyper.joinToString(", ")} er sanne"
}

fun Opplysningstype<Boolean>.ingenAv(vararg opplysningstype: Opplysningstype<Boolean>) = IngenAv(this, *opplysningstype)

fun Opplysningstype<Boolean>.ikke(opplysningstype: Opplysningstype<Boolean>) = IngenAv(this, opplysningstype)

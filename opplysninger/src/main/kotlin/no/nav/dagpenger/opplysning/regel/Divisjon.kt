package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class Divisjon internal constructor(
    produserer: Opplysningstype<Double>,
    private vararg val opplysningstyper: Opplysningstype<Double>,
) : Regel<Double>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): Double {
        val verdier = opplysninger.finnAlle(opplysningstyper.toList()).map { it.verdi as Double }
        return verdier.reduce { acc, d -> acc / d }
    }

    override fun toString(): String = "Multiplikasjon av ${opplysningstyper.joinToString(", ")}"
}

fun Opplysningstype<Double>.divisjon(vararg opplysningstype: Opplysningstype<Double>) = Multiplikasjon(this, *opplysningstype)

operator fun Opplysningstype<Double>.div(opplysningstype: Opplysningstype<Double>) = Multiplikasjon(this, opplysningstype)

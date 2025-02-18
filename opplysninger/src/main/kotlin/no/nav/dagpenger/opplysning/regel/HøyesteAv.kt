package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp

class HøyesteAv<T : Comparable<T>>(
    produserer: Opplysningstype<T>,
    vararg val opplysningstyper: Opplysningstype<T>,
) : Regel<T>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger) =
        opplysningstyper.maxOfOrNull { opplysningstype -> opplysninger.finnOpplysning(opplysningstype).verdi }
            ?: throw IllegalArgumentException("Ingen opplysninger å sammenligne")

    override fun toString() = "Produserer $produserer ved å velge høyeste verdien av ${opplysningstyper.joinToString { it.navn }}"
}

@JvmName("høyesteAvInt")
fun Opplysningstype<Int>.høyesteAv(vararg opplysningstype: Opplysningstype<Int>) = HøyesteAv(this, *opplysningstype)

@JvmName("høyesteAvDesimaltall")
fun Opplysningstype<Double>.høyesteAv(vararg opplysningstype: Opplysningstype<Double>) = HøyesteAv(this, *opplysningstype)

@JvmName("høyesteAvBeløp")
fun Opplysningstype<Beløp>.høyesteAv(vararg opplysningstype: Opplysningstype<Beløp>) = HøyesteAv(this, *opplysningstype)

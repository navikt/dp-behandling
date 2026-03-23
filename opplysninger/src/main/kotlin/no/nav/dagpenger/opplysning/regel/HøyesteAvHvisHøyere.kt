package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp

class HøyesteAvHvisHøyere<T : Comparable<T>>(
    produserer: Opplysningstype<T>,
    vararg opplysningstyper: Opplysningstype<T>,
) : HøyesteAv<T>(produserer, *opplysningstyper) {
    override fun kjør(opplysninger: LesbarOpplysninger): T {
        val nyMax = super.kjør(opplysninger)
        if (!opplysninger.har(produserer)) return nyMax
        return maxOf(opplysninger.finnOpplysning(produserer).verdi, nyMax)
    }

    override fun toString() = "${super.toString()}, bare hvis høyere enn gjeldende"
}

@JvmName("høyesteAvInt")
fun Opplysningstype<Int>.høyesteAvHvisHøyere(vararg opplysningstype: Opplysningstype<Int>) = HøyesteAvHvisHøyere(this, *opplysningstype)

@JvmName("høyesteAvBeløp")
fun Opplysningstype<Beløp>.høyesteAvHvisHøyere(vararg opplysningstype: Opplysningstype<Beløp>) = HøyesteAvHvisHøyere(this, *opplysningstype)

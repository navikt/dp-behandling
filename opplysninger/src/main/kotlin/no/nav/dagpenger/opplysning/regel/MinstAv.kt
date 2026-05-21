package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp

class MinstAv<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<T>,
    private vararg val opplysningstyper: Opplysningstype<T>,
) : Regel<T>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): T =
        opplysningstyper.minOfOrNull { opplysningstype -> opplysninger.finnOpplysning(opplysningstype).verdi }
            ?: throw IllegalArgumentException("Ingen opplysninger å sammenligne")

    override fun toString() = "Fastsetter $produserer til laveste verdi av ${opplysningstyper.joinToString { it.navn }}"
}

@JvmName("minstAvDouble")
fun Opplysningstype<Double>.minstAv(vararg verdi: Opplysningstype<Double>) = MinstAv(this, *verdi)

@JvmName("minstAvAvBeløp")
fun Opplysningstype<Beløp>.minstAv(vararg opplysningstype: Opplysningstype<Beløp>) = MinstAv(this, *opplysningstype)

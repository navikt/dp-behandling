package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class Utgangspunkt<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<T>,
    private val verdi: T,
    nullstillesAv: List<Opplysningstype<*>> = emptyList(),
) : Regel<T>(produserer, nullstillesAv) {
    override fun kjør(opplysninger: LesbarOpplysninger): T = verdi

    override fun toString() = "Bruker $verdi som utgangspunkt for $produserer"
}

fun <T : Comparable<T>> Opplysningstype<T>.somUtgangspunkt(verdi: T) = Utgangspunkt(this, verdi)

fun <T : Comparable<T>> Opplysningstype<T>.somUtgangspunkt(
    verdi: T,
    vararg nullstillesAv: Opplysningstype<*>,
) = Utgangspunkt(this, verdi, nullstillesAv.toList())

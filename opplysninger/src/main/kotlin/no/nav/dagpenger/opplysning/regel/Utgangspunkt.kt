package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class Utgangspunkt<T : Any> internal constructor(
    produserer: Opplysningstype<T>,
    private val verdi: T,
    nullstillesAv: List<Opplysningstype<out Any>> = emptyList(),
) : Regel<T>(produserer, nullstillesAv) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): T = verdi

    override fun toString() = "Bruker $verdi som utgangspunkt for $produserer"
}

fun <T : Any> Opplysningstype<T>.somUtgangspunkt(verdi: T) = Utgangspunkt(this, verdi)

fun <T : Any> Opplysningstype<T>.somUtgangspunkt(
    verdi: T,
    vararg nullstillesAv: Opplysningstype<out Any>,
) = Utgangspunkt(this, verdi, nullstillesAv.toList())

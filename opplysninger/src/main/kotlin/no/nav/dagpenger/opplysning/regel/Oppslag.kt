package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class Oppslag<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<T>,
    private val block: (LocalDate) -> T,
) : Regel<T>(produserer, emptyList()) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): T = block(prøvingsdato)

    override fun toString() = "Finner gjeldende verdi for $produserer på dato"
}

fun <T : Comparable<T>> Opplysningstype<T>.oppslag(block: (LocalDate) -> T) = Oppslag(this, block)

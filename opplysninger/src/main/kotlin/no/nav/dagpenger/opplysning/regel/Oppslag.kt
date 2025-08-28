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

class OppslagMedOpplysning<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<T>,
    private val oppslagsdato: Opplysningstype<LocalDate>,
    private val block: (LocalDate) -> T,
) : Regel<T>(produserer, listOf(oppslagsdato)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): T {
        val dato = opplysninger.finnOpplysning(oppslagsdato).verdi
        return block(dato)
    }

    override fun toString() = "Finner gjeldende verdi for $produserer på dato $oppslagsdato"
}

fun <T : Comparable<T>> Opplysningstype<T>.oppslag(block: (LocalDate) -> T) = Oppslag(this, block)

fun <T : Comparable<T>> Opplysningstype<T>.oppslag(
    oppslagsdato: Opplysningstype<LocalDate>,
    block: (LocalDate) -> T,
) = OppslagMedOpplysning(this, oppslagsdato, block)

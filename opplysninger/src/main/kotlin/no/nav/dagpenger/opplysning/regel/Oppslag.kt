package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class Oppslag<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<T>,
    private val dato: Opplysningstype<LocalDate>,
    private val block: (LocalDate) -> T,
) : Regel<T>(produserer, listOf(dato)) {
    override fun kjør(opplysninger: LesbarOpplysninger): T {
        val oppslagsdato = opplysninger.finnOpplysning(dato).verdi

        return block(oppslagsdato)
    }

    override fun toString() = "Finner gjeldende verdi for $produserer på $dato"
}

fun <T : Comparable<T>> Opplysningstype<T>.oppslag(
    dato: Opplysningstype<LocalDate>,
    block: (LocalDate) -> T,
) = Oppslag(this, dato, block)

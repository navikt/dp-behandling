package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class SisteAv internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private vararg val datoer: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, datoer.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): LocalDate {
        val dager = opplysninger.finnAlle(datoer.toList()).map { it.verdi }
        return dager.maxOrNull() ?: throw IllegalStateException("Ingen datoer funnet")
    }

    override fun toString() = "Fastsetter $produserer til siste dato av ${datoer.joinToString()}"
}

fun Opplysningstype<LocalDate>.sisteAv(vararg liste: Opplysningstype<LocalDate>) = SisteAv(this, *liste)

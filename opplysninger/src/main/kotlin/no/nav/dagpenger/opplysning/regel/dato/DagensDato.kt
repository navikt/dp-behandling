package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class DagensDato internal constructor(
    produserer: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer) {
    override fun skalKjøre(opplysninger: LesbarOpplysninger): Boolean {
        val dagensDato = LocalDate.now()
        if (opplysninger.mangler(produserer)) {
            return true
        }

        // Sjekk om dagens dato har endret seg siden sist
        val dag = opplysninger.finnOpplysning(produserer).verdi
        if (dagensDato.isEqual(dag)) {
            return true
        }

        return false
    }

    override fun kjør(opplysninger: LesbarOpplysninger): LocalDate = LocalDate.now()

    override fun toString() = "Fastsetter $produserer til dagens dato"
}

val Opplysningstype<LocalDate>.finnDagensDato get() = DagensDato(this)

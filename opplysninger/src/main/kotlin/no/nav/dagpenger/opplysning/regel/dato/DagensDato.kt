package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class DagensDato internal constructor(
    produserer: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer) {
    override fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: MutableSet<Regel<*>>,
        produsenter: Map<Opplysningstype<out Comparable<*>>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        val dagensDato = LocalDate.now()
        if (opplysninger.mangler(produserer)) {
            plan.add(this)
            return
        }

        // Sjekk om dagens dato har endret seg siden sist
        val dag = opplysninger.finnOpplysning(produserer).verdi
        if (dagensDato != dag) {
            plan.add(this)
        }
    }

    override fun kjør(opplysninger: LesbarOpplysninger): LocalDate = LocalDate.now()

    override fun toString() = "Fastsetter $produserer til dagens dato"
}

val Opplysningstype<LocalDate>.finnDagensDato get() = DagensDato(this)

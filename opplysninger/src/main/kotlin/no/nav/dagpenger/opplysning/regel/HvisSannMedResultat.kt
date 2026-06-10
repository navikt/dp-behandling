package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelplanlegger
import no.nav.dagpenger.opplysning.finn

class HvisSannMedResultat<T : Any>(
    produserer: Opplysningstype<T>,
    private val sjekk: Opplysningstype<Boolean>,
    private val hvisSann: Opplysningstype<T>,
    private val hvisUsann: Opplysningstype<T>,
) : Regel<T>(produserer, listOf(sjekk, hvisSann, hvisUsann)) {
    override fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        kø: Regelkø,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
    ) {
        if (opplysninger.har(produserer)) {
            // Produktet finnes — deleger til Regel sin logikk for å håndtere endringer
            return super.lagPlan(opplysninger, plan, kø, produsenter)
        }

        if (opplysninger.mangler(sjekk)) {
            kø.add(produsenter.finn(sjekk))
            return
        }

        val sjekkVerdi = opplysninger.finnOpplysning(sjekk).verdi
        val neste = if (sjekkVerdi) hvisSann else hvisUsann

        if (opplysninger.mangler(neste)) {
            kø.add(produsenter.finn(neste))
        } else {
            plan.add(this)
        }
    }

    override fun kjør(opplysninger: LesbarOpplysninger): T {
        val sjekk = opplysninger.finnOpplysning(sjekk).verdi

        return if (sjekk) {
            opplysninger.finnOpplysning(hvisSann).verdi
        } else {
            opplysninger.finnOpplysning(hvisUsann).verdi
        }
    }

    override fun toString() = "Hvis $sjekk er sann, returner $hvisSann, ellers returner $hvisUsann"
}

fun <T : Any> Opplysningstype<T>.hvisSannMedResultat(
    sjekk: Opplysningstype<Boolean>,
    hvisSann: Opplysningstype<T>,
    hvisUsann: Opplysningstype<T>,
) = HvisSannMedResultat(this, sjekk, hvisSann, hvisUsann)

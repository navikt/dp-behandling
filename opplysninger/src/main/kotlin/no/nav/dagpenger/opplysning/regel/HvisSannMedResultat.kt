package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.finn

class HvisSannMedResultat<T : Comparable<T>>(
    produserer: Opplysningstype<T>,
    private val sjekk: Opplysningstype<Boolean>,
    private val hvisSann: Opplysningstype<T>,
    private val hvisUsann: Opplysningstype<T>,
) : Regel<T>(produserer, listOf(sjekk, hvisSann, hvisUsann)) {
    override fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: MutableSet<Regel<*>>,
        produsenter: Map<Opplysningstype<*>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        besøkt.add(this)
        if (opplysninger.har(produserer)) return
        if (opplysninger.mangler(sjekk)) {
            produsenter.finn(sjekk).lagPlan(opplysninger, plan, produsenter, besøkt)
            return
        }

        val sjekkVerdi = opplysninger.finnOpplysning(sjekk).verdi
        val neste = if (sjekkVerdi) hvisSann else hvisUsann

        if (opplysninger.mangler(neste)) {
            produsenter.finn(neste).lagPlan(opplysninger, plan, produsenter, besøkt)
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

fun <T : Comparable<T>> Opplysningstype<T>.hvisSannMedResultat(
    sjekk: Opplysningstype<Boolean>,
    hvisSann: Opplysningstype<T>,
    hvisUsann: Opplysningstype<T>,
) = HvisSannMedResultat(this, sjekk, hvisSann, hvisUsann)

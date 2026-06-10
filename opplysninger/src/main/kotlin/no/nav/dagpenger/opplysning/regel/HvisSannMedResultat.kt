package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelplanlegger
import no.nav.dagpenger.opplysning.TreNode
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
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
    ): TreNode<Plannode>? {
        if (opplysninger.har(produserer)) {
            // Deleger til Regel sin logikk for endringer
            return super.lagPlan(opplysninger, plan, produsenter)
        }

        if (opplysninger.mangler(sjekk)) {
            val avhengighet = produsenter.finn(sjekk).lagPlan(opplysninger, plan, produsenter) ?: error("Forventer ikke dette")
            return TreNode(Plannode(this, Plannode.Årsak.MANGLER_PRODUKT), listOfNotNull(avhengighet))
        }

        val sjekkVerdi = opplysninger.finnOpplysning(sjekk).verdi
        val neste = if (sjekkVerdi) hvisSann else hvisUsann

        if (opplysninger.mangler(neste)) {
            val avhengighet = produsenter.finn(neste).lagPlan(opplysninger, plan, produsenter) ?: error("Forventer ikke dette")
            return TreNode(Plannode(this, Plannode.Årsak.MANGLER_PRODUKT), listOfNotNull(avhengighet))
        } else {
            plan.add(this)
            return TreNode(Plannode(this, Plannode.Årsak.MANGLER_PRODUKT), emptyList())
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

package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Utledning
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
        if (besøkt.contains(this)) return else besøkt.add(this)

        val produkt = opplysninger.finnNullableOpplysning(produserer)
        if (produkt != null && produkt.utledetAv != null) {
            // Sjekk om produktet er basert på erstattet informasjon. Legg til planen for denne regelen hvis det er tilfelle, ellers er produktet fortsatt gyldig og regelen trenger ikke å kjøres på nytt
            // denne henger sammen med sjekken for utdatert som lager plan for alle opplysninger som er utdatert, slik at de blir erstattet og denne regelen blir kjørt på nytt.
            val (erstattet, ikkeErstattet) = produkt.utledetAv.opplysninger.partition { opplysninger.erErstattet(listOf(it)) }
            if (erstattet.isNotEmpty()) {
                plan.add(this)
            }
            // Sjekk om produktet er basert på utdaterte opplysninger. Lag plan for disse hvis det er tilfelle, ellers er produktet fortsatt gyldig og regelen trenger ikke å kjøres på nytt
            val utdaterte = ikkeErstattet.filter { it.erUtdatert }
            if (utdaterte.isNotEmpty()) {
                utdaterte.forEach { produsenter[it.opplysningstype]?.lagPlan(opplysninger, plan, produsenter, besøkt) }
            }
            // Produktet er fortsatt gyldig, ingen grunn til å kjøre regelen på nytt
            return
        }

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

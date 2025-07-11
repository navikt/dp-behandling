package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class Utgangspunkt<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<T>,
    private val verdi: T,
) : Regel<T>(produserer, emptyList()) {
    override fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: MutableSet<Regel<*>>,
        produsenter: Map<Opplysningstype<*>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        besøkt.add(this)
        if (opplysninger.har(produserer)) return
        plan.add(this)
    }

    override fun kjør(opplysninger: LesbarOpplysninger): T = verdi

    override fun toString() = "Bruker $verdi som utgangspunkt for $produserer"
}

fun <T : Comparable<T>> Opplysningstype<T>.somUtgangspunkt(verdi: T) = Utgangspunkt(this, verdi)

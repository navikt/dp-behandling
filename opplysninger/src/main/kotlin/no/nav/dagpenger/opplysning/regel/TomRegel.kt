package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class TomRegel<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<T>,
    avhengigheter: List<Opplysningstype<*>>,
) : Regel<T>(produserer, avhengigheter) {
    override fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: MutableSet<Regel<*>>,
        produsenter: Map<Opplysningstype<*>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        return
    }

    override fun kjør(opplysninger: LesbarOpplysninger): T = throw IllegalStateException("Kan ikke kjøres")

    override fun toString() = "Tom regel for $produserer"
}

val <T : Comparable<T>> Opplysningstype<T>.tomRegel get() = TomRegel(this, emptyList())

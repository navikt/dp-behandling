package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelplanlegger
import no.nav.dagpenger.opplysning.TreNode

class TomRegel<T : Any> internal constructor(
    produserer: Opplysningstype<T>,
    avhengigheter: List<Opplysningstype<Any>>,
) : Regel<T>(produserer, avhengigheter) {
    override fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
    ): TreNode<Plannode>? = null

    override fun kjør(opplysninger: LesbarOpplysninger): T = throw IllegalStateException("Kan ikke kjøres")

    override fun toString() = "Venter på ekstern verdi for $produserer"
}

val <T : Any> Opplysningstype<T>.tomRegel get() = TomRegel(this, emptyList())

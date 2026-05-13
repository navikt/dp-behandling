package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class TomRegel<T : Any> internal constructor(
    produserer: Opplysningstype<T>,
    avhengigheter: List<Opplysningstype<Any>>,
) : Regel<T>(produserer, avhengigheter) {
    override fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: MutableSet<Regel<*>>,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        return
    }

    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): T = throw IllegalStateException("Kan ikke kjøres")

    override fun toString() = "Tom regel for $produserer"
}

val <T : Any> Opplysningstype<T>.tomRegel get() = TomRegel(this, emptyList())

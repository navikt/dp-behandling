package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class Ekstern<T : Any> internal constructor(
    produserer: Opplysningstype<T>,
    avhengigheter: List<Opplysningstype<out Any>>,
) : Regel<T>(produserer, avhengigheter) {
    override fun kjør(opplysninger: LesbarOpplysninger): T = throw IllegalStateException("Kan ikke kjøres")

    override fun toString() = "Henter inn opplysning for $produserer med ${avhengerAv.joinToString { it.navn }} som avhengigheter."
}

fun <T : Any> Opplysningstype<T>.innhentMed(vararg opplysninger: Opplysningstype<out Any>) = Ekstern(this, opplysninger.toList())

val <T : Any> Opplysningstype<T>.innhentes get() = Ekstern(this, emptyList())

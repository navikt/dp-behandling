package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.Regel

open class Vilkår<T : Comparable<T>>(private val navn: String, block: Vilkår<T>.() -> Unit = {}) {
    private val opplysningstyper: MutableMap<String, Opplysningstype<*>> = mutableMapOf()
    private val regelsett = Regelsett("Regler")

    init {
        opplysningstyper[navn] = Opplysningstype<T>(navn.id("vilkår"))
        block()
    }

    fun regler(): Regelsett = regelsett

    fun vilkår(): Opplysningstype<T> {
        return (opplysningstyper[navn] ?: throw IllegalArgumentException("Finner ikke vilkåret")) as Opplysningstype<T>
    }

    fun opplysningstyper(): Map<String, Opplysningstype<*>> = opplysningstyper

    fun <O : Comparable<O>> hentOpplysningstype(navn: String): Opplysningstype<O> {
        return (opplysningstyper[navn] ?: throw IllegalArgumentException("Finner ikke opplysningstypen")) as Opplysningstype<O>
    }

    fun <O : Comparable<O>> opplysning(navn: String): Opplysningstype<O> {
        val opplysningstype = Opplysningstype<O>(navn)
        opplysningstyper[navn] = opplysningstype

        return opplysningstype
    }

    fun <O : Comparable<O>> Opplysningstype<O>.av(block: Opplysningstype<O>.() -> Regel<*>): Opplysningstype<O> {
        regelsett.regel(produserer = this, block = block)

        return this
    }
}

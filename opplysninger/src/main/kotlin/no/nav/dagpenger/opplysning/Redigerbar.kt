package no.nav.dagpenger.opplysning

fun interface Redigerbar {
    fun kanRedigere(opplysningstype: Opplysningstype<*>): Boolean
}

package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Opplysningstype

sealed interface RegelStatus {
    // Produktet finnes og er basert på gjeldende grunnlag
    data object Gyldig : RegelStatus

    // Produktet finnes uten utledning og skal ikke røres
    data object Overstyrt : RegelStatus

    // Alle avhengigheter finnes, regelen kan kjøres nå
    data object Kjørbar : RegelStatus

    // Regelen kan ikke kjøres før disse opplysningene finnes eller er oppdatert
    data class Blokkert(
        val venterPå: Set<Opplysningstype<*>>,
    ) : RegelStatus
}

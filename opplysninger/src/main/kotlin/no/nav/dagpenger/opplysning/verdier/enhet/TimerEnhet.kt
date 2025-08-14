package no.nav.dagpenger.opplysning.verdier.enhet

import no.nav.dagpenger.opplysning.Enhet

object TimerEnhet : Enhet<Double, Timer> {
    override val navn = "timer"

    override fun valider(verdi: Double) {
        require(verdi >= 0) { "Timer kan ikke være negativ. Verdi: $verdi" }
        require(verdi % 0.5 == 0.0) { "Timer må være hele og halve timer. Verdi: $verdi" }
    }

    override fun somEnhet(verdi: Double): Timer = Timer(verdi)
}

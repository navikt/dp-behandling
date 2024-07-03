package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentes

object Verneplikt {
    val avtjentVerneplikt = Opplysningstype.somBoolsk("Avtjent verneplikt".id("Verneplikt"))
    val vurderingAvVerneplikt =
        Opplysningstype.somBoolsk("Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste")

    val regelsett =
        Regelsett("Verneplikt") {
            regel(avtjentVerneplikt) { innhentes }
            regel(vurderingAvVerneplikt) { erSann(avtjentVerneplikt) }
        }

    val VernepliktKontroll =
        Kontrollpunkt(sjekker = Avklaringspunkter.Verneplikt) { opplysninger ->
            opplysninger.har(avtjentVerneplikt) && opplysninger.finnOpplysning(avtjentVerneplikt).verdi
        }
}

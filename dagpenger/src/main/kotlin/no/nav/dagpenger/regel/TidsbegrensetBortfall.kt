package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.regel.tomRegel
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper.antallBortfallsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.harBortfallId

object TidsbegrensetBortfall {
    val harBortfall = boolsk(harBortfallId, "Er ilagt tidsbegrenset bortfall av dagpenger")

    val antallBortfallsdager =
        Opplysningstype.heltall(antallBortfallsdagerId, "Antall dager med bortfall", enhet = Enhet.Dager)

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 10, "Tidsbegrenset bortfall av dagpenger", "Tidsbegrenset bortfall"),
        ) {
            skalVurderes { it.har(harBortfall) && it.erSann(harBortfall) }

            regel(harBortfall) { somUtgangspunkt(false) }
            regel(antallBortfallsdager) { tomRegel }
        }
}

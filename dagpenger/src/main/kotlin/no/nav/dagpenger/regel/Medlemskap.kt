package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadstidspunkt

object Medlemskap {
    val medlemFolketrygden = Opplysningstype.somBoolsk("Er personen medlem av folketrygden?")
    val oppfyllerMedlemskap = Opplysningstype.somBoolsk("Oppfyller personen vilkåret om medlemskap?")

    val regelsett =
        Regelsett(
            "Medlemskap",
        ) {
            regel(medlemFolketrygden) { oppslag(søknadstidspunkt) { true } }
            regel(oppfyllerMedlemskap) { erSann(medlemFolketrygden) }
        }
}

package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.brukerErUtestengtFraDagpengerId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravTilIkkeUtestengtId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object Utestengning {
    val utestengt = Opplysningstype.boolsk(brukerErUtestengtFraDagpengerId, "Bruker er utestengt fra dagpenger")
    val oppfyllerKravetTilIkkeUtestengt =
        Opplysningstype.boolsk(
            oppfyllerKravTilIkkeUtestengtId,
            "Oppfyller krav til ikke utestengt",
            synlig = aldriSynlig,
        )

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 28, "Utestengning", "Utestengning"),
        ) {
            skalVurderes { kravetTilMinsteinntektEllerVerneplikt(it) }

            regel(utestengt) { oppslag(prøvingsdato) { false } }
            utfall(oppfyllerKravetTilIkkeUtestengt) { ikke(utestengt) }

            påvirkerResultat { kravetTilMinsteinntektEllerVerneplikt(it) }
        }
}

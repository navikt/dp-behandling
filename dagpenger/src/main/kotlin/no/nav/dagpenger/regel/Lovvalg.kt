package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.erLovvalgNorgeId
import no.nav.dagpenger.regel.OpplysningsTyper.hvisIkkeNorgeId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object Lovvalg {
    val erLovvalgNorge = Opplysningstype.boolsk(erLovvalgNorgeId, "Er omfattet av trygdelovgivningen i Norge")
    val hvisIkkeNorge =
        Opplysningstype.tekst(hvisIkkeNorgeId, "Annet lovvalgsland", synlig = {
            it.har(erLovvalgNorge) && !it.finnOpplysning(erLovvalgNorge).verdi
        })

    val regelsett =
        vilkår(
            folketrygden.hjemmel(
                4,
                1,
                "Forholdet til bestemmelser om internasjonal trygdekoordinering",
                "Lovvalg",
            ),
        ) {
            skalVurderes { true }
            utfall(erLovvalgNorge) { oppslag(prøvingsdato) { true } }
            regel(hvisIkkeNorge) { oppslag(prøvingsdato) { "<annet land>" } }

            ønsketResultat(hvisIkkeNorge)
            påvirkerResultat {
                it.har(erLovvalgNorge)
            }
        }
}

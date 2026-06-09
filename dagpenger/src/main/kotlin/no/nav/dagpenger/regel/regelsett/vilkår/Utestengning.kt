package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.regel.Avklaringspunkter.Utestengt
import no.nav.dagpenger.regel.Behov
import no.nav.dagpenger.regel.OpplysningsTyper.brukerErUtestengtFraDagpengerId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravTilIkkeUtestengtId
import no.nav.dagpenger.regel.oppfyllerKravetTilMinsteinntektEllerVerneplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato

object Utestengning {
    val utestengt =
        Opplysningstype.boolsk(
            brukerErUtestengtFraDagpengerId,
            "Bruker er utestengt fra dagpenger",
            behovId = Behov.ErUtestengt,
        )
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
            skalVurderes { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            regel(utestengt) { innhentMed(prøvingsdato) }
            utfall(oppfyllerKravetTilIkkeUtestengt) { ikke(utestengt) }

            påvirkerResultat { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            avklaring(Utestengt)
        }

    val utestengtKontroll = Kontrollpunkt(Utestengt) { it.erSann(utestengt) }
}

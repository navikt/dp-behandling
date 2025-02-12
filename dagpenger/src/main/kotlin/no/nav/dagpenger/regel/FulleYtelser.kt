package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.IkkeFulleYtelserId
import no.nav.dagpenger.regel.Samordning.skalSamordnes
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.skalSamordnesUtenforFolketrygden

object FulleYtelser {
    val ikkeFulleYtelser = boolsk(IkkeFulleYtelserId, "Mottar ikke andre fulle ytelser")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(
                4,
                24,
                "Medlem som har fulle ytelser etter folketrygdloven eller avtalefestet pensjon",
                "Fulle ytelser",
            ),
        ) {
            skalVurderes { kravetTilMinsteinntektEllerVerneplikt(it) }

            utfall(ikkeFulleYtelser) { oppslag(prøvingsdato) { true } }

            påvirkerResultat { kravetTilMinsteinntektEllerVerneplikt(it) }
        }

    val FulleYtelserKontrollpunkt =
        Kontrollpunkt(sjekker = Avklaringspunkter.FulleYtelser) { opplysninger ->
            listOf(skalSamordnes, skalSamordnesUtenforFolketrygden).any {
                opplysninger.har(it) && opplysninger.finnOpplysning(it).verdi
            }
        }
}

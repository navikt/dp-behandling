package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.IkkeFulleYtelserId
import no.nav.dagpenger.regel.Samordning.skalSamordnes
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.skalSamordnesUtenforFolketrygden

object FulleYtelser {
    val ikkeFulleYtelser = boolsk(IkkeFulleYtelserId, "Mottar ikke andre fulle ytelser")

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(
                4,
                24,
                "Medlem som har fulle ytelser etter folketrygdloven eller avtalefestet pensjon",
                "Fulle ytelser",
            ),
        ) {
            skalKjøres { kravetTilMinsteinntektEllerVerneplikt(it) }

            utfall(ikkeFulleYtelser) { oppslag(prøvingsdato) { true } }

            relevantHvis { kravetTilMinsteinntektEllerVerneplikt(it) }

            ønsketResultat = listOf(ikkeFulleYtelser)
        }

    val FulleYtelserKontrollpunkt =
        Kontrollpunkt(sjekker = Avklaringspunkter.FulleYtelser) { opplysninger ->
            listOf(skalSamordnes, skalSamordnesUtenforFolketrygden).any {
                opplysninger.har(it) && opplysninger.finnOpplysning(it).verdi
            }
        }
}

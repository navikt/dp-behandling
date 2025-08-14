package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.IkkeFulleYtelserId
import no.nav.dagpenger.regel.Samordning.skalSamordnes
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.skalSamordnesUtenforFolketrygden

object FulleYtelser {
    val ikkeFulleYtelser = boolsk(IkkeFulleYtelserId, "Oppfyller vilkåret om å ikke motta andre fulle ytelser")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(
                4,
                24,
                "Medlem som har fulle ytelser etter folketrygdloven eller avtalefestet pensjon",
                "Fulle ytelser",
            ),
        ) {
            skalVurderes { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            utfall(ikkeFulleYtelser) { somUtgangspunkt(true) }

            påvirkerResultat { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }
        }

    val FulleYtelserKontrollpunkt =
        Kontrollpunkt(sjekker = Avklaringspunkter.FulleYtelser) { opplysninger ->
            listOf(skalSamordnes, skalSamordnesUtenforFolketrygden).any {
                opplysninger.har(it) && opplysninger.finnOpplysning(it).verdi
            }
        }
}

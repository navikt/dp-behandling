package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.ingenAv
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.deltarStreikEllerLockoutId
import no.nav.dagpenger.regel.OpplysningsTyper.ikkePåvirketAvStreikEllerLockoutId
import no.nav.dagpenger.regel.OpplysningsTyper.ledigVedSammeBedriftOgPåvirketAvUtfalletId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object StreikOgLockout {
    val deltarIStreikOgLockout =
        Opplysningstype.boolsk(
            deltarStreikEllerLockoutId,
            "Brukeren deltar i streik eller er omfattet av lock-out",
        )
    val sammeBedriftOgPåvirket =
        Opplysningstype.boolsk(
            ledigVedSammeBedriftOgPåvirketAvUtfalletId,
            "Brukeren er ledig ved samme bedrift eller arbeidsplass, og blir påvirket av utfallet",
        )

    val ikkeStreikEllerLockout =
        Opplysningstype.boolsk(
            ikkePåvirketAvStreikEllerLockoutId,
            "Brukeren er ikke påvirket av streik eller lock-out",
            synlig = aldriSynlig,
        )

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 22, "Bortfall ved streik og lock-out", "Streik og lock-out"),
        ) {
            skalVurderes { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            regel(deltarIStreikOgLockout) { oppslag(prøvingsdato) { false } }
            regel(sammeBedriftOgPåvirket) { oppslag(prøvingsdato) { false } }
            utfall(ikkeStreikEllerLockout) { ingenAv(deltarIStreikOgLockout, sammeBedriftOgPåvirket) }

            påvirkerResultat { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }
        }
}

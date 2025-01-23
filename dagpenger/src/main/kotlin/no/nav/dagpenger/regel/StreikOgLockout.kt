package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Regelsett
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
            "Deltar medlemmet i streik eller er omfattet av lock-out?",
        )
    val sammeBedriftOgPåvirket =
        Opplysningstype.boolsk(
            ledigVedSammeBedriftOgPåvirketAvUtfalletId,
            "Ledig ved samme bedrift eller arbeidsplass, og blir påvirket av utfallet?",
        )

    val ikkeStreikEllerLockout =
        Opplysningstype.boolsk(
            ikkePåvirketAvStreikEllerLockoutId,
            "Er medlemmet ikke påvirket av streik eller lock-out?",
            synlig = aldriSynlig,
        )

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 22, "Bortfall ved streik og lock-out", "4-22 Streik og lock-out"),
        ).apply {
            regel(deltarIStreikOgLockout) { oppslag(prøvingsdato) { false } }
            regel(sammeBedriftOgPåvirket) { oppslag(prøvingsdato) { false } }
            utfall(ikkeStreikEllerLockout) { ingenAv(deltarIStreikOgLockout, sammeBedriftOgPåvirket) }
        }
}

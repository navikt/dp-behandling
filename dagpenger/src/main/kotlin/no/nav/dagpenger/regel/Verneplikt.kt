package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.regel.OpplysningsTyper.avtjentVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravetTilVernepliktId
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst

object Verneplikt {
    val avtjentVerneplikt =
        boolsk(
            avtjentVernepliktId,
            "Avtjent verneplikt",
            formål = Opplysningsformål.Bruker,
            behovId = Behov.Verneplikt,
        )
    val oppfyllerKravetTilVerneplikt =
        boolsk(oppfyllerKravetTilVernepliktId, "Oppfyller kravet til verneplikt")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 19, "Dagpenger etter avtjent verneplikt", "Verneplikt"),
        ) {
            skalVurderes { it.oppfyller(Alderskrav.kravTilAlder) }

            regel(avtjentVerneplikt) { innhentMed(søknadIdOpplysningstype) }
            utfall(oppfyllerKravetTilVerneplikt) { erSann(avtjentVerneplikt) }

            avklaring(Avklaringspunkter.Verneplikt)

            påvirkerResultat {
                it.har(grunnlagForVernepliktErGunstigst) && it.finnOpplysning(grunnlagForVernepliktErGunstigst).verdi
            }
        }

    val VernepliktKontroll =
        Kontrollpunkt(sjekker = Avklaringspunkter.Verneplikt) { opplysninger ->
            opplysninger.har(avtjentVerneplikt) && opplysninger.finnOpplysning(avtjentVerneplikt).verdi
        }
}

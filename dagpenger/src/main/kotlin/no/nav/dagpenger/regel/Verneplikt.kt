package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
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
                val a = it.har(oppfyllerKravetTilVerneplikt) && it.finnOpplysning(oppfyllerKravetTilVerneplikt).verdi
                val b = it.har(grunnlagForVernepliktErGunstigst) && it.finnOpplysning(grunnlagForVernepliktErGunstigst).verdi
                val c = it.har(minsteinntekt) && it.finnOpplysning(minsteinntekt).verdi
                val d = it.har(avtjentVerneplikt) && it.finnOpplysning(avtjentVerneplikt).verdi

                (a && b) || (a && !c) || (d && !a && !b && !c)
            }
        }

    val VernepliktKontroll =
        Kontrollpunkt(sjekker = Avklaringspunkter.Verneplikt) { opplysninger ->
            opplysninger.har(avtjentVerneplikt) && opplysninger.finnOpplysning(avtjentVerneplikt).verdi
        }
}

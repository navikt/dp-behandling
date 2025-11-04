package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.OpplysningsTyper.avtjentVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravetTilVernepliktId
import no.nav.dagpenger.regel.Rettighetstype.skalVernepliktVurderes
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
    val oppfyllerKravetTilVerneplikt = boolsk(oppfyllerKravetTilVernepliktId, "Oppfyller kravet til verneplikt")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 19, "Dagpenger etter avtjent verneplikt", "Verneplikt"),
        ) {
            skalVurderes {
                it.oppfyller(kravTilAlder)
                // && it.erSann(skalVernepliktVurderes)
            }

            regel(avtjentVerneplikt) { innhentMed(søknadIdOpplysningstype) }
            utfall(oppfyllerKravetTilVerneplikt) { erSann(avtjentVerneplikt) }

            avklaring(Avklaringspunkter.Verneplikt)

            påvirkerResultat {
                val skalVurderes = it.erSann(skalVernepliktVurderes)
                val oppfyllerVerneplikt = it.erSann(oppfyllerKravetTilVerneplikt)
                val vernepliktErGunstigst = it.erSann(grunnlagForVernepliktErGunstigst)
                val oppfyllerMinsteinntekt = it.erSann(minsteinntekt)
                val søktOmVerneplikt = it.erSann(avtjentVerneplikt)

                if (!skalVurderes) {
                    return@påvirkerResultat false
                }

                if (skalVurderes && (vernepliktErGunstigst || !oppfyllerMinsteinntekt)) {
                    return@påvirkerResultat true
                }

                if (søktOmVerneplikt && !oppfyllerVerneplikt && !vernepliktErGunstigst && !oppfyllerMinsteinntekt) {
                    return@påvirkerResultat true
                }

                return@påvirkerResultat false
            }
        }

    val VernepliktKontroll =
        Kontrollpunkt(sjekker = Avklaringspunkter.Verneplikt) { opplysninger ->
            opplysninger.har(avtjentVerneplikt) && opplysninger.finnOpplysning(avtjentVerneplikt).verdi
        }
}

package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.prosentTerskel
import no.nav.dagpenger.regel.Avklaringspunkter.TapAvArbeidstidBeregningsregel
import no.nav.dagpenger.regel.Behov.HarTaptArbeid
import no.nav.dagpenger.regel.Behov.KravPåLønn
import no.nav.dagpenger.regel.ReellArbeidssøker.ønsketArbeidstid
import no.nav.dagpenger.regel.Samordning.samordnetBeregnetArbeidstid
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.vernepliktFastsattVanligArbeidstid

object TapAvArbeidsinntektOgArbeidstid {
    internal val tapAvArbeid = Opplysningstype.somBoolsk("Har tapt arbeid".id(HarTaptArbeid))
    internal val kravPåLønn = Opplysningstype.somBoolsk("Krav på lønn fra tidligere arbeidsgiver".id(KravPåLønn))
    private val ikkeKravPåLønn = Opplysningstype.somBoolsk("Ikke krav på lønn fra tidligere arbeidsgiver", synlig = aldriSynlig)
    val kravTilTapAvArbeidsinntekt = Opplysningstype.somBoolsk("Krav til tap av arbeidsinntekt")

    private val kravTilArbeidstidsreduksjon = Opplysningstype.somDesimaltall("Krav til prosentvis tap av arbeidstid")
    private val beregningsregel = Opplysningstype.somBoolsk("Beregningsregel: Tapt arbeidstid", synlig = aldriSynlig)

    val beregningsregel6mnd = Opplysningstype.somBoolsk("Beregningsregel: Arbeidstid siste 6 måneder")
    val beregningsregel12mnd = Opplysningstype.somBoolsk("Beregningsregel: Arbeidstid siste 12 måneder")
    val beregningsregel36mnd = Opplysningstype.somBoolsk("Beregeningsregel: Arbeidstid siste 36 måneder")

    val beregnetArbeidstid = Opplysningstype.somDesimaltall("Beregnet vanlig arbeidstid per uke før tap")
    private val maksimalVanligArbeidstid = Opplysningstype.somDesimaltall("Maksimal vanlig arbeidstid", synlig = aldriSynlig)
    val fastsattVanligArbeidstid = Opplysningstype.somDesimaltall("Fastsatt arbeidstid per uke før tap")
    val nyArbeidstid = Opplysningstype.somDesimaltall("Ny arbeidstid per uke")

    internal val ordinærEllerVernepliktArbeidstid =
        Opplysningstype.somDesimaltall("Fastsatt vanlig arbeidstid etter ordinær eller verneplikt", synlig = aldriSynlig)
    val kravTilTaptArbeidstid = Opplysningstype.somBoolsk("Tap av arbeidstid er minst terskel")
    val kravTilTapAvArbeidsinntektOgArbeidstid = Opplysningstype.somBoolsk("Krav til tap av arbeidsinntekt og arbeidstid")

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 3, "Krav til tap av arbeidsinntekt og arbeidstid", "4-3 Tap av arbeidsinntekt og arbeidstid"),
        ) {
            regel(tapAvArbeid) { oppslag(prøvingsdato) { true } } // TODO: Satt til true for testing av innvilgelse
            regel(kravPåLønn) { oppslag(prøvingsdato) { false } }
            regel(ikkeKravPåLønn) { ikke(kravPåLønn) }
            regel(kravTilTapAvArbeidsinntekt) { alle(tapAvArbeid, ikkeKravPåLønn) }

            regel(kravTilArbeidstidsreduksjon) { oppslag(prøvingsdato) { 50.0 } } // Perm og sånt har andre terskelverdier

            // TODO: Kun en av disse må være sann. Enforces med Avklaring (i framtiden)
            regel(beregningsregel6mnd) { oppslag(prøvingsdato) { true } } // TODO: Satt til true for testing av innvilgelse
            regel(beregningsregel12mnd) { oppslag(prøvingsdato) { false } }
            regel(beregningsregel36mnd) { oppslag(prøvingsdato) { false } }

            // TODO: Bør hentes fra noe
            regel(beregnetArbeidstid) { oppslag(prøvingsdato) { 37.5 } } // TODO: Satt til 37.5 for testing av innvilgelse

            // FVA fra verneplikt overstyrer ordinær FVA om verneplikt er gunstigst
            regel(ordinærEllerVernepliktArbeidstid) {
                hvisSannMedResultat(grunnlagForVernepliktErGunstigst, vernepliktFastsattVanligArbeidstid, beregnetArbeidstid)
            }

            regel(nyArbeidstid) { oppslag(prøvingsdato) { 0.0 } }
            regel(maksimalVanligArbeidstid) { oppslag(prøvingsdato) { 40.0 } }

            regel(fastsattVanligArbeidstid) {
                minstAv(
                    maksimalVanligArbeidstid,
                    ordinærEllerVernepliktArbeidstid,
                    samordnetBeregnetArbeidstid,
                    ønsketArbeidstid,
                )
            }

            regel(kravTilTaptArbeidstid) { prosentTerskel(nyArbeidstid, fastsattVanligArbeidstid, kravTilArbeidstidsreduksjon) }

            regel(beregningsregel) { enAv(beregningsregel6mnd, beregningsregel12mnd, beregningsregel36mnd) }

            utfall(kravTilTapAvArbeidsinntektOgArbeidstid) {
                alle(kravTilTapAvArbeidsinntekt, kravTilTaptArbeidstid, beregningsregel)
            }

            avklaring(TapAvArbeidstidBeregningsregel)
        }

    val TapArbeidstidBeregningsregelKontroll =
        Kontrollpunkt(sjekker = TapAvArbeidstidBeregningsregel) { opplysninger ->
            if (opplysninger.mangler(beregnetArbeidstid)) {
                return@Kontrollpunkt false
            }

            listOf(
                opplysninger.har(beregningsregel6mnd) && opplysninger.finnOpplysning(beregningsregel6mnd).verdi,
                opplysninger.har(beregningsregel12mnd) && opplysninger.finnOpplysning(beregningsregel12mnd).verdi,
                opplysninger.har(beregningsregel36mnd) && opplysninger.finnOpplysning(beregningsregel36mnd).verdi,
            ).count { it } != 1
        }
}

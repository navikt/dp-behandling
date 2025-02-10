package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.prosentTerskel
import no.nav.dagpenger.regel.Avklaringspunkter.TapAvArbeidstidBeregningsregel
import no.nav.dagpenger.regel.Behov.HarTaptArbeid
import no.nav.dagpenger.regel.Behov.KravPåLønn
import no.nav.dagpenger.regel.OpplysningsTyper.beregeningsregelArbeidstidSiste36MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.beregnetVanligArbeidstidPerUkeFørTapId
import no.nav.dagpenger.regel.OpplysningsTyper.beregningsregelArbeidstidSiste12MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.beregningsregelArbeidstidSiste6MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.beregningsregelTaptArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.fastsattVanligArbeidstidEtterOrdinærEllerVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.harTaptArbeidId
import no.nav.dagpenger.regel.OpplysningsTyper.ikkeKravPåLønnFraTidligereArbeidsgiverId
import no.nav.dagpenger.regel.OpplysningsTyper.kravPåLønnId
import no.nav.dagpenger.regel.OpplysningsTyper.kravTilProsentvisTapAvArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.kravTilTapAvArbeidsinntektId
import no.nav.dagpenger.regel.OpplysningsTyper.kravTilTapAvArbeidsinntektOgArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.maksimalVanligArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.nyArbeidstidPerUkeId
import no.nav.dagpenger.regel.OpplysningsTyper.tapAvArbeidstidErMinstTerskelId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.vernepliktFastsattVanligArbeidstid

object TapAvArbeidsinntektOgArbeidstid {
    internal val tapAvArbeid = Opplysningstype.boolsk(harTaptArbeidId, "Har tapt arbeid", behovId = HarTaptArbeid)
    val kravPåLønn = Opplysningstype.boolsk(kravPåLønnId, "Krav på lønn fra tidligere arbeidsgiver", behovId = KravPåLønn)
    private val ikkeKravPåLønn =
        Opplysningstype.boolsk(
            ikkeKravPåLønnFraTidligereArbeidsgiverId,
            "Ikke krav på lønn fra tidligere arbeidsgiver",
            synlig = aldriSynlig,
        )
    val kravTilTapAvArbeidsinntekt = Opplysningstype.boolsk(kravTilTapAvArbeidsinntektId, "Krav til tap av arbeidsinntekt")

    private val kravTilArbeidstidsreduksjon =
        Opplysningstype.desimaltall(
            kravTilProsentvisTapAvArbeidstidId,
            "Krav til prosentvis tap av arbeidstid",
        )
    private val beregningsregel =
        Opplysningstype.boolsk(
            beregningsregelTaptArbeidstidId,
            "Beregningsregel: Tapt arbeidstid",
            synlig = aldriSynlig,
        )

    val beregningsregel6mnd =
        Opplysningstype.boolsk(
            beregningsregelArbeidstidSiste6MånederId,
            "Beregningsregel: Arbeidstid siste 6 måneder",
        )
    val beregningsregel12mnd =
        Opplysningstype.boolsk(
            beregningsregelArbeidstidSiste12MånederId,
            "Beregningsregel: Arbeidstid siste 12 måneder",
        )
    val beregningsregel36mnd =
        Opplysningstype.boolsk(
            beregeningsregelArbeidstidSiste36MånederId,
            "Beregningsregel: Arbeidstid siste 36 måneder",
        )

    val beregnetArbeidstid =
        Opplysningstype.desimaltall(
            beregnetVanligArbeidstidPerUkeFørTapId,
            "Beregnet vanlig arbeidstid per uke før tap",
        )
    val maksimalVanligArbeidstid =
        Opplysningstype.desimaltall(
            maksimalVanligArbeidstidId,
            "Maksimal vanlig arbeidstid",
            synlig = aldriSynlig,
        )

    val nyArbeidstid = Opplysningstype.desimaltall(nyArbeidstidPerUkeId, "Ny arbeidstid per uke")

    internal val ordinærEllerVernepliktArbeidstid =
        Opplysningstype.desimaltall(
            fastsattVanligArbeidstidEtterOrdinærEllerVernepliktId,
            "Fastsatt vanlig arbeidstid etter ordinær eller verneplikt",
            synlig = aldriSynlig,
        )
    val kravTilTaptArbeidstid = Opplysningstype.boolsk(tapAvArbeidstidErMinstTerskelId, "Tap av arbeidstid er minst terskel")
    val kravTilTapAvArbeidsinntektOgArbeidstid =
        Opplysningstype.boolsk(
            kravTilTapAvArbeidsinntektOgArbeidstidId,
            "Krav til tap av arbeidsinntekt og arbeidstid",
        )

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 3, "Krav til tap av arbeidsinntekt og arbeidstid", "Tap av arbeidsinntekt og arbeidstid"),
        ) {
            skalKjøres { kravetTilMinsteinntektEllerVerneplikt(it) }

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

            regel(kravTilTaptArbeidstid) { prosentTerskel(nyArbeidstid, fastsattVanligArbeidstid, kravTilArbeidstidsreduksjon) }

            regel(beregningsregel) { enAv(beregningsregel6mnd, beregningsregel12mnd, beregningsregel36mnd) }

            utfall(kravTilTapAvArbeidsinntektOgArbeidstid) {
                alle(kravTilTapAvArbeidsinntekt, kravTilTaptArbeidstid, beregningsregel)
            }

            avklaring(TapAvArbeidstidBeregningsregel)

            relevantHvis { kravetTilMinsteinntektEllerVerneplikt(it) }
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

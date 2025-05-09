package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.prosentTerskel
import no.nav.dagpenger.regel.Avklaringspunkter.BeregnetArbeidstid
import no.nav.dagpenger.regel.Avklaringspunkter.TapAvArbeidstidBeregningsregel
import no.nav.dagpenger.regel.Behov.HarTaptArbeid
import no.nav.dagpenger.regel.Behov.KravPåLønn
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstidsreduksjonIkkeBruktTidligereId
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
import no.nav.dagpenger.regel.OpplysningsTyper.ordinærtKravTilTaptArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.tapAvArbeidstidErMinstTerskelId
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.kravTilArbeidstidsreduksjonVedFiskepermittering
import no.nav.dagpenger.regel.Rettighetstype.permitteringFiskeforedling
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

    val kravTilArbeidstidsreduksjon =
        desimaltall(
            kravTilProsentvisTapAvArbeidstidId,
            "Krav til prosentvis tap av arbeidstid",
        )

    val arbeidstidsreduksjonIkkeBruktTidligere =
        boolsk(
            arbeidstidsreduksjonIkkeBruktTidligereId,
            "Arbeidstidsreduksjonen er ikke brukt tidligere i en full stønadsperiode",
        )

    private val ordinærtKravTilTaptArbeidstid =
        desimaltall(
            ordinærtKravTilTaptArbeidstidId,
            "Ordinært krav til prosentvis tap av arbeidstid",
            synlig = aldriSynlig,
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
        desimaltall(
            beregnetVanligArbeidstidPerUkeFørTapId,
            "Beregnet vanlig arbeidstid per uke før tap",
        )
    val maksimalVanligArbeidstid =
        desimaltall(
            maksimalVanligArbeidstidId,
            "Maksimal vanlig arbeidstid",
            synlig = aldriSynlig,
        )

    val nyArbeidstid = desimaltall(nyArbeidstidPerUkeId, "Ny arbeidstid per uke")

    internal val ordinærEllerVernepliktArbeidstid =
        desimaltall(
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
        vilkår(
            folketrygden.hjemmel(4, 3, "Krav til tap av arbeidsinntekt og arbeidstid", "Tap av arbeidsinntekt og arbeidstid"),
        ) {
            skalVurderes { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            regel(tapAvArbeid) { oppslag(prøvingsdato) { true } } // TODO: Satt til true for testing av innvilgelse
            regel(kravPåLønn) { oppslag(prøvingsdato) { false } }
            regel(ikkeKravPåLønn) { ikke(kravPåLønn) }
            utfall(kravTilTapAvArbeidsinntekt) { alle(tapAvArbeid, ikkeKravPåLønn) }

            regel(ordinærtKravTilTaptArbeidstid) { oppslag(prøvingsdato) { 50.0 } }

            regel(kravTilArbeidstidsreduksjon) {
                hvisSannMedResultat(
                    permitteringFiskeforedling,
                    kravTilArbeidstidsreduksjonVedFiskepermittering,
                    ordinærtKravTilTaptArbeidstid,
                )
            }

            // TODO: Kun en av disse må være sann. Enforces med Avklaring (i gang framtiden)
            regel(beregningsregel6mnd) { oppslag(prøvingsdato) { true } } // TODO: Satt til true for testing av innvilgelse
            regel(beregningsregel12mnd) { oppslag(prøvingsdato) { false } }
            regel(beregningsregel36mnd) { oppslag(prøvingsdato) { false } }

            // TODO: Bør hentes fra noe
            regel(beregnetArbeidstid) { oppslag(prøvingsdato) { 37.5 } }

            regel(arbeidstidsreduksjonIkkeBruktTidligere) { oppslag(prøvingsdato) { true } }

            // FVA fra verneplikt overstyrer ordinær FVA om verneplikt er gunstigst
            regel(ordinærEllerVernepliktArbeidstid) {
                hvisSannMedResultat(grunnlagForVernepliktErGunstigst, vernepliktFastsattVanligArbeidstid, beregnetArbeidstid)
            }

            regel(nyArbeidstid) { oppslag(prøvingsdato) { 0.0 } }
            regel(maksimalVanligArbeidstid) { oppslag(prøvingsdato) { 40.0 } }

            utfall(kravTilTaptArbeidstid) { prosentTerskel(nyArbeidstid, fastsattVanligArbeidstid, kravTilArbeidstidsreduksjon) }

            regel(beregningsregel) { enAv(beregningsregel6mnd, beregningsregel12mnd, beregningsregel36mnd) }

            utfall(kravTilTapAvArbeidsinntektOgArbeidstid) {
                alle(kravTilTapAvArbeidsinntekt, kravTilTaptArbeidstid, beregningsregel, arbeidstidsreduksjonIkkeBruktTidligere)
            }

            avklaring(TapAvArbeidstidBeregningsregel)
            avklaring(BeregnetArbeidstid)

            påvirkerResultat { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }
        }

    val beregnetArbeidstidKontroll =
        Kontrollpunkt(sjekker = BeregnetArbeidstid) { opplysninger ->
            opplysninger.har(beregnetArbeidstid)
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

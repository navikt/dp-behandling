package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.prosentTerskel
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.Avklaringspunkter.BeregnetArbeidstid
import no.nav.dagpenger.regel.Avklaringspunkter.TapAvArbeidstidBeregningsregel
import no.nav.dagpenger.regel.Behov.AndreØkonomiskeYtelser
import no.nav.dagpenger.regel.Behov.HarTaptArbeid
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstidsreduksjonIkkeBruktTidligereId
import no.nav.dagpenger.regel.OpplysningsTyper.beregeningsregelArbeidstidSiste36MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.beregnetVanligArbeidstidPerUkeFørTapId
import no.nav.dagpenger.regel.OpplysningsTyper.beregningsregelArbeidstidSiste12MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.beregningsregelArbeidstidSiste6MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.beregningsregelTaptArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.fastsattVanligArbeidstidEtterOrdinærEllerVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.harTaptArbeidId
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
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.vernepliktFastsattVanligArbeidstid

object TapAvArbeidsinntektOgArbeidstid {
    // Ligger igjen så opplysningen ikke er synlig for gamle behandlinger
    private val tapAvArbeid = boolsk(harTaptArbeidId, "Har tapt arbeid", behovId = HarTaptArbeid, synlig = aldriSynlig)

    val kravPåLønn =
        boolsk(
            kravPåLønnId,
            "Har krav på lønn fra arbeidsgiver",
            behovId = AndreØkonomiskeYtelser,
            formål = Opplysningsformål.Bruker,
        )
    val kravTilTapAvArbeidsinntekt = boolsk(kravTilTapAvArbeidsinntektId, "Oppfyller vilkåret til tap av arbeidsinntekt")

    val kravTilArbeidstidsreduksjon =
        desimaltall(
            kravTilProsentvisTapAvArbeidstidId,
            "Krav til prosentvis tap av arbeidstid",
            enhet = Enhet.Prosent,
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
            enhet = Enhet.Prosent,
        )

    private val beregningsregel =
        boolsk(
            beregningsregelTaptArbeidstidId,
            "Beregningsregel: Tapt arbeidstid",
            synlig = aldriSynlig,
        )

    val beregningsregel6mnd =
        boolsk(
            beregningsregelArbeidstidSiste6MånederId,
            "Beregningsregel: Arbeidstid siste 6 måneder",
        )
    val beregningsregel12mnd =
        boolsk(
            beregningsregelArbeidstidSiste12MånederId,
            "Beregningsregel: Arbeidstid siste 12 måneder",
        )
    val beregningsregel36mnd =
        boolsk(
            beregeningsregelArbeidstidSiste36MånederId,
            "Beregningsregel: Arbeidstid siste 36 måneder",
        )

    val beregnetArbeidstid =
        desimaltall(
            beregnetVanligArbeidstidPerUkeFørTapId,
            "Beregnet vanlig arbeidstid per uke før tap",
            enhet = Enhet.Timer,
        )
    val maksimalVanligArbeidstid =
        desimaltall(
            maksimalVanligArbeidstidId,
            "Maksimal vanlig arbeidstid",
            synlig = aldriSynlig,
            enhet = Enhet.Timer,
        )

    val nyArbeidstid = desimaltall(nyArbeidstidPerUkeId, "Ny arbeidstid per uke", enhet = Enhet.Timer)

    internal val ordinærEllerVernepliktArbeidstid =
        desimaltall(
            fastsattVanligArbeidstidEtterOrdinærEllerVernepliktId,
            "Fastsatt vanlig arbeidstid etter ordinær eller verneplikt",
            synlig = aldriSynlig,
            enhet = Enhet.Timer,
        )
    val kravTilTaptArbeidstid = boolsk(tapAvArbeidstidErMinstTerskelId, "Oppfyller vilkåret om tap av arbeidstid")
    val kravTilTapAvArbeidsinntektOgArbeidstid =
        boolsk(kravTilTapAvArbeidsinntektOgArbeidstidId, "Oppfyller vilkåret om tap av arbeidsinntekt og arbeidstid", synlig = aldriSynlig)

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 3, "Tap av arbeidsinntekt og arbeidstid", "Tap av arbeidsinntekt og arbeidstid"),
        ) {
            skalVurderes { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            regel(kravPåLønn) { innhentMed(søknadIdOpplysningstype) }
            betingelse(kravTilTapAvArbeidsinntekt) { ikke(kravPåLønn) }

            regel(ordinærtKravTilTaptArbeidstid) { oppslag(prøvingsdato) { 50.0 } }

            regel(kravTilArbeidstidsreduksjon) {
                hvisSannMedResultat(
                    permitteringFiskeforedling,
                    kravTilArbeidstidsreduksjonVedFiskepermittering,
                    ordinærtKravTilTaptArbeidstid,
                )
            }

            // TODO: Dette bør bli en ENUM en gang i framtiden
            regel(beregningsregel6mnd) { somUtgangspunkt(true) }
            regel(beregningsregel12mnd) { somUtgangspunkt(false) }
            regel(beregningsregel36mnd) { somUtgangspunkt(false) }

            // TODO: Bør hentes fra noe, f.eks. innbyggerflate
            regel(beregnetArbeidstid) { somUtgangspunkt(37.5) }
            regel(arbeidstidsreduksjonIkkeBruktTidligere) { somUtgangspunkt(true) }

            // FVA fra verneplikt overstyrer ordinær FVA om verneplikt er gunstigst
            regel(ordinærEllerVernepliktArbeidstid) {
                hvisSannMedResultat(grunnlagForVernepliktErGunstigst, vernepliktFastsattVanligArbeidstid, beregnetArbeidstid)
            }

            regel(nyArbeidstid) { somUtgangspunkt(0.0) }
            regel(maksimalVanligArbeidstid) { oppslag(prøvingsdato) { 40.0 } }

            betingelse(kravTilTaptArbeidstid) { prosentTerskel(nyArbeidstid, fastsattVanligArbeidstid, kravTilArbeidstidsreduksjon) }

            regel(beregningsregel) { enAv(beregningsregel6mnd, beregningsregel12mnd, beregningsregel36mnd) }

            utfall(kravTilTapAvArbeidsinntektOgArbeidstid) {
                alle(kravTilTapAvArbeidsinntekt, kravTilTaptArbeidstid, beregningsregel, arbeidstidsreduksjonIkkeBruktTidligere)
            }

            avklaring(TapAvArbeidstidBeregningsregel)
            avklaring(BeregnetArbeidstid)

            påvirkerResultat {
                it.har(kravTilTapAvArbeidsinntektOgArbeidstid)
            }
        }

    val beregnetArbeidstidKontroll =
        Kontrollpunkt(sjekker = BeregnetArbeidstid) { opplysninger ->
            opplysninger.har(beregnetArbeidstid) &&
                opplysninger.finnOpplysning(beregnetArbeidstid).kilde !is Saksbehandlerkilde
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

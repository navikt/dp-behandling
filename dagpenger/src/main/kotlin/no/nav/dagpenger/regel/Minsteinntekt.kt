package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.grunnbelop.Regel
import no.nav.dagpenger.grunnbelop.forDato
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.opplysning.Opplysningsformål.Register
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.beløp
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.inntekt
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.dato.trekkFraMånedTilFørste
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.inntekt.SummerPeriode
import no.nav.dagpenger.opplysning.regel.inntekt.filtrerRelevanteInntekter
import no.nav.dagpenger.opplysning.regel.inntekt.summerPeriode
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Behov.Inntekt
import no.nav.dagpenger.regel.Behov.OpptjeningsperiodeFraOgMed
import no.nav.dagpenger.regel.GrenseverdierForMinsteArbeidsinntekt.finnTerskel
import no.nav.dagpenger.regel.OpplysningsTyper.BruttoArbeidsinntektId
import no.nav.dagpenger.regel.OpplysningsTyper.FørsteMånedAvOpptjeningsperiodeId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnbeløpId
import no.nav.dagpenger.regel.OpplysningsTyper.InntektSiste12MndId
import no.nav.dagpenger.regel.OpplysningsTyper.InntektSiste36MndId
import no.nav.dagpenger.regel.OpplysningsTyper.Inntektskrav12mndId
import no.nav.dagpenger.regel.OpplysningsTyper.Inntektskrav36MndId
import no.nav.dagpenger.regel.OpplysningsTyper.InntektsopplysningerId
import no.nav.dagpenger.regel.OpplysningsTyper.KravTilMinsteinntektId
import no.nav.dagpenger.regel.OpplysningsTyper.MaksPeriodeLengdeId
import no.nav.dagpenger.regel.OpplysningsTyper.Over12mndTerskelId
import no.nav.dagpenger.regel.OpplysningsTyper.Over36mndTerskelId
import no.nav.dagpenger.regel.OpplysningsTyper.TerskelFaktor12MndId
import no.nav.dagpenger.regel.OpplysningsTyper.TerskelFaktor36MndId
import no.nav.dagpenger.regel.Opptjeningstid.justertRapporteringsfrist
import no.nav.dagpenger.regel.Opptjeningstid.sisteAvsluttendendeKalenderMåned
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import java.time.LocalDate

object Minsteinntekt {
    private val `12mndTerskelFaktor` =
        desimaltall(TerskelFaktor12MndId, "Antall G for krav til 12 mnd arbeidsinntekt", synlig = aldriSynlig)
    private val `36mndTerskelFaktor` =
        desimaltall(TerskelFaktor36MndId, "Antall G for krav til 36 mnd arbeidsinntekt", synlig = aldriSynlig)
    val inntekt12 = beløp(InntektSiste12MndId, "Arbeidsinntekt siste 12 måneder", behovId = "InntektSiste12Mnd")
    val inntekt36 = beløp(InntektSiste36MndId, "Arbeidsinntekt siste 36 måneder", behovId = "InntektSiste36Mnd")
    val grunnbeløp = beløp(GrunnbeløpId, "Grunnbeløp")

    val inntektFraSkatt = inntekt(InntektsopplysningerId, beskrivelse = "Inntektsopplysninger", Register, behovId = Inntekt)
    private val tellendeInntekt = inntekt(BruttoArbeidsinntektId, "Brutto arbeidsinntekt", synlig = aldriSynlig)

    private val maksPeriodeLengde = heltall(MaksPeriodeLengdeId, "Maks lengde på opptjeningsperiode", synlig = aldriSynlig)
    private val førsteMånedAvOpptjeningsperiode =
        dato(FørsteMånedAvOpptjeningsperiodeId, beskrivelse = "Første måned av opptjeningsperiode", behovId = OpptjeningsperiodeFraOgMed)

    private val `12mndTerskel` = beløp(Inntektskrav12mndId, "Inntektskrav for siste 12 måneder")
    private val `36mndTerskel` = beløp(Inntektskrav36MndId, "Inntektskrav for siste 36 måneder")
    private val over12mndTerskel = boolsk(Over12mndTerskelId, "Arbeidsinntekt er over kravet for siste 12 måneder")
    private val over36mndTerskel = boolsk(Over36mndTerskelId, "Arbeidsinntekt er over kravet for siste 36 måneder")

    val minsteinntekt = boolsk(KravTilMinsteinntektId, "Oppfyller kravet til minsteinntekt")

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 4, "Krav til minsteinntekt", "Minsteinntekt")) {
            skalVurderes { it.oppfyller(kravTilAlder) }

            regel(maksPeriodeLengde) { oppslag(prøvingsdato) { 36 } }
            regel(førsteMånedAvOpptjeningsperiode) { trekkFraMånedTilFørste(sisteAvsluttendendeKalenderMåned, maksPeriodeLengde) }

            regel(inntektFraSkatt) { innhentMed(prøvingsdato, sisteAvsluttendendeKalenderMåned, førsteMånedAvOpptjeningsperiode) }

            regel(tellendeInntekt) { filtrerRelevanteInntekter(inntektFraSkatt, listOf(InntektKlasse.ARBEIDSINNTEKT)) }

            regel(grunnbeløp) { oppslag(prøvingsdato) { grunnbeløpFor(it) } }

            regel(inntekt12) { summerPeriode(tellendeInntekt, SummerPeriode.InntektPeriode.Første) }
            regel(`12mndTerskelFaktor`) { oppslag(prøvingsdato) { finnTerskel(it).nedre } }
            regel(`12mndTerskel`) { multiplikasjon(grunnbeløp, `12mndTerskelFaktor`) }
            regel(over12mndTerskel) { størreEnnEllerLik(inntekt12, `12mndTerskel`) }

            regel(inntekt36) {
                summerPeriode(
                    tellendeInntekt,
                    SummerPeriode.InntektPeriode.Første,
                    SummerPeriode.InntektPeriode.Andre,
                    SummerPeriode.InntektPeriode.Tredje,
                )
            }
            regel(`36mndTerskelFaktor`) { oppslag(prøvingsdato) { finnTerskel(it).øvre } }
            regel(`36mndTerskel`) { multiplikasjon(grunnbeløp, `36mndTerskelFaktor`) }
            regel(over36mndTerskel) { størreEnnEllerLik(inntekt36, `36mndTerskel`) }

            utfall(minsteinntekt) { enAv(over12mndTerskel, over36mndTerskel) }

            avklaring(Avklaringspunkter.SvangerskapsrelaterteSykepenger)
            avklaring(Avklaringspunkter.InntektNesteKalendermåned)
            avklaring(Avklaringspunkter.ØnskerEtterRapporteringsfrist)

            påvirkerResultat {
                // Hvis alder ikke er oppfylt, er minsteinntekt ikke relevant
                if (!it.erSann(kravTilAlder)) return@påvirkerResultat false

                // Hvis alder er oppfylt, er minsteinntekt relevant hvis:
                // - Inntekt er oppfylt, eller
                // - Verneplikt er oppfylt samtidig som inntekt ikke er nødvendig
                if (it.erSann(minsteinntekt)) return@påvirkerResultat true
                if (it.erSann(grunnlagForVernepliktErGunstigst)) return@påvirkerResultat false

                true
            }
        }

    private fun grunnbeløpFor(it: LocalDate) =
        getGrunnbeløpForRegel(Regel.Minsteinntekt)
            .forDato(it)
            .verdi
            .let { Beløp(it) }

    val SvangerskapsrelaterteSykepengerKontroll =
        Kontrollpunkt(Avklaringspunkter.SvangerskapsrelaterteSykepenger) {
            it.har(inntektFraSkatt) && it.finnOpplysning(minsteinntekt).verdi == false
        }

    val EØSArbeidKontroll =
        Kontrollpunkt(Avklaringspunkter.EØSArbeid) { it.har(inntektFraSkatt) }

    val JobbetUtenforNorgeKontroll =
        Kontrollpunkt(Avklaringspunkter.JobbetUtenforNorge) { it.har(inntektFraSkatt) }

    val InntektNesteKalendermånedKontroll =
        Kontrollpunkt(Avklaringspunkter.InntektNesteKalendermåned) { it.har(inntektFraSkatt) }

    val PrøverEtterRapporteringsfristKontroll =
        Kontrollpunkt(Avklaringspunkter.PrøvingsdatoEtterRapporteringsfrist) {
            if (!it.har(justertRapporteringsfrist)) return@Kontrollpunkt false
            if (!it.har(søknadstidspunkt)) return@Kontrollpunkt false
            if (!it.har(søknadsdato)) return@Kontrollpunkt false

            val rapporteringsfrist = it.finnOpplysning(justertRapporteringsfrist).verdi
            val prøvingsdato = it.finnOpplysning(prøvingsdato).verdi
            val behandlingstidspunkt = LocalDate.now()

            prøvingsdato > rapporteringsfrist && behandlingstidspunkt <= rapporteringsfrist
        }
}

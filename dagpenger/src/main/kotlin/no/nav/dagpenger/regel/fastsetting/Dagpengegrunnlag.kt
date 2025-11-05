package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.grunnbelop.forDato
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.opplysning.Opplysningsformål.Legacy
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.avrund
import no.nav.dagpenger.opplysning.regel.brukt
import no.nav.dagpenger.opplysning.regel.divisjon
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.høyesteAv
import no.nav.dagpenger.opplysning.regel.inntekt.SummerPeriode
import no.nav.dagpenger.opplysning.regel.inntekt.filtrerRelevanteInntekter
import no.nav.dagpenger.opplysning.regel.inntekt.oppjuster
import no.nav.dagpenger.opplysning.regel.inntekt.sumAv
import no.nav.dagpenger.opplysning.regel.inntekt.summerPeriode
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.regel.størreEnn
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.Minsteinntekt.inntektFraSkatt
import no.nav.dagpenger.regel.OpplysningsTyper.AntallÅrI36MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.AvkortetInntektperiode1Id
import no.nav.dagpenger.regel.OpplysningsTyper.AvkortetInntektperiode2Id
import no.nav.dagpenger.regel.OpplysningsTyper.AvkortetInntektperiode3Id
import no.nav.dagpenger.regel.OpplysningsTyper.BruktBeregningsregelId
import no.nav.dagpenger.regel.OpplysningsTyper.FaktorForMaksimaltMuligGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.GjennomsnittligArbeidsinntektSiste36MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnbeløpForGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagSiste12MndId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagVedOrdinæreDagpengerId
import no.nav.dagpenger.regel.OpplysningsTyper.HarAvkortetGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.HarAvkortetGrunnlagetIPeriode1Id
import no.nav.dagpenger.regel.OpplysningsTyper.HarAvkortetGrunnlagetIPeriode2Id
import no.nav.dagpenger.regel.OpplysningsTyper.HarAvkortetGrunnlagetIPeriode3Id
import no.nav.dagpenger.regel.OpplysningsTyper.InntektSiste36MånederId
import no.nav.dagpenger.regel.OpplysningsTyper.Inntektperiode1Id
import no.nav.dagpenger.regel.OpplysningsTyper.Inntektperiode2Id
import no.nav.dagpenger.regel.OpplysningsTyper.Inntektperiode3Id
import no.nav.dagpenger.regel.OpplysningsTyper.OppjustertInntektId
import no.nav.dagpenger.regel.OpplysningsTyper.SeksGangerGrunnbeløpId
import no.nav.dagpenger.regel.OpplysningsTyper.TellendeInntektId
import no.nav.dagpenger.regel.OpplysningsTyper.UavkortetGrunnlagSiste12MndId
import no.nav.dagpenger.regel.OpplysningsTyper.UavkortetGrunnlagSiste36MndId
import no.nav.dagpenger.regel.OpplysningsTyper.UavrundetGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.UtbetaltArbeidsinntektPeriode1Id
import no.nav.dagpenger.regel.OpplysningsTyper.UtbetaltArbeidsinntektPeriode2Id
import no.nav.dagpenger.regel.OpplysningsTyper.UtbetaltArbeidsinntektPeriode3Id
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagHvisVerneplikt
import no.nav.dagpenger.regel.folketrygden
import no.nav.dagpenger.regel.kravPåDagpenger
import java.time.LocalDate
import java.time.Month

object Dagpengegrunnlag {
    private val oppjustertinntekt = Opplysningstype.inntekt(OppjustertInntektId, "Oppjustert inntekt", synlig = aldriSynlig)
    private val relevanteinntekter = Opplysningstype.inntekt(TellendeInntektId, "Tellende inntekt", synlig = aldriSynlig)

    val grunnbeløpForDagpengeGrunnlag = Opplysningstype.beløp(GrunnbeløpForGrunnlagId, "Grunnbeløp for grunnlag")
    private val faktorForMaksgrense =
        Opplysningstype.som(FaktorForMaksimaltMuligGrunnlagId, "Faktor for maksimalt mulig grunnlag", synlig = aldriSynlig)
    private val maksgrenseForGrunnlag = Opplysningstype.beløp(SeksGangerGrunnbeløpId, "6 ganger grunnbeløp", synlig = aldriSynlig)

    val antallÅrI36Måneder = Opplysningstype.desimaltall(AntallÅrI36MånederId, "Antall år i 36 måneder", synlig = aldriSynlig)

    internal val grunnlag12mnd = Opplysningstype.beløp(GrunnlagSiste12MndId, "Inntekt etter avkortning og oppjustering siste 12 måneder")
    private val beløpSiste36 = Opplysningstype.beløp(InntektSiste36MånederId, "Inntekt siste 36 måneder", synlig = aldriSynlig)
    internal val grunnlag36mnd =
        Opplysningstype.som(
            GjennomsnittligArbeidsinntektSiste36MånederId,
            "Gjennomsnittlig inntekt etter avkortning og oppjustering siste 36 måneder",
        )

    // Brutto
    private val utbetaltArbeidsinntektPeriode1 =
        Opplysningstype.beløp(
            UtbetaltArbeidsinntektPeriode1Id,
            "Utbetalt inntekt periode 1",
        )
    private val utbetaltArbeidsinntektPeriode2 =
        Opplysningstype.beløp(
            UtbetaltArbeidsinntektPeriode2Id,
            "Utbetalt inntekt periode 2",
        )
    private val utbetaltArbeidsinntektPeriode3 =
        Opplysningstype.beløp(
            UtbetaltArbeidsinntektPeriode3Id,
            "Utbetalt inntekt periode 3",
        )

    private val inntektperiode1 = Opplysningstype.beløp(Inntektperiode1Id, "Inntektperiode 1", synlig = aldriSynlig)
    private val inntektperiode2 = Opplysningstype.beløp(Inntektperiode2Id, "Inntektperiode 2", synlig = aldriSynlig)
    private val inntektperiode3 = Opplysningstype.beløp(Inntektperiode3Id, "Inntektperiode 3", synlig = aldriSynlig)

    private val avkortetperiode1 = Opplysningstype.beløp(AvkortetInntektperiode1Id, "Avkortet inntektperiode 1", synlig = aldriSynlig)
    private val avkortetperiode2 = Opplysningstype.beløp(AvkortetInntektperiode2Id, "Avkortet inntektperiode 2", synlig = aldriSynlig)
    private val avkortetperiode3 = Opplysningstype.beløp(AvkortetInntektperiode3Id, "Avkortet inntektperiode 3", synlig = aldriSynlig)

    val harAvkortetPeriode1 =
        Opplysningstype.boolsk(HarAvkortetGrunnlagetIPeriode1Id, "Har avkortet grunnlaget i periode 1", synlig = aldriSynlig)
    val harAvkortetPeriode2 =
        Opplysningstype.boolsk(HarAvkortetGrunnlagetIPeriode2Id, "Har avkortet grunnlaget i periode 2", synlig = aldriSynlig)
    val harAvkortetPeriode3 =
        Opplysningstype.boolsk(HarAvkortetGrunnlagetIPeriode3Id, "Har avkortet grunnlaget i periode 3", synlig = aldriSynlig)
    val harAvkortet = Opplysningstype.boolsk(HarAvkortetGrunnlagId, "Har avkortet grunnlag")

    internal val bruktBeregningsregel = Opplysningstype.tekst(BruktBeregningsregelId, "Brukt beregningsregel")
    val uavrundetGrunnlag = Opplysningstype.beløp(UavrundetGrunnlagId, "Uavrundet grunnlag", synlig = aldriSynlig)
    val dagpengegrunnlag = Opplysningstype.beløp(GrunnlagVedOrdinæreDagpengerId, "Grunnlag ved ordinære dagpenger")
    val grunnlag = Opplysningstype.beløp(GrunnlagId, "Dagpengegrunnlag")
    val uavkortet12mnd = Opplysningstype.beløp(UavkortetGrunnlagSiste12MndId, "Uavkortet grunnlag siste 12 mnd", Legacy, aldriSynlig)
    val uavkortet36mnd = Opplysningstype.beløp(UavkortetGrunnlagSiste36MndId, "Uavkortet grunnlag siste 36 mnd", Legacy, aldriSynlig)

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 11, "Dagpengegrunnlag", "Dagpengegrunnlag"),
        ) {
            skalVurderes {
                kravPåDagpenger(it) &&
                    // TODO: Må være ekte vurdering av om grunnlag skal fastsettes
                    !it.har(grunnlag)
            }

            regel(antallÅrI36Måneder) { somUtgangspunkt(3.0) }
            regel(faktorForMaksgrense) { oppslag(prøvingsdato) { 6.0 } }
            regel(maksgrenseForGrunnlag) { multiplikasjon(grunnbeløpForDagpengeGrunnlag, faktorForMaksgrense) }

            regel(grunnbeløpForDagpengeGrunnlag) { oppslag(prøvingsdato) { grunnbeløpFor(it) } }

            regel(relevanteinntekter) {
                filtrerRelevanteInntekter(
                    inntektFraSkatt,
                    listOf(
                        InntektKlasse.ARBEIDSINNTEKT,
                        InntektKlasse.DAGPENGER,
                        InntektKlasse.SYKEPENGER,
                        InntektKlasse.TILTAKSLØNN,
                        InntektKlasse.PLEIEPENGER,
                        InntektKlasse.OPPLÆRINGSPENGER,
                        InntektKlasse.OMSORGSPENGER,
                    ),
                )
            }
            regel(oppjustertinntekt) { oppjuster(grunnbeløpForDagpengeGrunnlag, relevanteinntekter) }

            // Summer hver 12 månedersperiode for utbetalt arbeidsinntekt
            regel(utbetaltArbeidsinntektPeriode1) { summerPeriode(relevanteinntekter, SummerPeriode.InntektPeriode.Første) }
            regel(utbetaltArbeidsinntektPeriode2) { summerPeriode(relevanteinntekter, SummerPeriode.InntektPeriode.Andre) }
            regel(utbetaltArbeidsinntektPeriode3) { summerPeriode(relevanteinntekter, SummerPeriode.InntektPeriode.Tredje) }

            // Summer hver 12 månedersperiode for oppjustert inntekt
            regel(inntektperiode1) { summerPeriode(oppjustertinntekt, SummerPeriode.InntektPeriode.Første) }
            regel(inntektperiode2) { summerPeriode(oppjustertinntekt, SummerPeriode.InntektPeriode.Andre) }
            regel(inntektperiode3) { summerPeriode(oppjustertinntekt, SummerPeriode.InntektPeriode.Tredje) }

            regel(uavkortet12mnd) { sumAv(inntektperiode1) }
            regel(uavkortet36mnd) { sumAv(inntektperiode1, inntektperiode2, inntektperiode3) }

            // Avkort hver 12 månedersperiode
            regel(avkortetperiode1) { minstAv(inntektperiode1, maksgrenseForGrunnlag) }
            regel(avkortetperiode2) { minstAv(inntektperiode2, maksgrenseForGrunnlag) }
            regel(avkortetperiode3) { minstAv(inntektperiode3, maksgrenseForGrunnlag) }

            // Fastsett grunnlag basert på siste 12 mnd
            regel(grunnlag12mnd) { minstAv(inntektperiode1, maksgrenseForGrunnlag) }

            // Summer siste 36 måneder
            regel(beløpSiste36) { sumAv(avkortetperiode1, avkortetperiode2, avkortetperiode3) }

            // Fastsett grunnlag basert på siste 36 mnd
            regel(grunnlag36mnd) { divisjon(beløpSiste36, antallÅrI36Måneder) }

            // Fastsett grunnlag til det som gir høyest grunnlag
            regel(uavrundetGrunnlag) { høyesteAv(grunnlag12mnd, grunnlag36mnd) }

            // Finn beregningsregel brukt
            regel(bruktBeregningsregel) { brukt(uavrundetGrunnlag) }

            // Fastsett avrundet grunnlag
            regel(dagpengegrunnlag) { avrund(uavrundetGrunnlag) }

            // Velg høyeste grunnlag av ordinært grunnlag og verneplikt
            regel(grunnlag) { høyesteAv(dagpengegrunnlag, grunnlagHvisVerneplikt) }

            // Fastsett om grunnlaget er avkortet
            regel(harAvkortetPeriode1) { størreEnn(inntektperiode1, maksgrenseForGrunnlag) }
            regel(harAvkortetPeriode2) { størreEnn(inntektperiode2, maksgrenseForGrunnlag) }
            regel(harAvkortetPeriode3) { størreEnn(inntektperiode3, maksgrenseForGrunnlag) }
            regel(harAvkortet) { enAv(harAvkortetPeriode1, harAvkortetPeriode2, harAvkortetPeriode3) }

            påvirkerResultat {
                if (it.erSann(grunnlagForVernepliktErGunstigst)) return@påvirkerResultat false
                it.erSann(kravTilAlder)
            }

            ønsketResultat(
                grunnlag,
                grunnbeløpForDagpengeGrunnlag,
                harAvkortet,
                bruktBeregningsregel,
                utbetaltArbeidsinntektPeriode1,
                utbetaltArbeidsinntektPeriode2,
                utbetaltArbeidsinntektPeriode3,
            )

            avklaring(Avklaringspunkter.GrunnbeløpForGrunnlagEndret)
        }
}

val NyttGrunnbeløpForGrunnlag =
    Kontrollpunkt(Avklaringspunkter.GrunnbeløpForGrunnlagEndret) {
        if (it.har(grunnbeløpForDagpengeGrunnlag) && it.har(prøvingsdato)) {
            val prøvingdato = it.finnOpplysning(prøvingsdato).verdi
            val sisteDagMedGammelG = LocalDate.of(LocalDate.now().year, Month.MAY, 1).minusDays(1)
            val policy = getGrunnbeløpForRegel(no.nav.dagpenger.grunnbelop.Regel.Grunnlag).forDato(prøvingdato)

            (prøvingdato.isAfter(sisteDagMedGammelG) && policy.iverksattFom.year != prøvingdato.year) ||
                (it.finnOpplysning(grunnbeløpForDagpengeGrunnlag).verdi.verdien != policy.verdi)
        } else {
            false
        }
    }

private fun grunnbeløpFor(it: LocalDate) =
    getGrunnbeløpForRegel(no.nav.dagpenger.grunnbelop.Regel.Grunnlag)
        .forDato(it)
        .verdi
        .let { Beløp(it) }

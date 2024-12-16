package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.grunnbelop.forDato
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
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
import no.nav.dagpenger.opplysning.regel.størreEnn
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Minsteinntekt.inntektFraSkatt
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagHvisVerneplikt
import java.time.LocalDate

object Dagpengegrunnlag {
    val uavrundetGrunnlag = Opplysningstype.somBeløp("Uavrundet grunnlag")
    val dagpengegrunnlag = Opplysningstype.somBeløp("Grunnlag ved ordinære dagpenger")
    val grunnlag = Opplysningstype.somBeløp("Grunnlag")
    val harAvkortet = Opplysningstype.somBoolsk("Har avkortet grunnlag")
    val uavkortet12mnd = Opplysningstype.somBeløp("Uavkortet grunnlag siste 12 mnd")
    val uavkortet36mnd = Opplysningstype.somBeløp("Uavkortet grunnlag siste 36 mnd")

    private val oppjustertinntekt = Opplysningstype.somInntekt("Oppjustert inntekt")
    private val relevanteinntekter = Opplysningstype.somInntekt("Tellende inntekt")
    val grunnbeløpForDagpengeGrunnlag = Opplysningstype.somBeløp("Grunnbeløp for grunnlag")

    private val faktorForMaksgrense = Opplysningstype.somDesimaltall("Faktor for maksimalt mulig grunnlag")
    private val maksgrenseForGrunnlag = Opplysningstype.somBeløp("6 ganger grunnbeløp")
    private val antallÅrI36Måneder = Opplysningstype.somDesimaltall("Antall år i 36 måneder")

    internal val grunnlag12mnd = Opplysningstype.somBeløp("Grunnlag siste 12 mnd.")

    private val beløpSiste36 = Opplysningstype.somBeløp("Inntekt siste 36 måneder")
    internal val grunnlag36mnd = Opplysningstype.somBeløp("Gjennomsnittlig arbeidsinntekt siste 36 måneder")

    private val utbetaltArbeidsinntektPeriode1 = Opplysningstype.somBeløp("Utbetalt arbeidsinntekt periode 1")
    private val utbetaltArbeidsinntektPeriode2 = Opplysningstype.somBeløp("Utbetalt arbeidsinntekt periode 2")
    private val utbetaltArbeidsinntektPeriode3 = Opplysningstype.somBeløp("Utbetalt arbeidsinntekt periode 3")

    private val inntektperiode1 = Opplysningstype.somBeløp("Inntektperiode 1")
    private val inntektperiode2 = Opplysningstype.somBeløp("Inntektperiode 2")
    private val inntektperiode3 = Opplysningstype.somBeløp("Inntektperiode 3")
    private val avkortetperiode1 = Opplysningstype.somBeløp("Avkortet inntektperiode 1")
    private val avkortetperiode2 = Opplysningstype.somBeløp("Avkortet inntektperiode 2")
    private val avkortetperiode3 = Opplysningstype.somBeløp("Avkortet inntektperiode 3")

    internal val bruktBeregningsregel = Opplysningstype.somTekst("Brukt beregningsregel")

    val regelsett =
        Regelsett("Dagpengegrunnlag") {
            regel(antallÅrI36Måneder) { oppslag(prøvingsdato) { 3.0 } }
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

            val harAvkortetPeriode1 = Opplysningstype.somBoolsk("Har avkortet grunnlaget i periode 1")
            val harAvkortetPeriode2 = Opplysningstype.somBoolsk("Har avkortet grunnlaget i periode 2")
            val harAvkortetPeriode3 = Opplysningstype.somBoolsk("Har avkortet grunnlaget i periode 3")

            // Fastsett om grunnlaget er avkortet
            regel(harAvkortetPeriode1) { størreEnn(inntektperiode1, maksgrenseForGrunnlag) }
            regel(harAvkortetPeriode2) { størreEnn(inntektperiode2, maksgrenseForGrunnlag) }
            regel(harAvkortetPeriode3) { størreEnn(inntektperiode3, maksgrenseForGrunnlag) }
            regel(harAvkortet) { enAv(harAvkortetPeriode1, harAvkortetPeriode2, harAvkortetPeriode3) }
        }
    val ønsketResultat =
        listOf(
            grunnbeløpForDagpengeGrunnlag,
            harAvkortet,
            bruktBeregningsregel,
            utbetaltArbeidsinntektPeriode1,
            utbetaltArbeidsinntektPeriode2,
            utbetaltArbeidsinntektPeriode3,
        )
}

private fun grunnbeløpFor(it: LocalDate) =
    getGrunnbeløpForRegel(no.nav.dagpenger.grunnbelop.Regel.Grunnlag)
        .forDato(it)
        .verdi
        .let { Beløp(it) }

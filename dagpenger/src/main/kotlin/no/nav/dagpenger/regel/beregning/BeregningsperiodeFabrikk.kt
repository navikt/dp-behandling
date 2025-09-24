package no.nav.dagpenger.regel.beregning

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.Beregning.forbruktEgenandel
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk.Dagstype.Helg
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk.Dagstype.Hverdag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid
import java.time.DayOfWeek
import java.time.LocalDate

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
class BeregningsperiodeFabrikk(
    meldeperiodeFraOgMed: LocalDate,
    meldeperiodeTilOgMed: LocalDate,
    private val opplysninger: LesbarOpplysninger,
) {
    private val meldeperiode = Periode(meldeperiodeFraOgMed, meldeperiodeTilOgMed)

    private val logger = KotlinLogging.logger { }

    fun lagBeregningsperiode(): Beregningsperiode {
        val gjenståendeEgenandel = hentGjenståendeEgenandel()
        val dager = hentMeldekortDagerMedRett()
        logger.info { "Meldekort dager med rett: ${dager.joinToString("\n") { it.toString() }}" }
        val periode = opprettPeriode(dager)
        val stønadsdagerIgjen =
            opplysninger.finnOpplysning(antallStønadsdager).verdi -
                opplysninger.somListe().filter { it.er(forbruk) && it.verdi as Boolean }.size

        logger.info {
            """
            Oppretter beregningsperiode med:
            - gjenståendeEgenandel = $gjenståendeEgenandel, 
            - stønadsdagerIgjen = $stønadsdagerIgjen, 
            - periode = ${periode.joinToString("|") { "(" + it.dato.toString() + ", " + it.dato.dagstype.toString() + ")" }}
            """.trimIndent()
        }
        return Beregningsperiode(gjenståendeEgenandel, periode, stønadsdagerIgjen)
    }

    private fun hentGjenståendeEgenandel(): Beløp {
        val harGjenstående = opplysninger.har(Beregning.gjenståendeEgenandel)
        if (harGjenstående) {
            return opplysninger.finnOpplysning(Beregning.gjenståendeEgenandel).verdi
        }

        // Fall tilbake på å regne ut gjenstående egenandel
        val innvilgetEgenandel = opplysninger.finnOpplysning(Egenandel.egenandel).verdi
        val forbruktEgenandel = opplysninger.finnAlle(forbruktEgenandel)
        val totalForbruktEgenandel = Beløp(forbruktEgenandel.sumOf { it.verdi })
        return innvilgetEgenandel - totalForbruktEgenandel
    }

    private fun hentMeldekortDagerMedRett(): List<LocalDate> {
        val perioderMedRett = opplysninger.finnAlle(harLøpendeRett).filter { it.verdi }.map { it.gyldighetsperiode }

        return meldeperiode.filter { meldekortDag -> perioderMedRett.any { it.inneholder(meldekortDag) } }
    }

    private fun opprettPeriode(dager: List<LocalDate>): Set<Dag> =
        dager
            .map { dato ->
                val gjeldendeOpplysninger = opplysninger.forDato(dato)
                when (dato.dagstype) {
                    Hverdag -> opprettArbeidsdagEllerFraværsdag(dato, gjeldendeOpplysninger)
                    Helg -> Helgedag(dato, Timer(gjeldendeOpplysninger.finnOpplysning(Beregning.arbeidstimer).verdi))
                }
            }.toSortedSet()

    private fun opprettArbeidsdagEllerFraværsdag(
        dato: LocalDate,
        opplysninger: LesbarOpplysninger,
    ): Dag {
        val erArbeidsdag = opplysninger.har(Beregning.arbeidsdag) && opplysninger.finnOpplysning(Beregning.arbeidsdag).verdi
        return if (erArbeidsdag) {
            Arbeidsdag(
                dato = dato,
                sats =
                    opplysninger
                        .finnOpplysning(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg)
                        .verdi,
                fva = Timer(opplysninger.finnOpplysning(Vanligarbeidstid.fastsattVanligArbeidstid).verdi / 5),
                timerArbeidet = Timer(opplysninger.finnOpplysning(Beregning.arbeidstimer).verdi),
                terskel = opplysninger.finnOpplysning(TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon).verdi,
            )
        } else {
            Fraværsdag(dato)
        }
    }

    private enum class Dagstype {
        Hverdag,
        Helg,
    }

    private val LocalDate.dagstype
        get() =
            when (dayOfWeek) {
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                -> Hverdag

                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
                -> Helg
            }
}

package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk.Dagstype.Helg
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk.Dagstype.Hverdag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid
import java.time.DayOfWeek
import java.time.LocalDate

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
internal class BeregningsperiodeFabrikk(
    private val meldeperiodeFraOgMed: LocalDate,
    private val meldeperiodeTilOgMed: LocalDate,
    private val opplysninger: LesbarOpplysninger,
) {
    fun lagBeregningsperiode(): Beregningsperiode {
        val gjenståendeEgenandel = hentGjenståendeEgenandel()
        val virkningsdato = hentVedtaksperiode()
        val dager = beregnDager(meldeperiodeFraOgMed, virkningsdato)
        val periode = opprettPeriode(dager)

        return Beregningsperiode(gjenståendeEgenandel, periode)
    }

    private fun hentGjenståendeEgenandel() =
        opplysninger
            .finnOpplysning(Egenandel.egenandel)
            .verdi.verdien
            .toDouble()

    // TODO: Finn en ekte virkningsdato
    private fun hentVedtaksperiode() = opplysninger.finnOpplysning(Dagpengeperiode.ordinærPeriode).gyldighetsperiode

    private fun beregnDager(
        meldeperiodeFraOgMed: LocalDate,
        vedtaksperiode: Gyldighetsperiode,
    ): List<LocalDate> {
        val sisteStart = maxOf(vedtaksperiode.fom, meldeperiodeFraOgMed)
        val førsteSlutt = minOf(vedtaksperiode.tom, meldeperiodeTilOgMed)
        return sisteStart.datesUntil(førsteSlutt).toList()
    }

    private fun opprettPeriode(dager: List<LocalDate>): List<Dag> =
        dager.map { dato ->
            val gjeldendeOpplysninger = opplysninger.forDato(dato)
            when (dato.dagstype) {
                Hverdag -> opprettArbeidsdagEllerFraværsdag(dato, gjeldendeOpplysninger)
                Helg -> Helgedag(dato, gjeldendeOpplysninger.finnOpplysning(Beregning.arbeidstimer).verdi)
            }
        }

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
                        .verdi.verdien
                        .toInt(),
                fva = opplysninger.finnOpplysning(Vanligarbeidstid.fastsattVanligArbeidstid).verdi / 5,
                timerArbeidet = opplysninger.finnOpplysning(Beregning.arbeidstimer).verdi,
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

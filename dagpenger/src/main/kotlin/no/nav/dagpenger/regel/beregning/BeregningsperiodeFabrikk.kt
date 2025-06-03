package no.nav.dagpenger.regel.beregning

import mu.KotlinLogging
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk.Dagstype.Helg
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk.Dagstype.Hverdag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid
import java.time.DayOfWeek
import java.time.LocalDate

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
internal class BeregningsperiodeFabrikk(
    private val meldeperiodeFraOgMed: LocalDate,
    private val meldeperiodeTilOgMed: LocalDate,
    opplysninger: LesbarOpplysninger,
    private val rettighetstatuser: TemporalCollection<Rettighetstatus>,
) {
    private val opplysninger: LesbarOpplysninger = opplysninger.utenErstattet
    private val meldeperiode = Periode(meldeperiodeFraOgMed, meldeperiodeTilOgMed)

    private val logger = KotlinLogging.logger { }

    fun lagBeregningsperiode(): Beregningsperiode {
        val gjenståendeEgenandel = hentGjenståendeEgenandel()
        val dager = hentMeldekortDagerMedRett()
        logger.info { "Rettighetstatuser for meldeperiode, $meldeperiode  : ${rettighetstatuser.contents()}" }
        logger.info { "Meldekort dager med rett: ${dager.joinToString("\n") { it.toString() }}" }
        val periode = opprettPeriode(dager)
        val stønadsdagerIgjen =
            opplysninger.finnOpplysning(antallStønadsdager).verdi -
                opplysninger
                    .finnAlle()
                    .filter { it.er(forbruk) && it.verdi as Boolean }
                    .size

        return Beregningsperiode(gjenståendeEgenandel, periode, stønadsdagerIgjen)
    }

    private fun hentGjenståendeEgenandel() =
        opplysninger
            .finnOpplysning(Egenandel.egenandel)
            .verdi.verdien
            .toDouble()

    private fun hentMeldekortDagerMedRett(): List<LocalDate> =
        meldeperiode.filter { meldekortDag -> runCatching { rettighetstatuser.get(meldekortDag).utfall }.getOrElse { false } }

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

package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.fastsetting.DagpengensStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import java.time.DayOfWeek
import java.time.LocalDate

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
internal class BeregningsperiodeFabrikk(
    private val meldeperiodeFraOgMed: LocalDate,
    private val meldeperiodeTilOgMed: LocalDate,
    private val opplysninger: Opplysninger,
) {
    fun lagBeregningsperiode(): Beregningsperiode {
        val gjenståendeEgenandel = hentGjenståendeEgenandel(opplysninger)
        val virkningsdato = hentVedtaksperiode(opplysninger)
        val dager = beregnDager(meldeperiodeFraOgMed, virkningsdato)
        val periode = opprettPeriode(dager, opplysninger)

        return Beregningsperiode(gjenståendeEgenandel, periode)
    }

    private fun hentGjenståendeEgenandel(opplysninger: Opplysninger) =
        opplysninger
            .finnOpplysning(Egenandel.egenandel)
            .verdi.verdien
            .toDouble()

    // TODO: Finn en ekte virkningsdato
    private fun hentVedtaksperiode(opplysninger: Opplysninger) =
        opplysninger.finnOpplysning(Dagpengeperiode.antallStønadsuker).gyldighetsperiode

    private fun beregnDager(
        meldeperiodeFraOgMed: LocalDate,
        vedtaksperiode: Gyldighetsperiode,
    ): List<LocalDate> {
        val sisteStart = maxOf(vedtaksperiode.fom, meldeperiodeFraOgMed)
        val førsteSlutt = minOf(vedtaksperiode.tom, meldeperiodeTilOgMed)
        return sisteStart.datesUntil(førsteSlutt).toList()
    }

    private fun opprettPeriode(
        dager: List<LocalDate>,
        opplysninger: Opplysninger,
    ): List<Dag> =
        dager.map { dato ->
            opplysninger.forDato = dato
            when (dato.dayOfWeek) {
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                -> opprettArbeidsdagEllerFraværsdag(dato, opplysninger)

                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
                -> Helgedag(dato, opplysninger.finnOpplysning(Beregning.arbeidstimer).verdi)
            }
        }

    private fun opprettArbeidsdagEllerFraværsdag(
        dato: LocalDate,
        opplysninger: Opplysninger,
    ): Dag {
        val erArbeidsdag = opplysninger.har(Beregning.arbeidsdag) && opplysninger.finnOpplysning(Beregning.arbeidsdag).verdi
        return if (erArbeidsdag) {
            Arbeidsdag(
                dato,
                opplysninger
                    .finnOpplysning(DagpengensStørrelse.sats)
                    .verdi.verdien
                    .toInt(),
                opplysninger.finnOpplysning(TapAvArbeidsinntektOgArbeidstid.fastsattVanligArbeidstid).verdi / 5,
                opplysninger.finnOpplysning(Beregning.arbeidstimer).verdi,
                opplysninger.finnOpplysning(Beregning.terskel).verdi.toBigDecimal(),
            )
        } else {
            Fraværsdag(dato)
        }
    }
}

package no.nav.dagpenger.regel.regelsett.beregning

import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.gjenståendeVed
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.meldtITide
import no.nav.dagpenger.regel.regelsett.beregning.BeregningsperiodeFabrikk.Dagstype.Helg
import no.nav.dagpenger.regel.regelsett.beregning.BeregningsperiodeFabrikk.Dagstype.Hverdag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.regelsett.fastsetting.Egenandel
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import java.time.DayOfWeek
import java.time.LocalDate

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
class BeregningsperiodeFabrikk(
    meldeperiodeFraOgMed: LocalDate,
    meldeperiodeTilOgMed: LocalDate,
    private val opplysninger: LesbarOpplysninger,
    private val kvoter: List<KvoteDefinisjon>,
) {
    private val meldeperiode = Periode(meldeperiodeFraOgMed, meldeperiodeTilOgMed)

    private val logger =
        io.github.oshai.kotlinlogging.KotlinLogging
            .logger { }

    fun lagBeregningsperiode(): Beregningsperiode {
        val dager = opprettPeriode(meldeperiode)
        val stønadsdagerIgjen =
            opplysninger.finnOpplysning(antallStønadsdager).verdi -
                opplysninger.somListe().filter { it.er(forbruk) && it.verdi as Boolean }.size
        val gjenståendeEgenandel = hentGjenståendeEgenandel(meldeperiode.fraOgMed)
        val bortfallsdagerIgjen = hentGjenståendeBortfall(meldeperiode.fraOgMed)

        logger.info {
            """
            Oppretter beregningsperiode med:
            - gjenståendeEgenandel = $gjenståendeEgenandel, 
            - stønadsdagerIgjen = $stønadsdagerIgjen, 
            - bortfallsdagerIgjen = $bortfallsdagerIgjen,
            - periode = ${dager.joinToString(
                "|",
            ) { "(" + it.dato.toString() + ", " + it.dato.dagstype.toString() + ", " + it.javaClass.simpleName + ")" }}
            """.trimIndent()
        }
        return Beregningsperiode(gjenståendeEgenandel, dager, stønadsdagerIgjen, bortfallsdagerIgjen)
    }

    private fun hentGjenståendeEgenandel(førsteDag: LocalDate): Beløp {
        val innvilgetEgenandel = opplysninger.finnOpplysning(Egenandel.egenandel).verdi

        // Finn siste registrerte gjenstående egenandel før denne meldeperioden, ellers bruk innvilget egenandel
        return opplysninger
            .finnAlle(Beregning.gjenståendeEgenandel)
            .lastOrNull { it.gyldighetsperiode.tilOgMed.isBefore(førsteDag) }
            ?.verdi ?: innvilgetEgenandel
    }

    private fun hentGjenståendeBortfall(førsteDag: LocalDate): Int =
        kvoter
            .filter { it.forbrukKriterium == Beregning.erBortfallsdag }
            .sumOf { kvote -> kvote.gjenståendeVed(opplysninger, førsteDag) }

    private fun hentMeldekortDagerMedRett(): List<LocalDate> {
        val perioderMedRett = opplysninger.finnAlle(harLøpendeRett).filter { it.verdi }.map { it.gyldighetsperiode }
        val meldtITide = opplysninger.finnOpplysning(meldtITide, meldeperiode.fraOgMed).verdi
        val dagerMedRett =
            meldeperiode
                .filter { meldekortDag -> perioderMedRett.any { it.inneholder(meldekortDag) } }

        return if (meldtITide) {
            dagerMedRett
        } else {
            dagerMedRett.filter { meldekortDag -> opplysninger.erSann(Beregning.meldt, meldekortDag) }
        }
    }

    private fun opprettPeriode(meldeperiode: Periode): Set<Dag> {
        val dagerMedRett = hentMeldekortDagerMedRett()
        return meldeperiode
            .map { dato ->
                if (dato !in dagerMedRett) {
                    return@map IkkeRettighetDag(dato)
                }
                val gjeldendeOpplysninger = opplysninger.forDato(dato)

                when (dato.dagstype) {
                    Hverdag -> {
                        opprettArbeidsdagEllerFraværsdag(dato, gjeldendeOpplysninger)
                    }

                    Helg -> {
                        Helgedag(
                            dato = dato,
                            timerArbeidet = Timer(gjeldendeOpplysninger.finnOpplysning(Beregning.arbeidstimer).verdi),
                        )
                    }
                }
            }.toSortedSet()
    }

    private fun opprettArbeidsdagEllerFraværsdag(
        dato: LocalDate,
        opplysninger: LesbarOpplysninger,
    ): Dag {
        val erArbeidsdag = opplysninger.har(Beregning.arbeidsdag) && opplysninger.finnOpplysning(Beregning.arbeidsdag).verdi
        return if (erArbeidsdag) {
            Arbeidsdag(
                dato = dato,
                sats = opplysninger.finnOpplysning(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg).verdi,
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

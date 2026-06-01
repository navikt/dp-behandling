package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.Egenandel
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MeldekortBeregningPluginTest {
    @Test
    fun `skriver beregningsresultat og daglige opplysninger for bortfall`() {
        val fraOgMed = LocalDate.of(2018, 6, 18)
        val tilOgMed = LocalDate.of(2018, 7, 1)
        val meldeperiode = Periode(fraOgMed, tilOgMed)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(Beregning.meldeperiode, meldeperiode))
                leggTil(Faktum(Beregning.meldtITide, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                leggTil(Faktum(KravPåDagpenger.harLøpendeRett, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                leggTil(Faktum(Dagpengeperiode.antallStønadsdager, 52, Gyldighetsperiode(fraOgMed)))
                leggTil(Faktum(Egenandel.egenandel, Beløp(0), Gyldighetsperiode(fraOgMed)))
                leggTil(Faktum(TidsbegrensetBortfall.harTidsbegrensetBortfall, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                leggTil(Faktum(TidsbegrensetBortfall.antallBortfallsdager, 2, Gyldighetsperiode(fraOgMed)))
                leggTil(Faktum(Beregning.arbeidstimer, 0.0, Gyldighetsperiode(fraOgMed, tilOgMed)))
                leggTil(
                    Faktum(
                        DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg,
                        Beløp(100),
                        Gyldighetsperiode(fraOgMed, tilOgMed),
                    ),
                )
                leggTil(Faktum(Vanligarbeidstid.fastsattVanligArbeidstid, 37.5, Gyldighetsperiode(fraOgMed, tilOgMed)))
                leggTil(
                    Faktum(
                        TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon,
                        50.0,
                        Gyldighetsperiode(fraOgMed, tilOgMed),
                    ),
                )

                var dato = fraOgMed
                while (!dato.isAfter(tilOgMed)) {
                    if (dato.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                        leggTil(Faktum(Beregning.arbeidsdag, true, Gyldighetsperiode(dato, dato)))
                    }
                    dato = dato.plusDays(1)
                }
            }

        val resultat = MeldekortBeregningPlugin(RegelverkDagpenger.kvoter()).beregnForPeriode(Prosesskontekst(opplysninger), meldeperiode)

        resultat.forbruksdager shouldBe resultat.forbruksdager.sortedBy { it.dag.dato }
        resultat.forbruksdager.size shouldBe 10
        resultat.forbruksdager.count { it.erBortfall } shouldBe 2
        resultat.utbetaling shouldBe Beløp(800)
        resultat.forbruktEgenandel shouldBe Beløp(0)
        resultat.gjenståendeEgenandel shouldBe Beløp(0)
        resultat.sumFva.timer shouldBe 75.0
        resultat.sumArbeidstimer.timer shouldBe 0.0
        resultat.prosentfaktor shouldBe 1.0

        opplysninger.finnAlle(Beregning.forbruk).map { it.verdi }.shouldBeSizeAndValues(trueCount = 10, falseCount = 4)
        opplysninger.finnAlle(Beregning.erBortfallsdag).map { it.verdi }.shouldBeSizeAndValues(trueCount = 2, falseCount = 12)
        opplysninger.finnAlle(Beregning.utbetaling).map { it.verdi.verdien.toInt() }.sum() shouldBe 800
        opplysninger.finnAlle(Beregning.utbetalingForPeriode).last().verdi shouldBe Beløp(800)
        opplysninger.finnAlle(Beregning.sumFva).last().verdi shouldBe 75.0
        opplysninger.finnAlle(Beregning.sumArbeidstimer).last().verdi shouldBe 0.0
        opplysninger.finnAlle(Beregning.prosentfaktor).last().verdi shouldBe 1.0
        opplysninger.finnAlle(Beregning.gjenståendeEgenandel).last().verdi shouldBe Beløp(0)
    }

    private fun List<Boolean>.shouldBeSizeAndValues(
        trueCount: Int,
        falseCount: Int,
    ) {
        size shouldBe trueCount + falseCount
        count { it == true } shouldBe trueCount
        count { it == false } shouldBe falseCount
    }
}

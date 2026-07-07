package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forbrukstype
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
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import org.junit.jupiter.api.Nested
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
                leggTil(Faktum(Sanksjonsperiode.harSanksjon, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                leggTil(Faktum(Sanksjonsperiode.antallSanksjonsuker, 0, Gyldighetsperiode(fraOgMed)))
                leggTil(Faktum(Sanksjonsperiode.antallSanksjonsdager, 0, Gyldighetsperiode(fraOgMed)))
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

        resultat.beregningsdager shouldBe resultat.beregningsdager.sortedBy { it.dag.dato }
        resultat.beregningsdager.size shouldBe 14
        resultat.beregningsdager.count { it.avviklerSanksjon } shouldBe 2
        resultat.beregningsdager.filter { it.avviklerSanksjon }.map { it.tilUtbetaling } shouldBe listOf(Beløp(0), Beløp(0))
        resultat.utbetaling shouldBe Beløp(800)
        resultat.forbruktEgenandel shouldBe Beløp(0)
        resultat.gjenståendeEgenandel shouldBe Beløp(0)
        resultat.sumFva.timer shouldBe 75.0
        resultat.sumArbeidstimer.timer shouldBe 0.0
        resultat.prosentfaktor shouldBe 1.0

        opplysninger.finnAlle(Beregning.forbruk).map { it.verdi }.shouldBeSizeAndValues(trueCount = 10, falseCount = 4)
        opplysninger.finnAlle(Beregning.erSanksjonsdag).map { it.verdi }.shouldBeSizeAndValues(trueCount = 2, falseCount = 12)
        opplysninger.finnAlle(Beregning.utbetaling).map { it.verdi.verdien.toInt() }.sum() shouldBe 800
        opplysninger.finnAlle(Beregning.utbetalingForPeriode).last().verdi shouldBe Beløp(800)
        opplysninger.finnAlle(Beregning.sumFva).last().verdi shouldBe 75.0
        opplysninger.finnAlle(Beregning.sumArbeidstimer).last().verdi shouldBe 0.0
        opplysninger.finnAlle(Beregning.prosentfaktor).last().verdi shouldBe 1.0
        opplysninger.finnAlle(Beregning.gjenståendeEgenandel).last().verdi shouldBe Beløp(0)
    }

    @Test
    fun `sanksjonskvoter styres av ilagt dato og ikke typeprioritet`() {
        val sanksjonskvoter =
            RegelverkDagpenger
                .kvoter()
                .filter { it.teller(Forbrukstype.Sanksjon) }

        sanksjonskvoter.map { it.navn } shouldBe listOf("Sanksjonsperiode", "Tidsbegrenset bortfall")
        sanksjonskvoter.map { it.forbrukstype }.toSet() shouldBe setOf(Forbrukstype.Sanksjon)
    }

    @Nested
    inner class AllokeringSanksjonerTest {
        /**
         * Verifiserer sekvensiell allokering uten deling av dager mellom kvoter.
         */
        @Test
        fun `én bortfallsdag allokeres til kun én sanksjonskvote og oversettes til faktafelter`() {
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

                    // §4-10: 3 sanksjondager
                    leggTil(Faktum(Sanksjonsperiode.harSanksjon, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                    leggTil(Faktum(Sanksjonsperiode.antallSanksjonsuker, 0, Gyldighetsperiode(fraOgMed)))
                    leggTil(Faktum(Sanksjonsperiode.antallSanksjonsdager, 3, Gyldighetsperiode(fraOgMed)))

                    // §4-20: 2 bortfallsdager
                    leggTil(Faktum(TidsbegrensetBortfall.harTidsbegrensetBortfall, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                    leggTil(Faktum(TidsbegrensetBortfall.antallBortfallsuker, 0, Gyldighetsperiode(fraOgMed)))
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

            val resultat =
                MeldekortBeregningPlugin(
                    RegelverkDagpenger.kvoter(),
                ).beregnForPeriode(Prosesskontekst(opplysninger), meldeperiode)

            // Totalt 5 bortfallsdager (3 fra sanksjon + 2 fra bortfall)
            resultat.beregningsdager.count { it.avviklerSanksjon } shouldBe 5

            val forbrukteSanksjonsdager =
                opplysninger
                    .finnAlle(Beregning.forbruktSanksjonsdager)
                    .sortedBy { it.gyldighetsperiode.fraOgMed }
            val sanksjonDatoer =
                listOf(1, 2, 3).map { forbruk ->
                    forbrukteSanksjonsdager.first { it.verdi == forbruk }.gyldighetsperiode.fraOgMed
                }
            sanksjonDatoer shouldBe
                listOf(
                    fraOgMed,
                    fraOgMed.plusDays(1),
                    fraOgMed.plusDays(2),
                )

            val forbrukteBortfallsdager =
                opplysninger
                    .finnAlle(Beregning.forbruktBortfallsdager)
                    .sortedBy { it.gyldighetsperiode.fraOgMed }
            val bortfallDatoer =
                listOf(1, 2).map { forbruk ->
                    forbrukteBortfallsdager.first { it.verdi == forbruk }.gyldighetsperiode.fraOgMed
                }
            bortfallDatoer shouldBe
                listOf(
                    fraOgMed.plusDays(3),
                    fraOgMed.plusDays(4),
                )

            // Regel 1: samme dag kan ikke allokeres til begge kvoter
            sanksjonDatoer.toSet().intersect(bortfallDatoer.toSet()) shouldBe emptySet()

            // Regel 4: intern allokering oversettes til forbrukt/gjenstående/siste-verdier
            opplysninger.finnAlle(Beregning.gjenståendeSanksjonsdager).last().verdi shouldBe 0
            opplysninger.finnAlle(Beregning.gjenståendeBortfallsdager).last().verdi shouldBe 0
        }

        @Test
        fun `rekkefølge følger ilagt dato fra kapasitet`() {
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

                    // §4-10 ilagt senere
                    leggTil(Faktum(Sanksjonsperiode.harSanksjon, true, Gyldighetsperiode(fraOgMed.plusDays(2), tilOgMed)))
                    leggTil(Faktum(Sanksjonsperiode.antallSanksjonsuker, 0, Gyldighetsperiode(fraOgMed)))
                    leggTil(Faktum(Sanksjonsperiode.antallSanksjonsdager, 2, Gyldighetsperiode(fraOgMed.plusDays(2))))

                    // §4-20 ilagt først
                    leggTil(Faktum(TidsbegrensetBortfall.harTidsbegrensetBortfall, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                    leggTil(Faktum(TidsbegrensetBortfall.antallBortfallsuker, 0, Gyldighetsperiode(fraOgMed)))
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

            MeldekortBeregningPlugin(
                RegelverkDagpenger.kvoter(),
            ).beregnForPeriode(Prosesskontekst(opplysninger), meldeperiode)

            val forbrukteBortfallsdager =
                opplysninger
                    .finnAlle(Beregning.forbruktBortfallsdager)
                    .sortedBy { it.gyldighetsperiode.fraOgMed }
            val forbrukteSanksjonsdager =
                opplysninger
                    .finnAlle(Beregning.forbruktSanksjonsdager)
                    .sortedBy { it.gyldighetsperiode.fraOgMed }

            listOf(1, 2).map { forbruk ->
                forbrukteBortfallsdager.first { it.verdi == forbruk }.gyldighetsperiode.fraOgMed
            } shouldBe
                listOf(
                    fraOgMed,
                    fraOgMed.plusDays(1),
                )
            listOf(1, 2).map { forbruk ->
                forbrukteSanksjonsdager.first { it.verdi == forbruk }.gyldighetsperiode.fraOgMed
            } shouldBe
                listOf(
                    fraOgMed.plusDays(2),
                    fraOgMed.plusDays(3),
                )
        }

        @Test
        fun `bevarer historisk gjenstående for senere sanksjonskvote når den ikke får dager i perioden`() {
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

                    // §4-10 tar alle bortfallsdager i denne meldeperioden
                    leggTil(Faktum(Sanksjonsperiode.harSanksjon, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                    leggTil(Faktum(Sanksjonsperiode.antallSanksjonsuker, 1, Gyldighetsperiode(fraOgMed)))
                    leggTil(Faktum(Sanksjonsperiode.antallSanksjonsdager, 10, Gyldighetsperiode(fraOgMed)))

                    // §4-20 har historisk gjenstående 2 fra tidligere periode
                    leggTil(Faktum(TidsbegrensetBortfall.harTidsbegrensetBortfall, true, Gyldighetsperiode(fraOgMed, tilOgMed)))
                    leggTil(Faktum(TidsbegrensetBortfall.antallBortfallsuker, 0, Gyldighetsperiode(fraOgMed)))
                    leggTil(Faktum(TidsbegrensetBortfall.antallBortfallsdager, 5, Gyldighetsperiode(fraOgMed)))
                    leggTil(
                        Faktum(
                            Beregning.gjenståendeBortfallsdager,
                            2,
                            Gyldighetsperiode(fraOgMed.minusDays(1), fraOgMed.minusDays(1)),
                        ),
                    )

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

            MeldekortBeregningPlugin(RegelverkDagpenger.kvoter()).beregnForPeriode(Prosesskontekst(opplysninger), meldeperiode)

            val gjenståendeBortfall =
                opplysninger
                    .finnAlle(Beregning.gjenståendeBortfallsdager)
            gjenståendeBortfall.last().verdi shouldBe 5
            gjenståendeBortfall.count { it.gyldighetsperiode.overlapper(Gyldighetsperiode(fraOgMed, tilOgMed)) } shouldBe 14
        }
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

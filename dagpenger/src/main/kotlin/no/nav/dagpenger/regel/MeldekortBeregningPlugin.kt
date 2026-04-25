package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.Beregning.utbetaling
import no.nav.dagpenger.regel.beregning.Beregningresultat
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.beregning.TerskelTrekkForSenMelding

class MeldekortBeregningPlugin : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        val opplysninger = kontekst.opplysninger
        val meldeperiode = meldeperiode(opplysninger)
        beregnForPeriode(kontekst, meldeperiode)
    }

    fun beregnForPeriode(
        kontekst: Prosesskontekst,
        meldeperiode: Periode,
    ): Beregningresultat {
        val opplysninger = kontekst.opplysninger
        val gyldighetsperiode = Gyldighetsperiode(meldeperiode.fraOgMed, meldeperiode.tilOgMed)

        val terskelForAntallDagerEnIkkeKanVæreMeldt = TerskelTrekkForSenMelding.forDato(meldeperiode.fraOgMed)
        val antallIkkeMeldtDager =
            opplysninger
                .finnAlle(Beregning.meldt)
                .filter { it.gyldighetsperiode.overlapper(gyldighetsperiode) }
                .filterNot { it.verdi }
                .size
        val erMeldtITide = antallIkkeMeldtDager < terskelForAntallDagerEnIkkeKanVæreMeldt

        opplysninger.leggTil(Faktum(Beregning.meldtITide, erMeldtITide, gyldighetsperiode))

        val resultat =
            BeregningsperiodeFabrikk(meldeperiode.fraOgMed, meldeperiode.tilOgMed, opplysninger)
                .lagBeregningsperiode()
                .resultat

        opplysninger.leggTil(Faktum(Beregning.forbruktEgenandel, resultat.forbruktEgenandel, gyldighetsperiode))
        opplysninger.leggTil(Faktum(Beregning.utbetalingForPeriode, resultat.utbetaling, gyldighetsperiode))
        opplysninger.leggTil(Faktum(Beregning.gjenståendeEgenandel, resultat.gjenståendeEgenandel, gyldighetsperiode))
        opplysninger.leggTil(
            Faktum(Beregning.oppfyllerKravTilTaptArbeidstidIPerioden, resultat.oppfyllerKravTilTaptArbeidstid, gyldighetsperiode),
        )
        opplysninger.leggTil(Faktum(Beregning.sumFva, resultat.sumFva.timer, gyldighetsperiode))
        opplysninger.leggTil(Faktum(Beregning.sumArbeidstimer, resultat.sumArbeidstimer.timer, gyldighetsperiode))
        opplysninger.leggTil(Faktum(Beregning.prosentfaktor, resultat.prosentfaktor, gyldighetsperiode))

        val forbruksdager = resultat.forbruksdager
        meldeperiode
            .forEach { dato ->
                val forbruksdag = forbruksdager.singleOrNull { it.dag.dato.isEqual(dato) }
                val dagGyldighetsperiode = Gyldighetsperiode(dato, dato)

                opplysninger.leggTil(Faktum(forbruk, forbruksdag != null, dagGyldighetsperiode))
                opplysninger.leggTil(Faktum(utbetaling, forbruksdag?.tilUtbetaling ?: Beløp(0), dagGyldighetsperiode))
            }

        return resultat
    }

    private fun meldeperiode(opplysninger: LesbarOpplysninger): Periode = opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).verdi
}

package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.KvotetellingsSkriver
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.erBortfallsdag
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.oppfyllerKravTilTaptArbeidstidIPerioden
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.utbetaling
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.utbetalingForPeriode
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat.Beregningsdag.Forbruksdag
import no.nav.dagpenger.regel.regelsett.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.regelsett.beregning.TerskelTrekkForSenMelding

class MeldekortBeregningPlugin(
    private val kvoter: List<KvoteDefinisjon>,
) : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        val opplysninger = kontekst.opplysninger
        val meldeperiode = meldeperiode(opplysninger)
        beregnForPeriode(kontekst, meldeperiode)
    }

    fun beregnForPeriode(
        kontekst: Prosesskontekst,
        meldeperiode: Periode,
    ): Beregningresultat {
        kontekst.kontekst(this)
        val opplysninger = kontekst.opplysninger
        val gyldighetsperiode = Gyldighetsperiode(meldeperiode.fraOgMed, meldeperiode.tilOgMed)
        kontekst.info("Beregner meldeperiode: ${gyldighetsperiode.fraOgMed} til ${gyldighetsperiode.tilOgMed}")

        opplysninger.fastsettMeldtITide(meldeperiode, gyldighetsperiode)

        val resultat =
            BeregningsperiodeFabrikk(meldeperiode.fraOgMed, meldeperiode.tilOgMed, opplysninger, kvoter)
                .lagBeregningsperiode()
                .resultat

        opplysninger.lagreEgenandel(resultat, gyldighetsperiode)
        opplysninger.lagreBeregningsverdier(resultat, gyldighetsperiode)

        val forbruksdager = resultat.beregningsdager
        forbruksdager
            .forEach { dag ->
                val dagGyldighetsperiode = dag.gyldighetsperiode
                opplysninger.leggTil(Faktum(forbruk, dag is Forbruksdag, dagGyldighetsperiode))
                opplysninger.leggTil(Faktum(utbetaling, dag.tilUtbetaling, dagGyldighetsperiode))
                opplysninger.leggTil(Faktum(erBortfallsdag, dag?.erBortfall ?: false, dagGyldighetsperiode))
            }

        val kategoriserteDager = resultat.forbruksdager.kategoriser()
        KvotetellerOrkestrering(kvoter)
            .beregn(kategoriserteDager, opplysninger, meldeperiode.fraOgMed)
            .forEach { (kvote, kvoteresultat) -> KvotetellingsSkriver(kvote).skriv(opplysninger, kvoteresultat) }
        return resultat
    }

    private fun Opplysninger.lagreEgenandel(
        resultat: Beregningresultat,
        gyldighetsperiode: Gyldighetsperiode,
    ) {
        leggTil(Faktum(Beregning.forbruktEgenandel, resultat.forbruktEgenandel, gyldighetsperiode))
        leggTil(Faktum(Beregning.gjenståendeEgenandel, resultat.gjenståendeEgenandel, gyldighetsperiode))
    }

    private fun Opplysninger.lagreBeregningsverdier(
        resultat: Beregningresultat,
        gyldighetsperiode: Gyldighetsperiode,
    ) {
        leggTil(Faktum(Beregning.sumFva, resultat.sumFva.timer, gyldighetsperiode))
        leggTil(Faktum(Beregning.sumArbeidstimer, resultat.sumArbeidstimer.timer, gyldighetsperiode))
        leggTil(Faktum(Beregning.prosentfaktor, resultat.prosentfaktor, gyldighetsperiode))
    }

    private fun Opplysninger.fastsettMeldtITide(
        meldeperiode: Periode,
        gyldighetsperiode: Gyldighetsperiode,
    ) {
        val terskelForAntallDagerEnIkkeKanVæreMeldt = TerskelTrekkForSenMelding.forDato(meldeperiode.fraOgMed)
        val antallIkkeMeldtDager =
            finnAlle(Beregning.meldt)
                .filter { it.gyldighetsperiode.overlapper(gyldighetsperiode) }
                .filterNot { it.verdi }
                .size
        val erMeldtITide = antallIkkeMeldtDager < terskelForAntallDagerEnIkkeKanVæreMeldt

        leggTil(Faktum(Beregning.meldtITide, erMeldtITide, gyldighetsperiode))
    }

    private fun meldeperiode(opplysninger: Opplysninger): Periode = opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).verdi

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst(
            "MeldekortBeregningPlugin",
        )
}

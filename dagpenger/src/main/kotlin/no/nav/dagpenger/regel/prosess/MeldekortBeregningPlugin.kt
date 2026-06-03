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
import no.nav.dagpenger.regel.Kvoteteller
import no.nav.dagpenger.regel.Kvotetellingsresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.erBortfallsdag
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.utbetaling
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat.Beregningsdag.Forbruksdag
import no.nav.dagpenger.regel.regelsett.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.regelsett.beregning.TerskelTrekkForSenMelding
import java.time.LocalDate

class MeldekortBeregningPlugin(
    private val kvoter: List<KvoteDefinisjon>,
) : ProsessPlugin {
    private val kvoteLagring = KvoteLagring()

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
            BeregningsperiodeFabrikk(meldeperiode.fraOgMed, meldeperiode.tilOgMed, opplysninger, kvoter)
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

        val forbruksdager = resultat.beregningsdager
        forbruksdager
            .forEach { dag ->
                val dagGyldighetsperiode = dag.gyldighetsperiode
                opplysninger.leggTil(Faktum(forbruk, dag is Forbruksdag, dagGyldighetsperiode))
                opplysninger.leggTil(Faktum(utbetaling, dag.tilUtbetaling, dagGyldighetsperiode))
                opplysninger.leggTil(Faktum(erBortfallsdag, dag?.erBortfall ?: false, dagGyldighetsperiode))
            }

        kvoteLagring.lagre(opplysninger, beregnKvotetellinger(kvoter, opplysninger, meldeperiode, forbruksdager))
        return resultat
    }

    private fun meldeperiode(opplysninger: Opplysninger): Periode = opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).verdi

    private fun beregnKvotetellinger(
        kvoter: List<KvoteDefinisjon>,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
        forbruksdager: List<Beregningresultat.Forbruksdag>,
    ): List<KvoteTelling> = kvoter.map { kvote -> KvoteTelling(kvote, kvote.beregn(opplysninger, meldeperiode, forbruksdager)) }

    private fun List<Beregningresultat.Forbruksdag>.medTildeltKvote(
        kvoter: List<KvoteDefinisjon>,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
    ): List<Beregningresultat.Forbruksdag> {
        val allokeringskjede = kvoter.allokeringskjede(opplysninger)
        if (allokeringskjede.isEmpty()) return this.sortedBy { it.dag.dato }

        val forbruksdagerPerType =
            sortedBy { it.dag.dato }
                .groupBy { it.forbrukstype }
                .mapValues { ArrayDeque(it.value) }

        val kvotePerDato = mutableMapOf<LocalDate, KvoteDefinisjon>()
        allokeringskjede.forEach { kvote ->
            val kapasitet = kvote.gjenståendeVed(opplysninger, meldeperiode.fraOgMed)
            val allokerbareDager = forbruksdagerPerType[kvote.forbrukstype].orEmptyDeque()
            repeat(minOf(kapasitet, allokerbareDager.size)) {
                kvotePerDato[allokerbareDager.removeFirst().dag.dato] = kvote
            }
        }

        return sortedBy { it.dag.dato }.map { forbruksdag ->
            val kvote = kvotePerDato[forbruksdag.dag.dato]
            if (kvote == null) forbruksdag else forbruksdag.copy(kvote = kvote)
        }
    }

    private fun KvoteDefinisjon.beregn(
        opplysninger: Opplysninger,
        meldeperiode: Periode,
        forbruksdager: List<Beregningresultat.Forbruksdag>,
    ): Kvotetellingsresultat =
        if (erEtterfølgendeForbruk()) {
            beregnMedTildelteDager(opplysninger, meldeperiode, forbruksdager)
        } else {
            Kvoteteller(this).beregn(opplysninger)
        }

    private fun KvoteDefinisjon.beregnMedTildelteDager(
        opplysninger: Opplysninger,
        meldeperiode: Periode,
        forbruksdager: List<Beregningresultat.Forbruksdag>,
    ): Kvotetellingsresultat {
        val kvotedager = forbruksdager.filter { it.kvote == this }.map { it.dag.dato }.toSet()
        if (kvotedager.isEmpty()) return Kvotetellingsresultat()

        val opplysningerForKvote =
            Opplysninger.basertPå(opplysninger).apply {
                meldeperiode.forEach { dato ->
                    leggTil(
                        Faktum(
                            Beregning.erBortfallsdag,
                            dato in kvotedager,
                            Gyldighetsperiode(dato, dato),
                        ),
                    )
                }
            }

        return Kvoteteller(this).beregn(opplysningerForKvote)
    }

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst(
            "MeldekortBeregningPlugin",
        )
}

private fun <T> ArrayDeque<T>?.orEmptyDeque(): ArrayDeque<T> = this ?: ArrayDeque()

package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.tilTimer
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.TerskelTrekkForSenMelding

fun Meldekort.tilOpplysninger(kilde: Kilde): List<Opplysning<*>> {
    val opplysninger = mutableListOf<Opplysning<*>>()
    opplysninger.addAll(dager.tilOpplysninger(kilde))
    opplysninger.add(Faktum(Beregning.meldedato, meldedato, Gyldighetsperiode(this.fom, this.tom), kilde = kilde))
    val terskelForAntallDagerEnIkkeKanVæreMeldt = TerskelTrekkForSenMelding.forDato(this.fom)
    val antallIkkeMeldtDager = opplysninger.filter { it.opplysningstype == Beregning.meldt }.count { !(it.verdi as Boolean) }
    opplysninger.add(
        Faktum(
            Beregning.meldtITide,
            (antallIkkeMeldtDager < terskelForAntallDagerEnIkkeKanVæreMeldt),
            Gyldighetsperiode(this.fom, this.tom),
            kilde = kilde,
        ),
    )
    return opplysninger.toList()
}

private fun List<Dag>.tilOpplysninger(kilde: Kilde): List<Opplysning<*>> {
    val opplysninger = mutableListOf<Opplysning<*>>()
    this.forEach { dag ->
        val gyldighetsperiode = Gyldighetsperiode(dag.dato, dag.dato)

        val timer = dag.aktiviteter.map { it.timer?.tilTimer ?: Timer(0) }.summer()
        val fraværEllerSyk = dag.aktiviteter.any { it.type == AktivitetType.Fravær || it.type == AktivitetType.Syk }
        val arbeidEllerUtdanning = dag.aktiviteter.any { it.type == AktivitetType.Utdanning || it.type == AktivitetType.Arbeid }

        when {
            fraværEllerSyk -> {
                opplysninger.add(Faktum(Beregning.arbeidsdag, false, gyldighetsperiode, kilde = kilde))
                opplysninger.add(Faktum(Beregning.arbeidstimer, 0.0, gyldighetsperiode, kilde = kilde))
            }

            arbeidEllerUtdanning -> {
                opplysninger.add(Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde))
                opplysninger.add(Faktum(Beregning.arbeidstimer, timer.timer, gyldighetsperiode, kilde = kilde))
            }

            else -> {
                opplysninger.add(Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde))
                opplysninger.add(Faktum(Beregning.arbeidstimer, 0.0, gyldighetsperiode, kilde = kilde))
            }
        }

        opplysninger.add(Faktum(Beregning.meldt, dag.meldt, gyldighetsperiode, kilde = kilde))
    }

    return opplysninger.toList()
}

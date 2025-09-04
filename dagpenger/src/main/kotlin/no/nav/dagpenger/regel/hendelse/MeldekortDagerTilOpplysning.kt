package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.tilTimer
import no.nav.dagpenger.regel.beregning.Beregning

fun List<Dag>.tilOpplysninger(kilde: Kilde): List<Opplysning<*>> {
    val opplysninger = mutableListOf<Opplysning<*>>()
    this.forEach { dag ->
        val gyldighetsperiode = Gyldighetsperiode(dag.dato, dag.dato)

        val timer = dag.aktiviteter.map { it.timer?.tilTimer ?: Timer(0) }.summer()
        // TODO: Hva om det er flere aktiviteter? Utdanning og arbeid kan kombineres
        val type = dag.aktiviteter.firstOrNull()?.type
        when (type) {
            AktivitetType.Utdanning,
            AktivitetType.Arbeid,
            -> {
                listOf(
                    opplysninger.add(Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde)),
                    opplysninger.add(Faktum(Beregning.arbeidstimer, timer.timer, gyldighetsperiode, kilde = kilde)),
                )
            }

            AktivitetType.Syk,
            AktivitetType.Fravær,
            -> {
                opplysninger.add(Faktum(Beregning.arbeidsdag, false, gyldighetsperiode, kilde = kilde))
                opplysninger.add(Faktum(Beregning.arbeidstimer, 0.0, gyldighetsperiode, kilde = kilde))
            }

            null -> {
                opplysninger.add(Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde))
                opplysninger.add(Faktum(Beregning.arbeidstimer, 0.0, gyldighetsperiode, kilde = kilde))
            }
        }

        opplysninger.add(Faktum(Beregning.meldt, dag.meldt, gyldighetsperiode, kilde = kilde))
    }
    return opplysninger.toList()
}

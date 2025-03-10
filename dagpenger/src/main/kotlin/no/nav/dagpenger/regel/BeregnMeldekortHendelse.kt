package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.roundToInt

class BeregnMeldekortHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    meldekortId: UUID,
    opprettet: LocalDateTime,
    private val meldekort: Meldekort,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = MeldekortId(meldekort.eksternMeldekortId),
        skjedde = meldekort.innsendtTidspunkt.toLocalDate(),
        fagsakId = 0,
        opprettet = opprettet,
    ) {
    override val forretningsprosess = Søknadsprosess()

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring = Regelkjøring(skjedde, opplysninger, forretningsprosess)

    override fun behandling(forrigeBehandling: Behandling?): Behandling {
        requireNotNull(forrigeBehandling) { "Må ha en behandling å ta utgangspunkt i" }
        val kilde = Systemkilde(meldekort.meldingsreferanseId, opprettet)

        return Behandling(
            behandler = this,
            basertPå = listOf(forrigeBehandling),
            opplysninger =
                listOf(
                    // TODO: Fastsett terskel ved innvilgelse
                    Faktum(Beregning.terskel, 0.5),
                    Faktum(
                        Beregning.meldeperiode,
                        Periode(meldekort.fom, meldekort.tom),
                        Gyldighetsperiode(meldekort.fom, meldekort.tom),
                        kilde = kilde,
                    ),
                ) +
                    meldekort.dager.flatMap { dag ->
                        val gyldighetsperiode = Gyldighetsperiode(dag.dato, dag.dato)

                        // TODO: Dette bør være en double
                        val timer = dag.aktiviteter.sumOf { it.timer?.inWholeHours ?: 0 }.toInt()
                        // TODO: Hva om det er flere aktiviteter?
                        val type = dag.aktiviteter.firstOrNull()?.type
                        when (type) {
                            AktivitetType.Arbeid -> {
                                listOf(
                                    Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde),
                                    Faktum(Beregning.arbeidstimer, timer, gyldighetsperiode, kilde = kilde),
                                )
                            }

                            AktivitetType.Syk,
                            AktivitetType.Utdanning,
                            AktivitetType.Fravær,
                            -> listOf(Faktum(Beregning.arbeidsdag, false, gyldighetsperiode, kilde = kilde))

                            null -> {
                                listOf(
                                    Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde),
                                    Faktum(Beregning.arbeidstimer, 0, gyldighetsperiode, kilde = kilde),
                                )
                            }
                        } + Faktum(Beregning.meldt, dag.meldt, gyldighetsperiode, kilde = kilde)
                    },
        ).apply {
            val fabrikk = BeregningsperiodeFabrikk(meldekort.fom, meldekort.tom, this.opplysninger())
            val periode = fabrikk.lagBeregningsperiode()

            periode.forbruksdager.forEach {
                val gyldighetsperiode = Gyldighetsperiode(it.dato, it.dato)
                opplysninger.leggTil(Faktum(Beregning.utbetaling, it.tilUtbetaling.roundToInt(), gyldighetsperiode))
            }

            println(periode.forbruksdager)
        }
    }

    override fun kontrollpunkter(): List<Kontrollpunkt> = emptyList()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean = false
}

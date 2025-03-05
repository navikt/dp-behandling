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
import no.nav.dagpenger.regel.beregning.Beregning
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        TODO("Not yet implemented")
    }

    override fun behandling(forrigeBehandling: Behandling?): Behandling {
        requireNotNull(forrigeBehandling) { "Må ha en behandling å ta utgangspunkt i" }

        return Behandling(
            behandler = this,
            basertPå = listOf(forrigeBehandling),
            opplysninger =
                meldekort.dager.flatMap { dag ->
                    val kilde = Systemkilde(meldingsreferanseId, opprettet)
                    val gyldighetsperiode = Gyldighetsperiode(dag.dato, dag.dato)
                    // TODO: Dette bør være en double
                    val timer = dag.aktiviteter.sumOf { it.timer?.inWholeHours ?: 0 }.toInt()
                    // TODO: Hva om det er flere aktiviteter?
                    val type = dag.aktiviteter.first().type
                    // TODO: Det finnes også tomme dager, bør defaulte til arbeidsdag med 0 arbeidstimer
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
                    }
                },
        )
    }

    override fun kontrollpunkter(): List<Kontrollpunkt> {
        TODO("Not yet implemented")
    }

    override fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate {
        TODO("Not yet implemented")
    }

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
        TODO("Not yet implemented")
    }
}

package no.nav.dagpenger.regel.hendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.hendelser.SamordningId
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.IkkeOpprettet
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SamordningHendelseTest {
    private val gjelderDato = LocalDate.of(2025, 1, 1)

    private fun hendelse() =
        SamordningHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            ident = "12345678910",
            eksternId = SamordningId(UUIDv7.ny()),
            gjelderDato = gjelderDato,
            opprettet = LocalDateTime.now(),
        )

    @Test
    fun `uten en tidligere behandling blir det ikke opprettet en ny behandling`() {
        val resultat = hendelse().behandling(null, TemporalCollection())

        resultat shouldBe
            IkkeOpprettet("Samordningshendelse overlapper ikke med en aktiv rettighetsperiode fra $gjelderDato")
    }

    @Test
    fun `overlapper gjelderDato med en aktiv rettighetsperiode blir det ikke opprettet en ny behandling`() {
        val rettighetstatus =
            TemporalCollection<Rettighetstatus>().apply {
                put(gjelderDato.minusMonths(1), Rettighetstatus(gjelderDato.minusMonths(1), true, UUID.randomUUID(), UUID.randomUUID()))
            }

        // forrigeBehandling er null her også, men overlappsjekken skal slå til før den sjekken
        val resultat = hendelse().behandling(null, rettighetstatus)

        resultat shouldBe
            IkkeOpprettet("Samordningshendelse kan ikke starte en ny behandlingskjede uten en tidligere behandling")
    }

    @Test
    fun `overlapper ikke gjelderDato med en aktiv rettighetsperiode faller den videre til neste sjekk`() {
        val rettighetstatus =
            TemporalCollection<Rettighetstatus>().apply {
                put(gjelderDato.minusMonths(1), Rettighetstatus(gjelderDato.minusMonths(1), false, UUID.randomUUID(), UUID.randomUUID()))
            }

        val resultat = hendelse().behandling(null, rettighetstatus)

        resultat shouldBe
            IkkeOpprettet("Samordningshendelse overlapper ikke med en aktiv rettighetsperiode fra $gjelderDato")
    }
}

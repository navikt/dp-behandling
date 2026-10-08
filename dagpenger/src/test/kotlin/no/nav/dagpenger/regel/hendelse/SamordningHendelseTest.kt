package no.nav.dagpenger.regel.hendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.Rettighetstidslinje
import no.nav.dagpenger.modell.hendelser.SamordningId
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.IkkeOpprettet
import no.nav.dagpenger.regel.RegelverkDagpenger
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

    private fun rettighetsperioderMed(harRett: Boolean) =
        Rettighetstidslinje
            .fraPerioder(
                mapOf(
                    RegelverkDagpenger.ident to
                        listOf(
                            Rettighetstatus(
                                fraOgMed = gjelderDato.minusMonths(1),
                                tilOgMed = LocalDate.MAX,
                                harRett = harRett,
                                behandlingId = UUID.randomUUID(),
                                behandlingskjedeId = UUID.randomUUID(),
                                opplysningId = UUID.randomUUID(),
                            ),
                        ),
                ),
            ).forRegelverk(RegelverkDagpenger)

    @Test
    fun `uten en tidligere behandling blir det ikke opprettet en ny behandling`() {
        val resultat = hendelse().behandling(null, Rettighetstidslinje().forRegelverk(RegelverkDagpenger))

        resultat shouldBe
            IkkeOpprettet("Samordningshendelse overlapper ikke med en aktiv rettighetsperiode fra $gjelderDato")
    }

    @Test
    fun `overlapper gjelderDato med en aktiv rettighetsperiode blir det ikke opprettet en ny behandling`() {
        // forrigeBehandling er null her også, men overlappsjekken skal slå til før den sjekken
        val resultat = hendelse().behandling(null, rettighetsperioderMed(harRett = true))

        resultat shouldBe
            IkkeOpprettet("Samordningshendelse kan ikke starte en ny behandlingskjede uten en tidligere behandling")
    }

    @Test
    fun `overlapper ikke gjelderDato med en aktiv rettighetsperiode faller den videre til neste sjekk`() {
        val resultat = hendelse().behandling(null, rettighetsperioderMed(harRett = false))

        resultat shouldBe
            IkkeOpprettet("Samordningshendelse overlapper ikke med en aktiv rettighetsperiode fra $gjelderDato")
    }
}

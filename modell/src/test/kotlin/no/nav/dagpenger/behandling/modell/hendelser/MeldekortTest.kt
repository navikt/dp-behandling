package no.nav.dagpenger.behandling.modell.hendelser

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.hjelpere.januar
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class MeldekortTest {
    @Test
    fun `sorterer meldekort etter meldekortets fra dato`() {
        val meldekort1 =
            Meldekort(
                id = UUID.randomUUID(),
                meldingsreferanseId = UUID.randomUUID(),
                ident = "12345678901",
                eksternMeldekortId = MeldekortId("1"),
                fom = 1.januar(2025),
                tom = 14.januar(2025),
                kilde = MeldekortKilde("Rolle", "Ident"),
                dager = emptyList(),
                innsendtTidspunkt = LocalDateTime.now(),
                korrigeringAv = null,
                meldedato = LocalDate.now(),
                kanSendesFra = 13.januar(2025),
            )
        val meldekort2 =
            Meldekort(
                id = UUID.randomUUID(),
                meldingsreferanseId = UUID.randomUUID(),
                ident = "12345678901",
                eksternMeldekortId = MeldekortId("2"),
                fom = 15.januar(2025),
                tom = 30.januar(2025),
                kilde = MeldekortKilde("Rolle", "Ident"),
                dager = emptyList(),
                innsendtTidspunkt = LocalDateTime.now(),
                korrigeringAv = null,
                meldedato = LocalDate.now(),
                kanSendesFra = 29.januar(2025),
            )

        val liste = listOf(meldekort2, meldekort1)
        val sortertListe = liste.sorted()
        meldekort1 shouldBe sortertListe.first()
    }
}

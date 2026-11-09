package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.hendelser.MigreringId
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Utgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.AldersgrenseId
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.hendelse.MigrertYtelse
import no.nav.dagpenger.regel.hendelse.Tilleggsopplysning
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MigreringsprosessTest {
    @Test
    fun `etterspør aldersgrense når utgangspunktet er konfigurert for migrering`() {
        val produsent =
            Migreringsprosess(setOf(AldersgrenseId))
                .produsenter(prøvingsdato, grunnlag.forDato(prøvingsdato))
                .entries
                .single { (type, _) -> type.id == AldersgrenseId }
                .value

        (produsent is Ekstern<*>) shouldBe true
    }

    @Test
    fun `beholder utgangspunkt når opplysningen ikke er konfigurert for etterspørring`() {
        val produsent =
            Migreringsprosess(emptySet())
                .produsenter(prøvingsdato, grunnlag.forDato(prøvingsdato))
                .entries
                .single { (type, _) -> type.id == AldersgrenseId }
                .value

        (produsent is Utgangspunkt<*>) shouldBe true
    }

    private companion object {
        private val prøvingsdato = LocalDate.of(2026, 1, 5)

        private val grunnlag =
            MigrerYtelseHendelse(
                meldingsreferanseId = UUID.randomUUID(),
                ident = "12345678910",
                eksternId = MigreringId(UUID.randomUUID()),
                opprettet = LocalDateTime.now(),
                ytelse = MigrertYtelse(migereringTidspunkt = prøvingsdato, innvilgetTilOgMed = null, sakId = 13699879),
                tilleggsopplysninger = listOf(Tilleggsopplysning(Alderskrav.fødselsdato, LocalDate.of(1990, 1, 1))),
            ).lagFaktagrunnlag()
    }
}

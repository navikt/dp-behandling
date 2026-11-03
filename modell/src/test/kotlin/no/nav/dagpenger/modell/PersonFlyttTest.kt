package no.nav.dagpenger.modell

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import no.nav.dagpenger.modell.Behandling.TilstandType.Avbrutt
import no.nav.dagpenger.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.modell.Behandling.TilstandType.ForslagTilVedtak
import no.nav.dagpenger.modell.Behandling.TilstandType.Redigert
import no.nav.dagpenger.modell.Behandling.TilstandType.UnderBehandling
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PersonFlyttTest {
    private val ident = "12312312311"

    private fun behandling(
        basertPå: Behandling?,
        tilstand: Behandling.TilstandType,
    ) = Behandling.rehydrer(
        behandlingId = UUIDv7.ny(),
        behandler = TestHendelse(UUIDv7.ny(), ident, UUIDv7.ny()),
        gjeldendeOpplysninger = Opplysninger(),
        basertPå = basertPå,
        opprettet = LocalDateTime.now(),
        tilstand = tilstand,
        sistEndretTilstand = LocalDateTime.now(),
        avklaringer = emptyList(),
    )

    private fun flytt(
        behandling: Behandling,
        nyBasertPå: Behandling?,
    ) = FlyttBehandlingHendelse(
        meldingsreferanseId = UUIDv7.ny(),
        ident = ident,
        behandlingId = behandling.behandlingId,
        nyBasertPåId = nyBasertPå?.behandlingId,
        opprettet = LocalDateTime.now(),
    )

    private fun person(vararg kjeder: List<Behandling>) = Person(Ident(ident), kjeder.map { it.somKjede() })

    private fun Person.nyBehandling(gammel: Behandling) = behandlinger().single { it.behandler === gammel.behandler && it !== gammel }

    @Test
    fun `flytting til annen kjede lager ny behandling og avbryter den gamle`() {
        val a1 = behandling(null, Ferdig)
        val a2 = behandling(a1, ForslagTilVedtak)
        val b1 = behandling(null, Ferdig)
        val person = person(listOf(a1, a2), listOf(b1))

        person.håndter(flytt(a2, b1))

        val ny = person.nyBehandling(a2)
        ny.basertPå shouldBe b1
        ny.behandlingId shouldNotBeSameInstanceAs a2.behandlingId
        ny.harTilstand(Redigert) shouldBe true
        a2.harTilstand(Avbrutt) shouldBe true
        a2.basertPå shouldBe a1
        person.behandlinger().filter { it.behandlingskjedeId == b1.behandlingskjedeId } shouldContainExactly listOf(b1, ny)
    }

    @Test
    fun `flytting til ny rot gir egen kjede`() {
        val a1 = behandling(null, Ferdig)
        val a2 = behandling(a1, ForslagTilVedtak)
        val person = person(listOf(a1, a2))

        person.håndter(flytt(a2, null))

        val ny = person.nyBehandling(a2)
        ny.basertPå shouldBe null
        ny.behandlingskjedeId shouldBe ny.behandlingId
    }

    @Test
    fun `flytting innenfor samme kjede`() {
        val a1 = behandling(null, Ferdig)
        val a2 = behandling(a1, Ferdig)
        val a3 = behandling(a2, ForslagTilVedtak)
        val person = person(listOf(a1, a2, a3))

        person.håndter(flytt(a3, a1))

        val ny = person.nyBehandling(a3)
        ny.basertPå shouldBe a1
        a3.harTilstand(Avbrutt) shouldBe true
        person.behandlinger().filter { it.behandlingskjedeId == a1.behandlingskjedeId }.size shouldBe 4
    }

    @Test
    fun `ferdig behandling kan ikke flyttes`() {
        val a1 = behandling(null, Ferdig)
        val a2 = behandling(a1, Ferdig)
        val b1 = behandling(null, Ferdig)
        val person = person(listOf(a1, a2), listOf(b1))

        shouldThrow<IllegalStateException> { person.håndter(flytt(a2, b1)) }

        person.behandlinger().size shouldBe 3
    }

    @Test
    fun `flytting til forelder som ikke er ferdig avvises uten endring`() {
        val a1 = behandling(null, Ferdig)
        val a2 = behandling(a1, ForslagTilVedtak)
        val b1 = behandling(null, UnderBehandling)
        val person = person(listOf(a1, a2), listOf(b1))

        shouldThrow<IllegalArgumentException> { person.håndter(flytt(a2, b1)) }

        a2.harTilstand(ForslagTilVedtak) shouldBe true
        person.behandlinger().size shouldBe 3
    }

    @Test
    fun `kan ikke flytte behandling til seg selv`() {
        val a1 = behandling(null, Ferdig)
        val a2 = behandling(a1, ForslagTilVedtak)
        val person = person(listOf(a1, a2))

        shouldThrow<IllegalArgumentException> { person.håndter(flytt(a2, a2)) }

        a2.harTilstand(ForslagTilVedtak) shouldBe true
    }
}

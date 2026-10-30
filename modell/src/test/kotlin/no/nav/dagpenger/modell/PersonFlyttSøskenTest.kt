package no.nav.dagpenger.modell

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.modell.Behandling.TilstandType.Avbrutt
import no.nav.dagpenger.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.modell.Behandling.TilstandType.ForslagTilVedtak
import no.nav.dagpenger.modell.Behandling.TilstandType.Redigert
import no.nav.dagpenger.modell.Behandling.TilstandType.TilBeslutning
import no.nav.dagpenger.modell.Behandling.TilstandType.UnderBehandling
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PersonFlyttSøskenTest {
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

    private fun flyttSøsken(
        behandling: Behandling,
        nyBasertPå: Behandling,
    ) = FlyttBehandlingHendelse(
        meldingsreferanseId = UUIDv7.ny(),
        ident = ident,
        behandlingId = behandling.behandlingId,
        nyBasertPåId = nyBasertPå.behandlingId,
        opprettet = LocalDateTime.now(),
    )

    @Test
    fun `søsken beholder identitet og basererer seg på ferdig søsken`() {
        val a = behandling(null, Ferdig)
        val b1 = behandling(a, Ferdig)
        val b2 = behandling(a, ForslagTilVedtak)
        val person = Person(Ident(ident), listOf(a.somKjede() leggTil b1 leggTil b2))

        person.håndter(flyttSøsken(b2, b1))

        val flyttet = person.behandlinger().single { it.behandlingId == b2.behandlingId }
        flyttet.basertPå shouldBe b1
        flyttet.behandlingskjedeId shouldBe a.behandlingskjedeId
        flyttet.harTilstand(Redigert) shouldBe true
        flyttet.aktiveAvklaringer().map { it.kode.kode } shouldContainExactlyInAnyOrder listOf("BehandlingFlyttet")
        person.behandlinger().map { it.behandlingId } shouldContainExactlyInAnyOrder
            listOf(a.behandlingId, b1.behandlingId, b2.behandlingId)
    }

    @Test
    fun `to røtter i ulike kjeder er ikke søsken og gir ny behandling`() {
        val a = behandling(null, Ferdig)
        val b = behandling(null, ForslagTilVedtak)
        val person = Person(Ident(ident), listOf(a.somKjede(), b.somKjede()))

        person.håndter(flyttSøsken(b, a))

        b.harTilstand(Avbrutt) shouldBe true
        person.behandlinger().single { it.basertPå === a }.behandlingId shouldNotBe b.behandlingId
    }

    @Test
    fun `søsken til beslutning kan ikke flyttes og blir værende på gammel forelder`() {
        val a = behandling(null, Ferdig)
        val b1 = behandling(a, Ferdig)
        val b2 = behandling(a, TilBeslutning)
        val person = Person(Ident(ident), listOf(a.somKjede() leggTil b1 leggTil b2))

        shouldThrow<IllegalStateException> { person.håndter(flyttSøsken(b2, b1)) }

        b2.basertPå shouldBe a
        person.behandlinger().size shouldBe 3
    }

    @Test
    fun `søsken som ikke er ferdig avvises uten endring`() {
        val a = behandling(null, Ferdig)
        val b1 = behandling(a, UnderBehandling)
        val b2 = behandling(a, ForslagTilVedtak)
        val person = Person(Ident(ident), listOf(a.somKjede() leggTil b1 leggTil b2))

        shouldThrow<IllegalArgumentException> { person.håndter(flyttSøsken(b2, b1)) }

        person.behandlinger().size shouldBe 3
        b2.harTilstand(ForslagTilVedtak) shouldBe true
    }
}

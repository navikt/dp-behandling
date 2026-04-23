package no.nav.dagpenger.behandling.modell

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BehandlingkjedeTest {
    @Test
    fun `en løvnode har ikke barn`() {
        val rotbehandling = nyKjede()
        rotbehandling.erLøvnode shouldBe true

        val oppdatertKjede = rotbehandling med nyBehandling(rotbehandling.rot)
        oppdatertKjede.erLøvnode shouldBe false
    }

    @Test
    fun `tillater ikke barn som ikke baserer seg på forelder`() {
        val rotbehandling = nyKjede()
        val barn = nyBehandling(null)

        shouldThrow<IllegalStateException> { rotbehandling med barn }
    }
}

private fun nyKjede() = nyBehandling().somKjede()

private val testhendelse =
    TestHendelse(
        meldingsreferanseId = UUIDv7.ny(),
        ident = "123456789011",
        søknadId = UUIDv7.ny(),
    )
private val opplysningstype1 = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "aktiv-opplysning1")

fun nyBehandling(basertPå: Behandling? = null) =
    Behandling.rehydrer(
        behandlingId = UUIDv7.ny(),
        behandler = testhendelse,
        gjeldendeOpplysninger = Opplysninger.med(Faktum(opplysningstype1, 1.0)),
        basertPå = basertPå,
        opprettet = LocalDateTime.now(),
        tilstand = Ferdig,
        sistEndretTilstand = LocalDateTime.now(),
        avklaringer = emptyList(),
    )

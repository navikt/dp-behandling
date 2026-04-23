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
        val rot = nyKjede()
        rot.erLøvnode shouldBe true

        val oppdatertKjede = rot med nyBehandling(rot)
        oppdatertKjede.erLøvnode shouldBe false
    }

    @Test
    fun `tillater ikke barn som ikke baserer seg på forelder`() {
        val rot = nyKjede()
        val barn = nyBehandling(null)

        shouldThrow<IllegalStateException> { rot med barn }
    }

    @Test
    fun `en kjede uten barn har 0 i dybde`() {
        val rot = nyKjede()
        rot.dybde shouldBe 0
        rot.etterkommere shouldBe 0
    }

    @Test
    fun `en kjede sin dybde bestemmes av det dypeste treet`() {
        val rot = nyKjede()
        val barn1 = rot.nyttBarn()
        val barn2 = rot.nyttBarn()

        val rotMedEttBarn = rot med barn1
        rotMedEttBarn.dybde shouldBe 1
        rotMedEttBarn.etterkommere shouldBe 1

        val rotMedToBarn = rotMedEttBarn med barn2

        rotMedToBarn.dybde shouldBe 1
        rotMedToBarn.etterkommere shouldBe 2
    }
}

private fun nyKjede() = nyBehandling().somKjede()

private fun Behandlingkjede.nyttBarn() = nyBehandling(this)

private val testhendelse =
    TestHendelse(
        meldingsreferanseId = UUIDv7.ny(),
        ident = "123456789011",
        søknadId = UUIDv7.ny(),
    )
private val opplysningstype1 = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "aktiv-opplysning1")

private fun nyBehandling(basertPå: Behandlingkjede? = null) =
    Behandling.rehydrer(
        behandlingId = UUIDv7.ny(),
        behandler = testhendelse,
        gjeldendeOpplysninger = Opplysninger.med(Faktum(opplysningstype1, 1.0)),
        basertPå = basertPå?.rot,
        opprettet = LocalDateTime.now(),
        tilstand = Ferdig,
        sistEndretTilstand = LocalDateTime.now(),
        avklaringer = emptyList(),
    )

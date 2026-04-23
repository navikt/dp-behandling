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
import java.util.UUID

class BehandlingkjedeTest {
    @Test
    fun `en løvnode har ikke barn`() {
        val rot = nyKjede()
        rot.erLøvnode shouldBe true

        val oppdatertKjede = rot leggTil nyBehandling(rot)
        oppdatertKjede.erLøvnode shouldBe false
    }

    @Test
    fun `tillater ikke barn som ikke baserer seg på forelder`() {
        val rot = nyKjede()
        val barn = nyBehandling(null)

        shouldThrow<IllegalStateException> { rot leggTil barn }
    }

    @Test
    fun `kan legge til barn til en etterkommer`() {
        val rot = nyKjede()
        val barn1 = rot.nyttBarn()
        val barn2 = rot.nyttBarn()

        val barnebarn = nyBehandling(barn1)

        val rotMedEttBarn = rot leggTil barn1 leggTil barn2
        val rotMedBarnebarn = rotMedEttBarn leggTil barnebarn

        rotMedBarnebarn.dybde shouldBe 2
        rotMedBarnebarn.etterkommere shouldBe 3
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

        val rotMedEttBarn = rot leggTil barn1
        rotMedEttBarn.dybde shouldBe 1
        rotMedEttBarn.etterkommere shouldBe 1

        val rotMedToBarn = rotMedEttBarn leggTil barn2

        rotMedToBarn.dybde shouldBe 1
        rotMedToBarn.etterkommere shouldBe 2
    }

    @Test
    fun `sjekker om en kjede inneholder en behandling`() {
        val rot = nyBehandling()
        val barn = nyBehandling(rot)
        val barnSomIkkeErIKjeden = nyBehandling(rot)

        val idBarnebarn = UUIDv7.ny()
        val barnebarn = nyBehandling(barn, idBarnebarn)

        val kjedeMedBarnebarn = rot.somKjede() leggTil barn leggTil barnebarn

        kjedeMedBarnebarn.dybde shouldBe 2
        kjedeMedBarnebarn.etterkommere shouldBe 2

        (rot in kjedeMedBarnebarn) shouldBe true
        (barn in kjedeMedBarnebarn) shouldBe true
        (barnSomIkkeErIKjeden in kjedeMedBarnebarn) shouldBe false
        (barnebarn in kjedeMedBarnebarn) shouldBe true
    }

    @Test
    fun `sjekker om en kjede inneholder samme behandling ID`() {
        val rot = nyKjede()
        val idBarn = UUIDv7.ny()
        val barn = nyBehandling(rot.rot, idBarn)
        val barnKopi = nyBehandling(rot.rot, idBarn)

        val kjedeMedEttBarn = rot leggTil barn

        (barn in kjedeMedEttBarn) shouldBe true
        (barnKopi in kjedeMedEttBarn) shouldBe false
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

private fun nyBehandling(basertPå: Behandlingkjede) = nyBehandling(basertPå.rot)

private fun nyBehandling(
    basertPå: Behandling? = null,
    behandlingId: UUID = UUIDv7.ny(),
) = Behandling.rehydrer(
    behandlingId = behandlingId,
    behandler = testhendelse,
    gjeldendeOpplysninger = Opplysninger.med(Faktum(opplysningstype1, 1.0)),
    basertPå = basertPå,
    opprettet = LocalDateTime.now(),
    tilstand = Ferdig,
    sistEndretTilstand = LocalDateTime.now(),
    avklaringer = emptyList(),
)

package no.nav.dagpenger.regel.hendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.IkkeOpprettet
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.Opprettet
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.mottak.SøknadInnsendtMessage.Companion.Fagsystem
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SøknadInnsendtHendelseTest {
    private fun hendelse(
        fagsakId: Int?,
        fagsystem: Fagsystem?,
        søknadstype: Søknadstype = Søknadstype.NySøknad,
    ) = SøknadInnsendtHendelse(
        meldingsreferanseId = UUID.randomUUID(),
        ident = "12345678910",
        søknadId = UUID.randomUUID(),
        gjelderDato = LocalDate.now(),
        fagsakId = fagsakId,
        opprettet = LocalDateTime.now(),
        søknadstype = søknadstype,
        fagsystem = fagsystem,
    )

    @Test
    fun `Arena uten fagsakId gir IkkeOpprettet`() {
        val resultat = hendelse(fagsakId = null, fagsystem = Fagsystem("Arena")).behandling(null, TemporalCollection())
        resultat shouldBe IkkeOpprettet("Hendelse av type SøknadInnsendtHendelse mangler fagsakId og har ingen behandling å basere seg på")
    }

    @Test
    fun `Arena med fagsakId 0 gir IkkeOpprettet`() {
        val resultat = hendelse(fagsakId = 0, fagsystem = Fagsystem("Arena")).behandling(null, TemporalCollection())
        resultat shouldBe IkkeOpprettet("Hendelse av type SøknadInnsendtHendelse mangler fagsakId og har ingen behandling å basere seg på")
    }

    @Test
    fun `ikke-Arena uten fagsakId gir Opprettet uten fagsakId-opplysning`() {
        val resultat = hendelse(fagsakId = null, fagsystem = Fagsystem("NyttFagsystem")).behandling(null, TemporalCollection())

        val behandling = (resultat as Opprettet).behandling
        behandling.opplysninger.har(SøknadInnsendtHendelse.fagsakIdOpplysningstype) shouldBe false
    }

    @Test
    fun `ikke-Arena med fagsakId 0 gir Opprettet uten at sentinelverdien lagres som opplysning`() {
        val resultat = hendelse(fagsakId = 0, fagsystem = Fagsystem("NyttFagsystem")).behandling(null, TemporalCollection())

        val behandling = (resultat as Opprettet).behandling
        behandling.opplysninger.har(SøknadInnsendtHendelse.fagsakIdOpplysningstype) shouldBe false
    }

    @Test
    fun `Arena med gyldig fagsakId gir Opprettet med fagsakId-opplysning`() {
        val resultat = hendelse(fagsakId = 123, fagsystem = Fagsystem("Arena")).behandling(null, TemporalCollection())

        val behandling = (resultat as Opprettet).behandling
        behandling.opplysninger.finnNullableOpplysning(SøknadInnsendtHendelse.fagsakIdOpplysningstype)?.verdi shouldBe 123
    }
}

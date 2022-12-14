package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakAvslåttBehov
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilgetBehov
import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingTest {
    val ident = "12345678901"
    val person = Person(ident)
    val søknadHendelse = SøknadHendelse(søknadUUID = UUID.randomUUID(), journalpostId = "123454", ident = ident)

    @Test
    fun `Ny søknad hendelse fører til innvilgelsesvedtak`() {
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        assertTrue(person.harBehandlinger())

        val aldersvilkårLøsning = AldersvilkårLøsning(ident, oppfylt = true, person.sisteBehandlingId())
        person.håndter(aldersvilkårLøsning)
        assertEquals(1, aldersvilkårLøsning.behov().size)
        val behov = aldersvilkårLøsning.behov().first()

        assertEquals(VedtakInnvilgetBehov, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(ident, behov.kontekst()["behandlingId"])
    }

    @Test
    fun `Ny søknad hendelse fører til avslagsvedtak`() {
        person.håndter(søknadHendelse)
        assertEquals(1, søknadHendelse.behov().size)
        assertTrue(person.harBehandlinger())

        val aldersvilkårLøsning = AldersvilkårLøsning(ident, oppfylt = false, person.sisteBehandlingId())
        person.håndter(aldersvilkårLøsning)
        assertEquals(1, aldersvilkårLøsning.behov().size)
        val behov = aldersvilkårLøsning.behov().first()
        assertEquals(VedtakAvslåttBehov, behov.type)
        assertEquals(ident, behov.kontekst()["ident"])
        assertNotNull(ident, behov.kontekst()["behandlingId"])
    }

    @Test
    fun `En søknadhendelse skal bare behandles en gang`() {
        person.håndter(søknadHendelse)
        person.håndter(søknadHendelse)

        assertEquals(1, person.antallBehandlinger())
    }

    @Test
    fun `Håndtere to unike søknadhendelser`() {
        val søknadHendelse2 = SøknadHendelse(UUID.randomUUID(), "1243", ident)

        person.håndter(søknadHendelse)
        person.håndter(søknadHendelse2)
        val aldersvilkårLøsning = AldersvilkårLøsning(ident, oppfylt = false, person.sisteBehandlingId())
        person.håndter(aldersvilkårLøsning)
        assertEquals(1, aldersvilkårLøsning.behov().size)
    }
}

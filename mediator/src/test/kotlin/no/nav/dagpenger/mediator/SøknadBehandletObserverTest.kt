package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.repository.TestBehandlinger
import no.nav.dagpenger.mediator.repository.TestProsess
import no.nav.dagpenger.modell.Arbeidssteg
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingAvbrutt
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.hendelser.ManuellId
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SøknadBehandletObserverTest {
    private val rapid = TestRapid()
    private val observer = SøknadBehandletObserver()

    @Test
    fun `publiserer søknad_behandlet når en behandling av søknad er ferdig`() {
        val hendelse = TestBehandlinger.lagTestHendelse(ident = "12345678901")

        observer.ferdig(
            behandlingFerdig(
                behandlingAv = hendelse,
                avgjørelse = Avgjørelse.Innvilgelse,
                rettighetsperioder = listOf(Rettighetsperiode(1.januar, LocalDate.MAX, true, true)),
            ),
        )
        observer.ferdigstill(rapid)

        rapid.inspektør.size shouldBeExactly 1
        val message = rapid.inspektør.message(0)
        message["@event_name"].asString() shouldBe "søknad_behandlet"
        message["ident"].asString() shouldBe "12345678901"
        message["søknadId"].asString() shouldBe hendelse.eksternId.id.toString()
        message["førteTil"].asString() shouldBe "Innvilgelse"
        message["rettighetsperioder"][0]["fraOgMed"].asText() shouldBe "2024-01-01"
        message["rettighetsperioder"][0]["tilOgMed"].isNull shouldBe true
        message["rettighetsperioder"][0]["harRett"].asBoolean() shouldBe true
    }

    @Test
    fun `ignorerer behandlinger som ikke er av en søknad`() {
        observer.ferdig(
            behandlingFerdig(
                behandlingAv = TestManuellHendelse(ident = "12345678901"),
                avgjørelse = Avgjørelse.Innvilgelse,
                rettighetsperioder = emptyList(),
            ),
        )
        observer.ferdigstill(rapid)

        rapid.inspektør.size shouldBeExactly 0
    }

    @Test
    fun `publiserer søknadsbehandling_avbrutt når en behandling av søknad avbrytes`() {
        val hendelse = TestBehandlinger.lagTestHendelse(ident = "12345678901")

        observer.avbrutt(behandlingAvbrutt(behandlingAv = hendelse, årsak = "Trukket"))
        observer.ferdigstill(rapid)

        rapid.inspektør.size shouldBeExactly 1
        val message = rapid.inspektør.message(0)
        message["@event_name"].asString() shouldBe "søknadsbehandling_avbrutt"
        message["ident"].asString() shouldBe "12345678901"
        message["søknadId"].asString() shouldBe hendelse.eksternId.id.toString()
        message["årsak"].asString() shouldBe "Trukket"
    }

    @Test
    fun `ignorerer avbrutte behandlinger som ikke er av en søknad`() {
        observer.avbrutt(
            behandlingAvbrutt(behandlingAv = TestManuellHendelse(ident = "12345678901"), årsak = null),
        )
        observer.ferdigstill(rapid)

        rapid.inspektør.size shouldBeExactly 0
    }

    private fun behandlingAvbrutt(
        behandlingAv: StartHendelse,
        årsak: String?,
    ): BehandlingAvbrutt =
        BehandlingAvbrutt(
            behandlingId = UUIDv7.ny(),
            hendelse = behandlingAv.eksternId,
            behandlingAv = behandlingAv,
            årsak = årsak,
        ).also { it.ident = "12345678901" }

    private fun behandlingFerdig(
        behandlingAv: StartHendelse,
        avgjørelse: Avgjørelse,
        rettighetsperioder: List<Rettighetsperiode>,
    ): BehandlingFerdig {
        val behandlingId = UUIDv7.ny()
        return BehandlingFerdig(
            Behandling.Resultat(
                behandlingId = behandlingId,
                basertPåBehandling = null,
                behandlingskjedeId = behandlingId,
                regelverk = RegelverkType("Test"),
                rettighetsperioder = rettighetsperioder,
                avgjørelse = avgjørelse,
                virkningsdato = 1.januar,
                behandlingAv = behandlingAv,
                opplysninger = Opplysninger(),
                automatiskBehandlet = true,
                godkjentAv = Arbeidssteg(Arbeidssteg.Oppgave.Godkjent),
                besluttetAv = Arbeidssteg(Arbeidssteg.Oppgave.Besluttet),
                opprettet = LocalDateTime.now(),
                sistEndret = LocalDateTime.now(),
            ),
        ).also { it.ident = "12345678901" }
    }

    private val Int.januar get() = LocalDate.of(2024, 1, this)

    private class TestManuellHendelse(
        ident: String,
    ) : StartHendelse(UUID.randomUUID(), ident, ManuellId(UUID.randomUUID()), LocalDate.now(), LocalDateTime.now()) {
        override val forretningsprosess = TestProsess()

        override fun behandling(
            forrigeBehandling: Behandling?,
            rettighetstatus: TemporalCollection<Rettighetstatus>,
        ): StartHendelseResultat = throw UnsupportedOperationException("Brukes bare for testing")
    }
}

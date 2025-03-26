package no.nav.dagpenger.behandling.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.mediator.HendelseMediator
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDateTime

internal class TestSaksbehandler2(
    private val testPerson: Mennesket,
    private val hendelseMediator: HendelseMediator,
    private val personRepository: PersonRepository,
    private val rapid: TestRapid,
) {
    fun beslutt() {
        hendelseMediator.behandle(
            BesluttBehandlingHendelse(
                meldingsreferanseId = UUIDv7.ny(),
                ident = testPerson.ident,
                behandlingId = testPerson.behandlingId,
                opprettet = LocalDateTime.now(),
                besluttetAv = Saksbehandler("NAV987987"),
            ),
            rapid,
        )
    }

    fun godkjenn() {
        hendelseMediator.behandle(
            GodkjennBehandlingHendelse(
                meldingsreferanseId = UUIDv7.ny(),
                ident = testPerson.ident,
                behandlingId = testPerson.behandlingId,
                godkjentAv = Saksbehandler("NAV123123"),
                opprettet = LocalDateTime.now(),
            ),
            rapid,
        )
    }

    fun sendTilbake() {
        hendelseMediator.behandle(
            SendTilbakeHendelse(
                meldingsreferanseId = UUIDv7.ny(),
                ident = testPerson.ident,
                behandlingId = testPerson.behandlingId,
                opprettet = LocalDateTime.now(),
            ),
            rapid,
        )
    }

    fun lukkAlleAvklaringer() {
        val behandling = personRepository.hent(testPerson.ident.tilPersonIdentfikator())?.behandlinger()?.first()
        behandling.shouldNotBeNull()
        val avklaringer: List<Avklaring> = behandling.aktiveAvklaringer()

        avklaringer.forEach { avklaring ->
            hendelseMediator.behandle(
                AvklaringKvittertHendelse(
                    meldingsreferanseId = UUIDv7.ny(),
                    ident = testPerson.ident,
                    avklaringId = avklaring.id,
                    behandlingId = behandling.behandlingId,
                    saksbehandler = "NAV123123",
                    begrunnelse = "",
                    opprettet = LocalDateTime.now(),
                ),
                rapid,
            )
        }
    }
}

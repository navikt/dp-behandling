package no.nav.dagpenger.behandling.helpers.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.mediator.HendelseMediator
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.behandling.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvar
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerbegrunnelse
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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

    fun lagBehandling(gjelder: LocalDate) {
        rapid.sendTestMessage(Meldingskatalog.opprettBehandling(testPerson.ident, gjelder), testPerson.ident)
    }

    fun lukkAlleAvklaringer() {
        val behandling = personRepository.hent(testPerson.ident.tilPersonIdentfikator())?.behandlinger()?.last()
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

    fun <T : Comparable<T>> endreOpplysning(
        opplysningstype: Opplysningstype<T>,
        verdi: T,
        begrunnelse: String = "",
        gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    ): UUID {
        val meldingsreferanseId = UUIDv7.ny()
        val kildeId = UUIDv7.ny()
        hendelseMediator.behandle(
            OpplysningSvarHendelse(
                meldingsreferanseId = meldingsreferanseId,
                ident = testPerson.ident,
                behandlingId = testPerson.behandlingId,
                listOf(
                    OpplysningSvar(
                        opplysningstype = opplysningstype,
                        verdi = verdi,
                        tilstand = OpplysningSvar.Tilstand.Faktum,
                        gyldighetsperiode = gyldighetsperiode,
                        utledetAvRegelsett = RegelverkDagpenger.regelsett.firstOrNull { it.produserer == opplysningstype }?.navn,
                        kilde =
                            Saksbehandlerkilde(
                                meldingsreferanseId = meldingsreferanseId,
                                saksbehandler = Saksbehandler("NAV123123"),
                                begrunnelse = Saksbehandlerbegrunnelse(begrunnelse),
                                opprettet = LocalDateTime.now(),
                                id = kildeId,
                            ),
                    ),
                ),
                opprettet = LocalDateTime.now(),
            ),
            rapid,
        )

        return kildeId
    }

    fun fjernOpplysning(opplysningstype: Opplysningstype<*>): OpplysningDTO {
        val opplysningSomSkalFjernes = testPerson.behandling.opplysninger.last { it.opplysningTypeId == opplysningstype.id.uuid }

        hendelseMediator.behandle(
            FjernOpplysningHendelse(
                meldingsreferanseId = UUIDv7.ny(),
                ident = testPerson.ident,
                behandlingId = testPerson.behandlingId,
                opplysningId = opplysningSomSkalFjernes.id,
                behovId = opplysningstype.behovId,
                opprettet = LocalDateTime.now(),
            ),
            rapid,
        )

        return opplysningSomSkalFjernes
    }

    fun flyttBehandlingTilNyKjede(
        behandlingId: UUID,
        nyBehandlingskjedeId: UUID? = null,
        fagsakId: Int? = null,
    ) {
        hendelseMediator.behandle(
            FlyttBehandlingHendelse(
                meldingsreferanseId = UUIDv7.ny(),
                ident = testPerson.ident,
                behandlingId = behandlingId,
                nyBasertPåId = nyBehandlingskjedeId,
                opplysninger =
                    listOfNotNull(
                        fagsakId?.let { Faktum(fagsakIdOpplysningstype, fagsakId) },
                    ),
                opprettet = LocalDateTime.now(),
            ),
            rapid,
        )
    }
}

package no.nav.dagpenger.behandling.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.mediator.Metrikk.totalTidBruktPerHendelse
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.mediator.melding.MeldingRepository
import no.nav.dagpenger.behandling.mediator.mottak.AvbrytBehandlingMessage
import no.nav.dagpenger.behandling.mediator.mottak.AvbrytBehandlingMottak
import no.nav.dagpenger.behandling.mediator.mottak.AvklaringIkkeRelevantMessage
import no.nav.dagpenger.behandling.mediator.mottak.AvklaringIkkeRelevantMottak
import no.nav.dagpenger.behandling.mediator.mottak.BehandlingStårFastMessage
import no.nav.dagpenger.behandling.mediator.mottak.BeregnMeldekortMottak
import no.nav.dagpenger.behandling.mediator.mottak.BeregnMeldekortMottak.BeregnMeldekortMessage
import no.nav.dagpenger.behandling.mediator.mottak.FjernOpplysningMessage
import no.nav.dagpenger.behandling.mediator.mottak.FjernOpplysningMottak
import no.nav.dagpenger.behandling.mediator.mottak.GodkjennBehandlingMessage
import no.nav.dagpenger.behandling.mediator.mottak.GodkjennBehandlingMottak
import no.nav.dagpenger.behandling.mediator.mottak.InnsendingFerdigstiltMottak
import no.nav.dagpenger.behandling.mediator.mottak.MeldekortInnsendtMessage
import no.nav.dagpenger.behandling.mediator.mottak.MeldekortInnsendtMottak
import no.nav.dagpenger.behandling.mediator.mottak.OppgaveReturnertTilSaksbehandler
import no.nav.dagpenger.behandling.mediator.mottak.OppgaveSendtTilKontroll
import no.nav.dagpenger.behandling.mediator.mottak.OpplysningSvarMessage
import no.nav.dagpenger.behandling.mediator.mottak.OpplysningSvarMottak
import no.nav.dagpenger.behandling.mediator.mottak.OpprettBehandlingMessage
import no.nav.dagpenger.behandling.mediator.mottak.OpprettBehandlingMottak
import no.nav.dagpenger.behandling.mediator.mottak.PåminnelseMottak
import no.nav.dagpenger.behandling.mediator.mottak.RekjørBehandlingMessage
import no.nav.dagpenger.behandling.mediator.mottak.RekjørBehandlingMottak
import no.nav.dagpenger.behandling.mediator.mottak.ReturnerTilSaksbehandlerMessage
import no.nav.dagpenger.behandling.mediator.mottak.SendtTilKontrollMessage
import no.nav.dagpenger.behandling.mediator.mottak.SøknadInnsendtMessage
import no.nav.dagpenger.behandling.mediator.mottak.SøknadInnsendtMottak
import no.nav.dagpenger.behandling.mediator.mottak.UtbetalingStatusMessage
import no.nav.dagpenger.behandling.mediator.mottak.UtbetalingStatusMottak
import no.nav.dagpenger.behandling.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.behandling.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.behandling.modell.hendelser.LåsHendelse
import no.nav.dagpenger.behandling.modell.hendelser.LåsOppHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.behandling.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.UtbetalingStatus
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.hendelse.OpprettBehandlingHendelse
import java.util.UUID

internal class MessageMediator(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: HendelseMediator,
    private val meldingRepository: MeldingRepository,
    meldekortRepository: MeldekortRepository,
    private val apiRepositoryPostgres: ApiRepositoryPostgres,
    opplysningstyper: Set<Opplysningstype<*>>,
) : IMessageMediator {
    init {
        AvbrytBehandlingMottak(rapidsConnection, this)
        AvklaringIkkeRelevantMottak(rapidsConnection, this)
        BeregnMeldekortMottak(rapidsConnection, this, meldekortRepository)
        FjernOpplysningMottak(rapidsConnection, this, opplysningstyper)
        GodkjennBehandlingMottak(rapidsConnection, this)
        InnsendingFerdigstiltMottak(rapidsConnection)
        MeldekortInnsendtMottak(rapidsConnection, this)
        OppgaveReturnertTilSaksbehandler(rapidsConnection, this)
        OppgaveSendtTilKontroll(rapidsConnection, this)
        OpplysningSvarMottak(rapidsConnection, this, opplysningstyper)
        OpprettBehandlingMottak(rapidsConnection, this)
        PåminnelseMottak(rapidsConnection, this)
        RekjørBehandlingMottak(rapidsConnection, this)
        SøknadInnsendtMottak(rapidsConnection, this)
        UtbetalingStatusMottak(rapidsConnection, this)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun behandle(
        hendelse: StartHendelse,
        message: SøknadInnsendtMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: AvbrytBehandlingHendelse,
        message: AvbrytBehandlingMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: AvklaringIkkeRelevantHendelse,
        message: AvklaringIkkeRelevantMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: OpplysningSvarHendelse,
        message: OpplysningSvarMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)

            apiRepositoryPostgres.behovLøst(
                hendelse.behandlingId,
                *hendelse.opplysninger.map { it.opplysningstype.behovId }.toTypedArray(),
            )
        }
    }

    override fun behandle(
        hendelse: MeldekortInnsendtHendelse,
        message: MeldekortInnsendtMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: PåminnelseHendelse,
        message: BehandlingStårFastMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: RekjørBehandlingHendelse,
        message: RekjørBehandlingMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: ForslagGodkjentHendelse,
        message: GodkjennBehandlingMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: LåsHendelse,
        message: SendtTilKontrollMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: LåsOppHendelse,
        message: ReturnerTilSaksbehandlerMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: StartHendelse,
        message: BeregnMeldekortMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: OpprettBehandlingHendelse,
        message: OpprettBehandlingMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    override fun behandle(
        hendelse: FjernOpplysningHendelse,
        message: FjernOpplysningMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
            apiRepositoryPostgres.behovLøst(hendelse.behandlingId, hendelse.behovId)
        }
    }

    override fun behandle(
        hendelse: UtbetalingStatus,
        message: UtbetalingStatusMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
        }
    }

    private fun <HENDELSE : PersonHendelse> behandle(
        hendelse: HENDELSE,
        message: KafkaMelding,
        håndter: (HENDELSE) -> Unit,
    ) {
        withMDC(message.tracinginfo()) {
            if (meldingRepository.erBehandlet(message.id)) {
                logger.warn { "Hendelse er allerede behandlet: ${hendelse.javaClass.simpleName}, hopper over behandling" }
                return@withMDC
            }

            totalTidBruktPerHendelse
                .labelValues(hendelse.javaClass.simpleName)
                .time {
                    logger.info { "Behandler hendelse: ${hendelse.javaClass.simpleName}" }

                    message.lagreMelding(meldingRepository)
                    håndter(hendelse) // @todo: feilhåndtering
                    meldingRepository.markerSomBehandlet(message.id)
                    logger.info { "Behandlet hendelse: ${hendelse.javaClass.simpleName}" }
                }
        }
    }
}

internal interface IMessageMediator {
    fun behandle(
        hendelse: StartHendelse,
        message: SøknadInnsendtMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: OpplysningSvarHendelse,
        message: OpplysningSvarMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: AvbrytBehandlingHendelse,
        message: AvbrytBehandlingMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: AvklaringIkkeRelevantHendelse,
        message: AvklaringIkkeRelevantMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: PåminnelseHendelse,
        message: BehandlingStårFastMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: RekjørBehandlingHendelse,
        message: RekjørBehandlingMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: MeldekortInnsendtHendelse,
        message: MeldekortInnsendtMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: ForslagGodkjentHendelse,
        message: GodkjennBehandlingMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: LåsHendelse,
        message: SendtTilKontrollMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: LåsOppHendelse,
        message: ReturnerTilSaksbehandlerMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: StartHendelse,
        message: BeregnMeldekortMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: OpprettBehandlingHendelse,
        message: OpprettBehandlingMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: FjernOpplysningHendelse,
        message: FjernOpplysningMessage,
        context: MessageContext,
    )

    fun behandle(
        hendelse: UtbetalingStatus,
        message: UtbetalingStatusMessage,
        context: MessageContext,
    )
}

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }

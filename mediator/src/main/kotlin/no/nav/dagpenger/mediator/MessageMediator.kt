package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.mediator.Metrikk.totalTidBruktPerHendelse
import no.nav.dagpenger.mediator.mottak.AvbrytBehandlingMessage
import no.nav.dagpenger.mediator.mottak.AvbrytBehandlingMottak
import no.nav.dagpenger.mediator.mottak.AvklaringIkkeRelevantMessage
import no.nav.dagpenger.mediator.mottak.AvklaringIkkeRelevantMottak
import no.nav.dagpenger.mediator.mottak.BehandlingStårFastMessage
import no.nav.dagpenger.mediator.mottak.BehovsløserForbruksdagerMottak
import no.nav.dagpenger.mediator.mottak.BeregnMeldekortMottak
import no.nav.dagpenger.mediator.mottak.FjernOpplysningMessage
import no.nav.dagpenger.mediator.mottak.FjernOpplysningMottak
import no.nav.dagpenger.mediator.mottak.GodkjennBehandlingMessage
import no.nav.dagpenger.mediator.mottak.GodkjennBehandlingMottak
import no.nav.dagpenger.mediator.mottak.InnsendingFerdigstiltMottak
import no.nav.dagpenger.mediator.mottak.MeldekortInnsendtMessage
import no.nav.dagpenger.mediator.mottak.MeldekortInnsendtMottak
import no.nav.dagpenger.mediator.mottak.OmgjøringMottak
import no.nav.dagpenger.mediator.mottak.OppgaveReturnertTilSaksbehandler
import no.nav.dagpenger.mediator.mottak.OppgaveSendtTilKontroll
import no.nav.dagpenger.mediator.mottak.OpplysningSvarMessage
import no.nav.dagpenger.mediator.mottak.OpplysningSvarMottak
import no.nav.dagpenger.mediator.mottak.PåminnelseMottak
import no.nav.dagpenger.mediator.mottak.RekjørBehandlingMessage
import no.nav.dagpenger.mediator.mottak.RekjørBehandlingMottak
import no.nav.dagpenger.mediator.mottak.ReturnerTilSaksbehandlerMessage
import no.nav.dagpenger.mediator.mottak.SendtTilKontrollMessage
import no.nav.dagpenger.mediator.mottak.UtbetalingStatusMessage
import no.nav.dagpenger.mediator.mottak.UtbetalingStatusMottak
import no.nav.dagpenger.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.mediator.repository.MeldekortRepository
import no.nav.dagpenger.mediator.repository.PersonRepository
import no.nav.dagpenger.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.modell.hendelser.LåsHendelse
import no.nav.dagpenger.modell.hendelser.LåsOppHendelse
import no.nav.dagpenger.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.modell.hendelser.PersonHendelse
import no.nav.dagpenger.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.UtbetalingStatus
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.melding.KafkaMelding
import no.nav.dagpenger.regelverk.melding.MeldingRepository
import tools.jackson.databind.JsonNode
import java.util.UUID

internal class MessageMediator(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: IHendelseMediator,
    private val meldingRepository: MeldingRepository,
    meldekortRepository: MeldekortRepository,
    private val apiRepositoryPostgres: ApiRepositoryPostgres,
    private val behovssporer: Behovssporer,
    opplysningstyper: Set<Opplysningstype<*>>,
    personRepository: PersonRepository,
) : IMessageMediator {
    init {
        // Generiske mottak
        AvbrytBehandlingMottak(rapidsConnection, this)
        AvklaringIkkeRelevantMottak(rapidsConnection, this)
        BeregnMeldekortMottak(rapidsConnection, this, meldekortRepository)
        FjernOpplysningMottak(rapidsConnection, this, opplysningstyper)
        GodkjennBehandlingMottak(rapidsConnection, this)
        InnsendingFerdigstiltMottak(rapidsConnection)
        MeldekortInnsendtMottak(rapidsConnection, this)
        BehovsløserForbruksdagerMottak(rapidsConnection, personRepository)
        OmgjøringMottak(rapidsConnection, this, meldekortRepository)
        OppgaveReturnertTilSaksbehandler(rapidsConnection, this)
        OppgaveSendtTilKontroll(rapidsConnection, this)
        OpplysningSvarMottak(rapidsConnection, this, opplysningstyper)
        PåminnelseMottak(rapidsConnection, this)
        RekjørBehandlingMottak(rapidsConnection, this, opplysningstyper)
        UtbetalingStatusMottak(rapidsConnection, this)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun behandle(
        hendelse: StartHendelse,
        message: KafkaMelding,
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

            behovssporer.behovLøst(
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
        hendelse: FjernOpplysningHendelse,
        message: FjernOpplysningMessage,
        context: MessageContext,
    ) {
        behandle(hendelse, message) {
            hendelseMediator.behandle(it, context)
            behovssporer.behovLøst(hendelse.behandlingId, hendelse.behovId)
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

internal interface IMessageMediator : HendelseMottaker {
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

fun JsonNode.asUUID(): UUID = this.asString().let { UUID.fromString(it) }

package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.aktivitet.Hendelse
import no.nav.dagpenger.mediator.BehandlingMetrikker.Companion.besluttetTeller
import no.nav.dagpenger.mediator.BehandlingMetrikker.Companion.godkjentTeller
import no.nav.dagpenger.mediator.BehandlingMetrikker.Companion.hendelseTeller
import no.nav.dagpenger.mediator.BehandlingMetrikker.Companion.opplysningSvarTeller
import no.nav.dagpenger.mediator.BehandlingMetrikker.Companion.sendtTilbakeTeller
import no.nav.dagpenger.mediator.BehandlingMetrikker.Companion.startHendelseMottattTeller
import no.nav.dagpenger.mediator.BehandlingMetrikker.Companion.utbetalingStatusTeller
import no.nav.dagpenger.mediator.Metrikk.tidBruktPerHendelse
import no.nav.dagpenger.mediator.repository.MeldekortRepository
import no.nav.dagpenger.mediator.repository.OppdateringRepository
import no.nav.dagpenger.mediator.repository.PersonRepository
import no.nav.dagpenger.mediator.utboks.Utboks
import no.nav.dagpenger.mediator.utboks.UtboksLagerPostgres
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.Person
import no.nav.dagpenger.modell.PersonHåndter
import no.nav.dagpenger.modell.PersonObservatør
import no.nav.dagpenger.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.LåsHendelse
import no.nav.dagpenger.modell.hendelser.LåsOppHendelse
import no.nav.dagpenger.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.modell.hendelser.PersonHendelse
import no.nav.dagpenger.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.UtbetalingStatus

internal class HendelseMediator(
    private val postgres: UtboksLagerPostgres,
    private val personRepository: PersonRepository,
    private val meldekortRepository: MeldekortRepository,
    private val oppdateringRepository: OppdateringRepository,
    private val behovMediator: BehovMediator = BehovMediator(),
    private val aktivitetsloggMediator: IAktivitetsloggMediator = AktivitetsloggMediator(),
    observatører: Collection<PersonObservatør> = emptySet(),
) : IHendelseMediator {
    private val observatører = observatører.toSet()

    private companion object {
        val logger = KotlinLogging.logger { }
        val sikkerlogg = KotlinLogging.logger("tjenestekall.HendelseMediator")
    }

    fun håndter(
        hendelse: PersonHendelse,
        context: MessageContext,
    ) {
        hendelse.kontekster().forEach {
            if (!it.harFunksjonelleFeilEllerVerre()) håndter(it.hendelse(), context)
        }
    }

    private fun håndter(
        hendelser: List<Hendelse>,
        context: MessageContext,
    ) {
        val hendelsestyper = hendelser.groupBy { it.type }.mapValues { (_, hendelseliste) -> hendelseliste.single() }

        hendelsestyper.forEach { (type, hendelse) ->
            val data = hendelse.detaljer() + hendelse.kontekst()
            val melding = JsonMessage.newMessage(type.name, data)

            sikkerlogg.info { "sender hendelse ${type.name}:\n${melding.toJson()}}" }
            logger.info { "sender hendelse for ${type.name}" }
            Span.current().addEvent("Publiserer hendelse", Attributes.of(AttributeKey.stringKey("hendelse"), type.name))
            context.publish(melding.toJson())
        }
    }

    private fun <Hendelse : PersonHendelse> hentPersonOgHåndter(
        hendelse: Hendelse,
        context: MessageContext,
        handler: (PersonHåndter) -> Unit,
    ) {
        val personidentifikator = Ident(hendelse.ident())
        hentPersonOgHåndter(personidentifikator, hendelse, context, handler)
    }

    private fun lagreMeldekort(hendelse: MeldekortInnsendtHendelse) {
        val personidentifikator = Ident(hendelse.ident())
        withLoggingContext(hendelse.kontekstMap()) {
            // Duplikatkontroll fordi RAMP sender oss samme meldekort flere ganger
            if (meldekortRepository.harMeldekort(hendelse.meldekort.eksternMeldekortId)) {
                logger.warn {
                    """Meldekort med eksternMeldekortId=${hendelse.meldekort.eksternMeldekortId} er 
                    |allerede lagret. Hopper over lagring.
                    """.trimMargin()
                }
                return
            }
            val person = personRepository.hent(personidentifikator)
            if (person != null) {
                meldekortRepository.lagre(hendelse.meldekort)
            } else {
                logger.warn { "Personen har ikke behandling(er) i dp-sak. Har ikke grunnlag til å behandle dette meldekortet." }
            }
        }
    }

    @WithSpan
    private fun <Hendelse : PersonHendelse> hentPersonOgHåndter(
        ident: Ident,
        hendelse: Hendelse,
        context: MessageContext,
        handler: (Person) -> Unit,
    ) {
        Span.current().apply {
            setAttribute("hendelse", hendelse.javaClass.simpleName)
            hendelse.kontekstMap().forEach {
                setAttribute(it.key, it.value)
            }
        }
        try {
            val personMediator = PersonMediator()
            val oppdateringObserver = OppdateringObserver()
            person(ident) { person ->
                person.registrer(personMediator)
                person.registrer(oppdateringObserver)
                observatører.forEach { observatør -> person.registrer(observatør) }
                tidBruktPerHendelse.labelValues(hendelse.javaClass.simpleName).time {
                    handler(person)
                }
                hendelseTeller.labelValues(hendelse.javaClass.simpleName).inc()
            }
            ferdigstill(context, personMediator, oppdateringObserver, hendelse)
        } catch (aktivitetException: Aktivitetslogg.AktivitetException) {
            sikkerlogg.error(
                aktivitetException,
            ) { "aktivitetslogg inneholder feil: ${aktivitetException.message} \n${hendelse.toLogString()}" }
            error("Feil ved håndtering av ${hendelse.javaClass.simpleName}. Se sikkerlogg for detaljer.")
        } catch (e: Exception) {
            logger.error(e) { "Feil ved håndtering av ${hendelse.javaClass.simpleName}." }
            sikkerlogg.error(e) { "aktivitetslogg inneholder feil: ${e.message} \n${hendelse.toLogString()}" }
            throw e
        }
    }

    private fun person(
        ident: Ident,
        handler: (Person) -> Unit,
    ) {
        personRepository.håndter(ident, handler)
    }

    @WithSpan
    private fun ferdigstill(
        context: MessageContext,
        personMediator: PersonMediator,
        oppdateringObserver: OppdateringObserver,
        hendelse: PersonHendelse,
    ) {
        val utboks = Utboks(context, postgres)

        personMediator.ferdigstill(utboks)
        oppdateringObserver.ferdigstill(oppdateringRepository, hendelse.meldingsreferanseId())

        if (!hendelse.harAktiviteter()) return
        if (hendelse.harFunksjonelleFeilEllerVerre()) {
            logger.info { "aktivitetslogg inneholder feil (se sikkerlogg)" }
            sikkerlogg.error { "aktivitetslogg inneholder feil:\n${hendelse.toLogString()}" }
            sikkerlogg.info { "aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}" }
        }

        håndter(hendelse, utboks)

        // Behov og aktivitetslogg blir ikke lagret i utboks
        behovMediator.håndter(context, hendelse)
        aktivitetsloggMediator.håndter(context, hendelse)
    }

    override fun behandle(
        hendelse: StartHendelse,
        context: MessageContext,
    ) {
        startHendelseMottattTeller.labelValues(hendelse.javaClass.simpleName).inc()
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: OpplysningSvarHendelse,
        context: MessageContext,
    ) {
        opplysningSvarTeller.inc()
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: AvklaringIkkeRelevantHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: AvklaringKvittertHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: PåminnelseHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: RekjørBehandlingHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: MeldekortInnsendtHendelse,
        context: MessageContext,
    ) {
        lagreMeldekort(hendelse)
    }

    override fun behandle(
        hendelse: ForslagGodkjentHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: LåsHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: LåsOppHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: AvbrytBehandlingHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: GodkjennBehandlingHendelse,
        context: MessageContext,
    ) {
        godkjentTeller.inc()
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: BesluttBehandlingHendelse,
        context: MessageContext,
    ) {
        besluttetTeller.inc()
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: SendTilbakeHendelse,
        context: MessageContext,
    ) {
        sendtTilbakeTeller.inc()
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: FjernOpplysningHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: UtbetalingStatus,
        context: MessageContext,
    ) {
        utbetalingStatusTeller.labelValues(hendelse.status.name.lowercase()).inc()
        lagreUtbetalingStatus(hendelse)
    }

    private fun lagreUtbetalingStatus(hendelse: UtbetalingStatus) {
        val behandling = personRepository.hentBehandling(hendelse.behandlingId)
        require(behandling != null) { "Fant ikke behandling med ${hendelse.behandlingId}" }
        personRepository.lagreUtbetalingStatus(hendelse)
    }

    override fun behandle(
        hendelse: FlyttBehandlingHendelse,
        context: MessageContext,
    ) {
        val behandling = personRepository.hentBehandling(hendelse.behandlingId)
        if (behandling == null) {
            logger.error { "Ingen behandling funnet for id ${hendelse.behandlingId}" }
            return
        }

        if (behandling.kanRedigeres()) {
            personRepository.flyttBehandling(hendelse.behandlingId, hendelse.nyBasertPåId)
        }

        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }
}

interface IHendelseMediator {
    fun behandle(
        hendelse: StartHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: OpplysningSvarHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: AvbrytBehandlingHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: AvklaringIkkeRelevantHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: AvklaringKvittertHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: PåminnelseHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: RekjørBehandlingHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: MeldekortInnsendtHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: ForslagGodkjentHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: LåsHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: LåsOppHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: GodkjennBehandlingHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: BesluttBehandlingHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: SendTilbakeHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: FjernOpplysningHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: FlyttBehandlingHendelse,
        context: MessageContext,
    )

    fun behandle(
        hendelse: UtbetalingStatus,
        context: MessageContext,
    )
}

package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KotlinLogging
import no.nav.dagpenger.aktivitetslogg.aktivitet.Hendelse
import no.nav.dagpenger.behandling.mediator.Metrikk.tidBruktPerHendelse
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.PersonHåndter
import no.nav.dagpenger.behandling.modell.PersonObservatør
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.behandling.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.behandling.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.LåsHendelse
import no.nav.dagpenger.behandling.modell.hendelser.LåsOppHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.behandling.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.regel.hendelse.BeregnMeldekortHendelse

internal class HendelseMediator(
    private val personRepository: PersonRepository,
    private val meldekortRepository: MeldekortRepository,
    private val behovMediator: BehovMediator = BehovMediator(),
    private val aktivitetsloggMediator: AktivitetsloggMediator = AktivitetsloggMediator(),
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

    private fun lagreMeldekort(
        hendelse: MeldekortInnsendtHendelse,
        context: MessageContext,
    ) {
        val personidentifikator = Ident(hendelse.ident())
        val person = personRepository.hent(personidentifikator)
        if (person != null) {
            meldekortRepository.lagre(hendelse.meldekort)
        } else {
            logger.warn { "Vi kjenner ikke personen" }
        }
    }

    @WithSpan
    private fun <Hendelse : PersonHendelse> hentPersonOgHåndter(
        ident: Ident,
        hendelse: Hendelse,
        context: MessageContext,
        handler: (Person) -> Unit,
    ) {
        Span.current().setAttribute("hendelse", hendelse.javaClass.simpleName)
        try {
            val personMediator = PersonMediator()
            person(ident) { person ->
                person.registrer(personMediator)
                observatører.forEach { observatør -> person.registrer(observatør) }
                tidBruktPerHendelse.labelValues(hendelse.javaClass.simpleName).time {
                    handler(person)
                }
            }
            ferdigstill(context, personMediator, hendelse)
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
        hendelse: PersonHendelse,
    ) {
        personMediator.ferdigstill(context)
        if (!hendelse.harAktiviteter()) return
        if (hendelse.harFunksjonelleFeilEllerVerre()) {
            logger.info("aktivitetslogg inneholder feil (se sikkerlogg)")
            sikkerlogg.error("aktivitetslogg inneholder feil:\n${hendelse.toLogString()}")
        } else {
            sikkerlogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        }

        // TODO: Fjern denne når vi fjerner Behandlingshendelser
        håndter(hendelse, context)
        behovMediator.håndter(context, hendelse)
        aktivitetsloggMediator.håndter(context, hendelse)
    }

    override fun behandle(
        hendelse: StartHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: OpplysningSvarHendelse,
        context: MessageContext,
    ) {
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
        lagreMeldekort(hendelse, context)
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
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: BesluttBehandlingHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: SendTilbakeHendelse,
        context: MessageContext,
    ) {
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }

    override fun behandle(
        hendelse: BeregnMeldekortHendelse,
        context: MessageContext,
    ) {
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
        hendelse: FlyttBehandlingHendelse,
        context: MessageContext,
    ) {
        val behandling = personRepository.hentBehandling(hendelse.behandlingId)
        if (behandling == null) {
            logger.error { "Ingen behandling funnet for id ${hendelse.behandlingId}" }
            return
        }
        if (behandling.kanFlyttes()) {
            personRepository.flyttBehandling(hendelse.behandlingId, hendelse.nyBasertPåId)
        }
        hentPersonOgHåndter(hendelse, context) { person ->
            person.håndter(hendelse)
        }
    }
}

internal interface IHendelseMediator {
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
        hendelse: BeregnMeldekortHendelse,
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
}

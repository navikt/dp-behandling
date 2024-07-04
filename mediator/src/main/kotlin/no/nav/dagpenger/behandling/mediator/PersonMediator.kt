package no.nav.dagpenger.behandling.mediator

import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KotlinLogging
import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.PersonHåndter
import no.nav.dagpenger.behandling.modell.PersonObservatør
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.helse.rapids_rivers.withMDC

internal class PersonMediator(
    private val personRepository: PersonRepository,
    private val aktivitetsloggMediator: AktivitetsloggMediator,
    private val behovMediator: BehovMediator,
    private val hendelseMediator: HendelseMediator,
    private val observatører: Set<PersonObservatør> = emptySet(),
) : PersonHåndter {
    private companion object {
        val logger = KotlinLogging.logger { }
        val sikkerLogger = KotlinLogging.logger("tjenestekall")
    }

    override fun håndter(hendelse: SøknadInnsendtHendelse) {
        behandle(hendelse) { person ->
            person.håndter(hendelse)
        }
    }

    override fun håndter(hendelse: AvklaringIkkeRelevantHendelse) {
        behandle(hendelse) { person ->
            person.håndter(hendelse)
        }
    }

    override fun håndter(hendelse: OpplysningSvarHendelse) {
        behandle(hendelse) { person ->
            person.håndter(hendelse)
        }
    }

    override fun håndter(hendelse: AvbrytBehandlingHendelse) {
        behandle(hendelse) { person ->
            person.håndter(hendelse)
        }
    }

    override fun håndter(hendelse: ForslagGodkjentHendelse) {
        behandle(hendelse) { person ->
            person.håndter(hendelse)
        }
    }

    override fun håndter(hendelse: PåminnelseHendelse) {
        behandle(hendelse) { person ->
            person.håndter(hendelse)
        }
    }

    @WithSpan
    private fun behandle(
        hendelse: PersonHendelse,
        håndter: (Person) -> Unit,
    ) = try {
        val person = hentEllerOpprettPerson(hendelse)
        observatører.forEach { person.registrer(it) }
        håndter(person)
        lagre(person)
        ferdigstill(hendelse)
    } catch (err: Aktivitetslogg.AktivitetException) {
        logger.error("alvorlig feil i aktivitetslogg (se sikkerlogg for detaljer)")

        withMDC(err.kontekst()) {
            sikkerLogger.error("alvorlig feil i aktivitetslogg: ${err.message}", err)
        }
        throw err
    } catch (e: Exception) {
        errorHandler(e, e.message ?: "Ukjent feil")
        throw e
    }

    private fun lagre(person: Person) {
        personRepository.lagre(person)
    }

    private fun hentEllerOpprettPerson(hendelse: PersonHendelse): Person {
        val person = personRepository.hent(hendelse.ident().tilPersonIdentfikator())
        return person ?: Person(Ident(hendelse.ident()))
    }

    @WithSpan
    private fun ferdigstill(hendelse: PersonHendelse) {
        if (!hendelse.harAktiviteter()) return
        if (hendelse.harFunksjonelleFeilEllerVerre()) {
            logger.info("aktivitetslogg inneholder feil (se sikkerlogg)")
            sikkerLogger.error("aktivitetslogg inneholder feil:\n${hendelse.toLogString()}")
        } else {
            sikkerLogger.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        }
        sikkerLogger.info("aktivitetslogg inneholder meldinger: ${hendelse.toLogString()}")
        hendelseMediator.håndter(hendelse)
        behovMediator.håndter(hendelse)
        aktivitetsloggMediator.håndter(hendelse)
    }

    private fun errorHandler(
        err: Exception,
        message: String,
        context: Map<String, String> = emptyMap(),
    ) {
        logger.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        withMDC(context) { sikkerLogger.error("alvorlig feil: ${err.message}\n\t$message", err) }
    }
}

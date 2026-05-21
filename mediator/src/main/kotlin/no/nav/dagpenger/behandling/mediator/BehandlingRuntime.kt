package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import no.nav.dagpenger.behandling.mediator.api.behandlingApi
import no.nav.dagpenger.behandling.mediator.audit.Auditlogg
import no.nav.dagpenger.behandling.mediator.meldekort.MeldekortBehandlingskø
import no.nav.dagpenger.behandling.mediator.melding.PostgresMeldingRepository
import no.nav.dagpenger.behandling.mediator.mottak.ArenaOppgaveMottak
import no.nav.dagpenger.behandling.mediator.mottak.MarkerMeldekortSomBehandletMottak
import no.nav.dagpenger.behandling.mediator.mottak.SakRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.KildeRepository
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.VentendeMeldekortDings
import no.nav.dagpenger.behandling.mediator.utboks.UtboksLagerPostgres
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.regelverk.RegelverkRegistrering
import javax.sql.DataSource

/**
 * Kapsler all wiring av repositories, mediatorer, mottak og API.
 * Brukes av ApplicationBuilder i prod og SimulertDagpengerSystem i test.
 */
class BehandlingRuntime(
    private val dataSource: DataSource,
    val rapidsConnection: RapidsConnection,
    private val auditlogg: Auditlogg,
    private val regelverk: List<RegelverkRegistrering>,
    private val aktivitetsloggMediator: IAktivitetsloggMediator = AktivitetsloggMediator(),
    private val messageContextFactory: (ident: String) -> MessageContext = { rapidsConnection as MessageContext },
) {
    private val opplysningstyper: Set<Opplysningstype<*>> = regelverk.flatMap { it.opplysningstyper }.toSet()

    private val kildeRepository = KildeRepository(dataSource)
    private val opplysningerRepository = OpplysningerRepositoryPostgres(dataSource, kildeRepository)
    private val avklaringRepository = AvklaringRepositoryPostgres(dataSource, kildeRepository)
    private val prosessregister = Prosessregister()

    val personRepository: PersonRepository =
        PersonRepositoryPostgres(
            dataSource,
            BehandlingRepositoryPostgres(
                dataSource,
                opplysningerRepository,
                avklaringRepository,
                kildeRepository,
                prosessregister,
            ),
        )

    val meldekortRepository = MeldekortRepositoryPostgres(dataSource)
    private val ventendeMeldekort = VentendeMeldekortDings(meldekortRepository)

    private val behovssporer = Behovssporer(dataSource)

    val hendelseMediator: IHendelseMediator =
        HendelseMediator(
            UtboksLagerPostgres(dataSource),
            personRepository,
            meldekortRepository,
            behovMediator = BehovMediator(behovssporer),
            aktivitetsloggMediator = aktivitetsloggMediator,
            observatører = listOf(ventendeMeldekort, BehandlingMetrikker()),
        )

    private val postgresMeldingRepository = PostgresMeldingRepository(dataSource)
    private val apiRepositoryPostgres = ApiRepositoryPostgres(dataSource, postgresMeldingRepository, behovssporer)

    fun registrerMottak() {
        ArenaOppgaveMottak(rapidsConnection, SakRepositoryPostgres(dataSource))
        MarkerMeldekortSomBehandletMottak(rapidsConnection, meldekortRepository)

        avklaringRepository.registerObserver(AvklaringKafkaObservatør(rapidsConnection))

        MessageMediator(
            rapidsConnection = rapidsConnection,
            hendelseMediator = hendelseMediator,
            meldingRepository = postgresMeldingRepository,
            opplysningstyper = opplysningstyper,
            meldekortRepository = meldekortRepository,
            apiRepositoryPostgres = apiRepositoryPostgres,
            behovssporer = behovssporer,
            personRepository = personRepository,
        ).apply {
            regelverk.forEach { it.registrer(rapidsConnection, this, prosessregister) }
        }
    }

    fun lagreOpplysningstyper() {
        opplysningerRepository.lagreOpplysningstyper(opplysningstyper)
    }

    val api: Application.() -> Unit = {
        behandlingApi(
            personRepository = personRepository,
            hendelseMediator = hendelseMediator,
            auditlogg = auditlogg,
            opplysningstyper = opplysningstyper,
            apiRepositoryPostgres = apiRepositoryPostgres,
            meldekortRepository = meldekortRepository,
            messageContext = messageContextFactory,
        )
    }

    fun meldekortBehandlingskø(messageContext: MessageContext) =
        MeldekortBehandlingskø(dataSource, personRepository, meldekortRepository, messageContext)
}

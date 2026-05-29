package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import no.nav.dagpenger.mediator.api.auth.AuthFactory
import no.nav.dagpenger.mediator.api.behandlingApi
import no.nav.dagpenger.mediator.audit.Auditlogg
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.mediator.meldekort.MeldekortBehandlingskø
import no.nav.dagpenger.mediator.melding.PostgresMeldingRepository
import no.nav.dagpenger.mediator.mottak.ArenaOppgaveMottak
import no.nav.dagpenger.mediator.mottak.MarkerMeldekortSomBehandletMottak
import no.nav.dagpenger.mediator.mottak.MeldekortBehandlingsresultatKontrollregningMottak
import no.nav.dagpenger.mediator.mottak.SakRepositoryPostgres
import no.nav.dagpenger.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.mediator.repository.KildeRepository
import no.nav.dagpenger.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.mediator.repository.OppdateringRepositoryPostgres
import no.nav.dagpenger.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.mediator.repository.PersonRepository
import no.nav.dagpenger.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.mediator.repository.VentendeMeldekortDings
import no.nav.dagpenger.mediator.utboks.UtboksLagerPostgres
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.OpplysningstypeRegister
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.regel.OpplysningsTyper.DagensDatoId
import no.nav.dagpenger.regel.OpplysningsTyper.EttBeregnetVirkningstidspunktId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagUtenVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.KravPåDagpengerId
import no.nav.dagpenger.regel.OpplysningsTyper.UnntakForArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.andreØkonomiskeYtelserId
import no.nav.dagpenger.regel.OpplysningsTyper.ikkeKravPåLønnFraTidligereArbeidsgiverId
import no.nav.dagpenger.regelverk.RegelverkRegistrering

/**
 * Kapsler all wiring av repositories, mediatorer, mottak og API.
 * Brukes av ApplicationBuilder i prod og SimulertDagpengerSystem i test.
 */
class BehandlingRuntime(
    private val authFactory: AuthFactory,
    private val dbSession: DatabaseSession,
    val rapidsConnection: RapidsConnection,
    private val auditlogg: Auditlogg,
    private val regelverk: List<RegelverkRegistrering>,
    private val aktivitetsloggMediator: IAktivitetsloggMediator = AktivitetsloggMediator(),
    private val messageContextFactory: (ident: String) -> MessageContext = { rapidsConnection as MessageContext },
) {
    private val opplysningstyper: Set<Opplysningstype<*>> = regelverk.flatMap { it.opplysningstyper }.toSet()
    private val historiskeOpplysningstyper =
        setOf(
            DagensDatoId,
            EttBeregnetVirkningstidspunktId,
            KravPåDagpengerId,
            UnntakForArbeidssøkerId,
            andreØkonomiskeYtelserId,
            ikkeKravPåLønnFraTidligereArbeidsgiverId,
            GrunnlagUtenVernepliktId,
        )
    private val opplysningstypeRegister: OpplysningstypeRegister = OpplysningstypeRegister(opplysningstyper, historiskeOpplysningstyper)

    private val kildeRepository = KildeRepository(dbSession)
    private val opplysningerRepository = OpplysningerRepositoryPostgres(dbSession, kildeRepository, opplysningstypeRegister)
    private val avklaringRepository = AvklaringRepositoryPostgres(dbSession, kildeRepository)
    private val prosessregister = Prosessregister()

    val personRepository: PersonRepository =
        PersonRepositoryPostgres(
            dbSession,
            BehandlingRepositoryPostgres(
                dbSession,
                opplysningerRepository,
                avklaringRepository,
                kildeRepository,
                prosessregister,
                opplysningstypeRegister,
            ),
        )

    val meldekortRepository = MeldekortRepositoryPostgres(dbSession)
    private val ventendeMeldekort = VentendeMeldekortDings(meldekortRepository)

    private val behovssporer = Behovssporer(dbSession)
    private val oppdateringRepository = OppdateringRepositoryPostgres(dbSession)

    val hendelseMediator: IHendelseMediator =
        HendelseMediator(
            UtboksLagerPostgres(dbSession),
            personRepository,
            meldekortRepository,
            oppdateringRepository = oppdateringRepository,
            behovMediator = BehovMediator(behovssporer),
            aktivitetsloggMediator = aktivitetsloggMediator,
            observatører = listOf(ventendeMeldekort, BehandlingMetrikker()),
        )

    private val postgresMeldingRepository = PostgresMeldingRepository(dbSession)
    private val apiRepositoryPostgres = ApiRepositoryPostgres(dbSession, postgresMeldingRepository, behovssporer)

    fun registrerMottak() {
        ArenaOppgaveMottak(rapidsConnection, SakRepositoryPostgres(dbSession))
        MarkerMeldekortSomBehandletMottak(rapidsConnection, meldekortRepository)
        MeldekortBehandlingsresultatKontrollregningMottak(rapidsConnection)

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
            authFactory = authFactory,
            personRepository = personRepository,
            hendelseMediator = hendelseMediator,
            auditlogg = auditlogg,
            opplysningstyper = opplysningstyper,
            apiRepositoryPostgres = apiRepositoryPostgres,
            messageContext = messageContextFactory,
            oppdateringRepository = oppdateringRepository,
        )
    }

    fun meldekortBehandlingskø(messageContext: MessageContext) =
        MeldekortBehandlingskø(dbSession, personRepository, meldekortRepository, messageContext)
}

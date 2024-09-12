package no.nav.dagpenger.behandling.mediator

import mu.KotlinLogging
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.clean
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.behandling.konfigurasjon.Configuration.config
import no.nav.dagpenger.behandling.mediator.api.behandlingApi
import no.nav.dagpenger.behandling.mediator.audit.AktivitetsloggAuditlogg
import no.nav.dagpenger.behandling.mediator.melding.PostgresHendelseRepository
import no.nav.dagpenger.behandling.mediator.observatør.KafkaBehandlingObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(
    config: Map<String, String>,
) : RapidsConnection.StatusListener {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val rapidsConnection: RapidsConnection =
        RapidApplication
            .Builder(RapidApplication.RapidApplicationConfig.fromEnv(config))
            .withKtorModule {
                behandlingApi(
                    personRepository = personRepository,
                    personMediator,
                    AktivitetsloggAuditlogg(aktivitetsloggMediator),
                    opplysningstyper,
                )
            }.build()

    private val opplysningRepository = OpplysningerRepositoryPostgres()
    private val behandlingRepository =
        BehandlingRepositoryPostgres(opplysningRepository, AvklaringRepositoryPostgres(AvklaringKafkaObservatør(rapidsConnection)))
    private val personRepository = PersonRepositoryPostgres(behandlingRepository)

    private val aktivitetsloggMediator = AktivitetsloggMediator(rapidsConnection)
    private val outbox = Outbox(rapidsConnection)
    private val personMediator: PersonMediator =
        PersonMediator(
            personRepository = personRepository,
            aktivitetsloggMediator = aktivitetsloggMediator,
            behovMediator = BehovMediator(outbox),
            hendelseMediator = HendelseMediator(outbox),
            observatører = setOf(KafkaBehandlingObservatør(outbox)),
            rapidsConnection = rapidsConnection,
        )

    // TODO: Last alle regler ved startup. Dette må inn i ett register.
    private val opplysningstyper: Set<Opplysningstype<*>> = RegelverkDagpenger.produserer

    init {
        MessageMediator(
            rapidsConnection = rapidsConnection,
            personMediator = personMediator,
            hendelseRepository = PostgresHendelseRepository(),
            opplysningstyper = opplysningstyper,
        )

        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        if (config["CLEAN_ON_STARTUP"] == "true") clean()
        runMigration()
        outbox.start()
        logger.info { "Starter opp dp-behandling" }
    }
}

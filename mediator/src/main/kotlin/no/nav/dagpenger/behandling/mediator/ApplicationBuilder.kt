package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.KafkaRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.clean
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.behandling.konfigurasjon.Configuration.config
import no.nav.dagpenger.behandling.konfigurasjon.støtterInnvilgelse
import no.nav.dagpenger.behandling.mediator.api.ApiMessageContext
import no.nav.dagpenger.behandling.mediator.api.behandlingApi
import no.nav.dagpenger.behandling.mediator.audit.ApiAuditlogg
import no.nav.dagpenger.behandling.mediator.melding.PostgresHendelseRepository
import no.nav.dagpenger.behandling.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(
    config: Map<String, String>,
) : RapidsConnection.StatusListener {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    // TODO: Last alle regler ved startup. Dette må inn i ett register.
    private val opplysningstyper: Set<Opplysningstype<*>> = RegelverkDagpenger.produserer

    private val rapidsConnection: RapidsConnection =
        RapidApplication
            .create(config) { engine, rapidsConnection: KafkaRapid ->
                val aktivitetsloggMediator = AktivitetsloggMediator()

                val personRepository =
                    PersonRepositoryPostgres(
                        BehandlingRepositoryPostgres(
                            OpplysningerRepositoryPostgres(),
                            AvklaringRepositoryPostgres(AvklaringKafkaObservatør(rapidsConnection)),
                        ),
                    )

                val hendelseMediator = HendelseMediator(personRepository)
                engine.application.behandlingApi(
                    personRepository = personRepository,
                    hendelseMediator = hendelseMediator,
                    auditlogg = ApiAuditlogg(aktivitetsloggMediator, rapidsConnection),
                    opplysningstyper = opplysningstyper,
                ) { ident: String -> ApiMessageContext(rapidsConnection, ident) }

                MessageMediator(
                    rapidsConnection = rapidsConnection,
                    hendelseMediator = hendelseMediator,
                    hendelseRepository = PostgresHendelseRepository(),
                    opplysningstyper = opplysningstyper,
                )
            }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        if (config["CLEAN_ON_STARTUP"] == "true") clean()
        runMigration()
        logger.info { "Starter opp dp-behandling. Støtter innvilgelse=$støtterInnvilgelse" }
    }
}

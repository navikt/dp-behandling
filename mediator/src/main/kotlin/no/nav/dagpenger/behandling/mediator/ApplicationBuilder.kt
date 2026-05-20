package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.rapids_and_rivers.KafkaRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationStopped
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.tracer.initializer.SpanContextSupplier
import no.nav.dagpenger.behandling.mediator.api.ApiMessageContext
import no.nav.dagpenger.behandling.mediator.api.behandlingApi
import no.nav.dagpenger.behandling.mediator.api.simuleringApi
import no.nav.dagpenger.behandling.mediator.api.statusPagesConfig
import no.nav.dagpenger.behandling.mediator.audit.ApiAuditlogg
import no.nav.dagpenger.behandling.mediator.db.PostgresDataSourceBuilder
import no.nav.dagpenger.behandling.mediator.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.behandling.mediator.jobber.BehandleMeldekort
import no.nav.dagpenger.behandling.mediator.jobber.SlettFjernetOpplysninger
import no.nav.dagpenger.behandling.mediator.meldekort.MeldekortBehandlingskø
import no.nav.dagpenger.behandling.mediator.melding.PostgresMeldingRepository
import no.nav.dagpenger.behandling.mediator.mottak.ArenaOppgaveMottak
import no.nav.dagpenger.behandling.mediator.mottak.MarkerMeldekortSomBehandletMottak
import no.nav.dagpenger.behandling.mediator.mottak.SakRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.objectMapper
import no.nav.dagpenger.behandling.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.KildeRepository
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.VaktmesterPostgresRepo
import no.nav.dagpenger.behandling.mediator.repository.VentendeMeldekortDings
import no.nav.dagpenger.ferietillegg.FerietilleggRegistrering
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.regel.DagpengerRegistrering
import no.nav.dagpenger.regelverk.RegelverkRegistrering
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

internal class ApplicationBuilder(
    config: Map<String, String>,
) : RapidsConnection.StatusListener {
    private companion object {
        private val logger = KotlinLogging.logger { }

        private val meterRegistry =
            PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT,
                PrometheusRegistry.defaultRegistry,
                Clock.SYSTEM,
                SpanContextSupplier.getSpanContext(),
            )
    }

    private val regelverk: List<RegelverkRegistrering> = listOf(DagpengerRegistrering(), FerietilleggRegistrering())

    private val opplysningstyper: Set<Opplysningstype<*>> = regelverk.flatMap { it.opplysningstyper }.toSet()
    private val dataSource = PostgresDataSourceBuilder.dataSource
    private val kildeRepository = KildeRepository(dataSource)
    private val avklaringRepository = AvklaringRepositoryPostgres(dataSource, kildeRepository)
    private val opplysningRepository = OpplysningerRepositoryPostgres(dataSource, kildeRepository)
    private val prosessregister = Prosessregister()

    private val personRepository =
        PersonRepositoryPostgres(
            dataSource,
            BehandlingRepositoryPostgres(
                dataSource,
                opplysningRepository,
                avklaringRepository,
                kildeRepository,
                prosessregister,
            ),
        )

    private val meldekortRepositoryPostgres = MeldekortRepositoryPostgres(dataSource)
    private val ventendeMeldekort = VentendeMeldekortDings(meldekortRepositoryPostgres)

    private val behandlingMetrikker = BehandlingMetrikker()

    private val behovssporer = Behovssporer(dataSource)

    private val hendelseMediator =
        HendelseMediator(
            personRepository,
            meldekortRepositoryPostgres,
            behovMediator = BehovMediator(behovssporer),
            observatører = listOf(ventendeMeldekort, behandlingMetrikker),
        )

    private val postgresMeldingRepository = PostgresMeldingRepository(dataSource)

    private val apiRepositoryPostgres = ApiRepositoryPostgres(postgresMeldingRepository, behovssporer)

    private val rapidsConnection: RapidsConnection =
        RapidApplication.create(
            env = config,
            builder = {
                withKtor { preStopHook, rapid ->
                    naisApp(
                        meterRegistry =
                        meterRegistry,
                        objectMapper = objectMapper,
                        applicationLogger = LoggerFactory.getLogger("ApplicationLogger"),
                        callLogger = LoggerFactory.getLogger("CallLogger"),
                        aliveCheck = rapid::isReady,
                        readyCheck = rapid::isReady,
                        preStopHook = preStopHook::handlePreStopRequest,
                        statusPagesConfig = { statusPagesConfig() },
                    ) {
                        monitor.subscribe(ApplicationStopped) {
                            logger.info { "Forsøker å lukke datasource..." }
                            dataSource.close()
                            logger.info { "Lukket datasource" }
                        }
                        behandlingApi(
                            personRepository = personRepository,
                            hendelseMediator = hendelseMediator,
                            auditlogg = ApiAuditlogg(AktivitetsloggMediator(), rapid),
                            opplysningstyper = opplysningstyper,
                            apiRepositoryPostgres = apiRepositoryPostgres,
                            meldekortRepository = meldekortRepositoryPostgres,
                            meterRegistry,
                        ) { ident: String -> ApiMessageContext(rapid, ident) }
                        simuleringApi()
                    }
                }
            },
        ) { engine, rapidsConnection: KafkaRapid ->
            // Logger bare oppgaver enn så lenge. Bør inn i HendelseMediator
            ArenaOppgaveMottak(rapidsConnection, SakRepositoryPostgres())

            // Vedtak mottak
            MarkerMeldekortSomBehandletMottak(rapidsConnection, meldekortRepositoryPostgres)

            avklaringRepository.registerObserver(
                AvklaringKafkaObservatør(
                    rapidsConnection,
                ),
            )

            MessageMediator(
                rapidsConnection = rapidsConnection,
                hendelseMediator = hendelseMediator,
                meldingRepository = postgresMeldingRepository,
                opplysningstyper = opplysningstyper,
                meldekortRepository = meldekortRepositoryPostgres,
                apiRepositoryPostgres = apiRepositoryPostgres,
                behovssporer = behovssporer,
                personRepository = personRepository,
            ).apply {
                regelverk.forEach { it.registrer(rapidsConnection, this, prosessregister) }
            }

            rapidsConnection.register(
                object : RapidsConnection.StatusListener {
                    override fun onShutdown(rapidsConnection: RapidsConnection) {
                        engine.stop()
                    }
                },
            )
        }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        runMigration()
        opplysningRepository.lagreOpplysningstyper(opplysningstyper)
        logger.info { "Starter opp dp-behandling" }

        // Start jobb som sletter fjernet opplysninger
        SlettFjernetOpplysninger.slettOpplysninger(VaktmesterPostgresRepo())

        // Start meldekortbehandling
        BehandleMeldekort(
            MeldekortBehandlingskø(
                personRepository,
                meldekortRepositoryPostgres,
                rapidsConnection,
            ),
        ).start()
    }
}

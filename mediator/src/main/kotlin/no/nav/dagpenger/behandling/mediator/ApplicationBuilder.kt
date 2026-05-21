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
import no.nav.dagpenger.behandling.mediator.api.simuleringApi
import no.nav.dagpenger.behandling.mediator.api.statusPagesConfig
import no.nav.dagpenger.behandling.mediator.audit.ApiAuditlogg
import no.nav.dagpenger.behandling.mediator.db.PostgresDataSourceBuilder
import no.nav.dagpenger.behandling.mediator.jobber.BehandleMeldekort
import no.nav.dagpenger.behandling.mediator.jobber.SlettFjernetOpplysninger
import no.nav.dagpenger.behandling.mediator.repository.VaktmesterPostgresRepo
import no.nav.dagpenger.ferietillegg.FerietilleggRegistrering
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

    private val postgresDataSourceBuilder = PostgresDataSourceBuilder()
    private val dataSource = postgresDataSourceBuilder.dataSource

    private lateinit var runtime: BehandlingRuntime

    private val rapidsConnection: RapidsConnection =
        RapidApplication.create(
            env = config,
            builder = {
                withKtor { preStopHook, rapid ->
                    runtime =
                        BehandlingRuntime(
                            dataSource = dataSource,
                            rapidsConnection = rapid,
                            auditlogg = ApiAuditlogg(AktivitetsloggMediator(), rapid),
                            regelverk = regelverk,
                        ) { ident: String -> ApiMessageContext(rapid, ident) }

                    naisApp(
                        meterRegistry = meterRegistry,
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
                        runtime.api(this)
                        simuleringApi()
                    }
                }
            },
        ) { engine, rapidsConnection: KafkaRapid ->
            runtime.registrerMottak()

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
        postgresDataSourceBuilder.runMigration()
        runtime.lagreOpplysningstyper()
        logger.info { "Starter opp dp-behandling" }

        SlettFjernetOpplysninger.slettOpplysninger(VaktmesterPostgresRepo(dataSource))

        BehandleMeldekort(
            runtime.meldekortBehandlingskø(rapidsConnection),
        ).start()
    }
}

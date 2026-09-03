package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.Config
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.rapids_and_rivers.KafkaRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.tracer.initializer.SpanContextSupplier
import no.nav.dagpenger.ferietillegg.FerietilleggRegistrering
import no.nav.dagpenger.mediator.api.ApiMessageContext
import no.nav.dagpenger.mediator.api.auth.AuthFactory
import no.nav.dagpenger.mediator.api.simuleringApi
import no.nav.dagpenger.mediator.api.statusPagesConfig
import no.nav.dagpenger.mediator.audit.ApiAuditlogg
import no.nav.dagpenger.mediator.db.PostgresDataSourceBuilder
import no.nav.dagpenger.mediator.jobber.BehandleMeldekort
import no.nav.dagpenger.mediator.jobber.OppdaterMenneskerMetrikk
import no.nav.dagpenger.mediator.jobber.SlettFjernetOpplysninger
import no.nav.dagpenger.mediator.repository.VaktmesterPostgresRepo
import no.nav.dagpenger.regel.DagpengerRegistrering
import no.nav.dagpenger.regelverk.RegelverkRegistrering
import no.nav.dagpenger.utestengning.UtestengningRegistrering
import no.nav.helse.rapids_rivers.RapidApplication
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.record.CompressionType
import java.util.Properties

class AivenConfigCompression(
    val config: AivenConfig = AivenConfig.default,
) : Config by config {
    override fun producerConfig(properties: Properties): Properties =
        config.producerConfig(
            Properties().apply {
                put(ProducerConfig.COMPRESSION_TYPE_CONFIG, CompressionType.ZSTD.name)
                put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "2097152") // 2MB
            },
        )
}

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

    private val regelverk: List<RegelverkRegistrering> =
        listOf(DagpengerRegistrering(), FerietilleggRegistrering(), UtestengningRegistrering())

    private val postgresDataSourceBuilder = PostgresDataSourceBuilder()

    private lateinit var runtime: BehandlingRuntime

    private val rapidsConnection: RapidsConnection =
        RapidApplication.create(
            env = config,
            consumerProducerFactory = ConsumerProducerFactory(AivenConfigCompression()),
            builder = {
                withKtorModule {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                    install(StatusPages) {
                        statusPagesConfig()
                    }
                    runtime.api(this)
                    simuleringApi()
                }
            },
            meterRegistry = meterRegistry,
        ) { engine, rapidsConnection: KafkaRapid ->
            runtime =
                BehandlingRuntime(
                    authFactory = AuthFactory(no.nav.dagpenger.konfigurasjon.Configuration.properties),
                    dbSession = postgresDataSourceBuilder.dbsession,
                    rapidsConnection = rapidsConnection,
                    auditlogg = ApiAuditlogg(AktivitetsloggMediator(), rapidsConnection),
                    regelverk = regelverk,
                ) { ident: String -> ApiMessageContext(rapidsConnection, ident) }

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

        SlettFjernetOpplysninger.slettOpplysninger(VaktmesterPostgresRepo(postgresDataSourceBuilder.dbsession))

        BehandleMeldekort(
            runtime.meldekortBehandlingskø(rapidsConnection),
        ).start()

        OppdaterMenneskerMetrikk.start(runtime.personRepository)
    }
}

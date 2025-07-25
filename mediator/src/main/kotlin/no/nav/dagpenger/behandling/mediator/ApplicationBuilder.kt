package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.rapids_and_rivers.KafkaRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.tracer.initializer.SpanContextSupplier
import mu.KotlinLogging
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.behandling.konfigurasjon.Configuration.config
import no.nav.dagpenger.behandling.mediator.api.ApiMessageContext
import no.nav.dagpenger.behandling.mediator.api.behandlingApi
import no.nav.dagpenger.behandling.mediator.api.statusPagesConfig
import no.nav.dagpenger.behandling.mediator.audit.ApiAuditlogg
import no.nav.dagpenger.behandling.mediator.jobber.BehandleMeldekort
import no.nav.dagpenger.behandling.mediator.jobber.SlettFjernetOpplysninger
import no.nav.dagpenger.behandling.mediator.meldekort.MeldekortBehandlingskø
import no.nav.dagpenger.behandling.mediator.melding.PostgresMeldingRepository
import no.nav.dagpenger.behandling.mediator.mottak.ArenaOppgaveMottak
import no.nav.dagpenger.behandling.mediator.mottak.SakRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.mottak.VedtakFattetMottak
import no.nav.dagpenger.behandling.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.VaktmesterPostgresRepo
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.Manuellprosess
import no.nav.dagpenger.regel.Meldekortprosess
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Søknadsprosess
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import no.nav.helse.rapids_rivers.RapidApplication

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

    // TODO: Last alle regler ved startup. Dette må inn i ett register.
    private val opplysningstyper: Set<Opplysningstype<*>> = RegelverkDagpenger.produserer

    private val avklaringRepository = AvklaringRepositoryPostgres()
    private val opplysningRepository = OpplysningerRepositoryPostgres()

    private val personRepository =
        PersonRepositoryPostgres(
            BehandlingRepositoryPostgres(
                opplysningRepository,
                avklaringRepository,
            ),
        )

    private val hendelseMediator = HendelseMediator(personRepository, MeldekortRepositoryPostgres())

    private val postgresMeldingRepository = PostgresMeldingRepository()

    private val apiRepositoryPostgres = ApiRepositoryPostgres(postgresMeldingRepository)

    private val rapidsConnection: RapidsConnection =
        RapidApplication.create(
            env = config,
            builder = {
                withKtor { preStopHook, rapid ->
                    naisApp(
                        meterRegistry =
                        meterRegistry,
                        objectMapper = objectMapper,
                        applicationLogger = KotlinLogging.logger("ApplicationLogger"),
                        callLogger = KotlinLogging.logger("CallLogger"),
                        aliveCheck = rapid::isReady,
                        readyCheck = rapid::isReady,
                        preStopHook = preStopHook::handlePreStopRequest,
                        statusPagesConfig = { statusPagesConfig() },
                    ) {
                        behandlingApi(
                            personRepository = personRepository,
                            hendelseMediator = hendelseMediator,
                            auditlogg = ApiAuditlogg(AktivitetsloggMediator(), rapid),
                            opplysningstyper = opplysningstyper,
                            apiRepositoryPostgres = apiRepositoryPostgres,
                            meterRegistry,
                        ) { ident: String -> ApiMessageContext(rapid, ident) }
                    }
                }
            },
        ) { engine, rapidsConnection: KafkaRapid ->
            // Logger bare oppgaver enn så lenge. Bør inn i HendelseMediator
            ArenaOppgaveMottak(rapidsConnection, SakRepositoryPostgres())

            // Start jobb som sletter fjernet opplysninger
            SlettFjernetOpplysninger.slettOpplysninger(VaktmesterPostgresRepo())

            // Vedtak mottak
            VedtakFattetMottak(rapidsConnection, MeldekortRepositoryPostgres())

            BehandleMeldekort(
                MeldekortBehandlingskø(
                    personRepository,
                    MeldekortRepositoryPostgres(),
                    rapidsConnection,
                ),
            ).start()

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
                meldekortRepository = MeldekortRepositoryPostgres(),
                apiRepositoryPostgres = apiRepositoryPostgres,
            )

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
        registrerRegelverk(opplysningRepository, opplysningstyper)
        logger.info { "Starter opp dp-behandling" }
    }
}

fun registrerRegelverk(
    opplysningRepository: OpplysningerRepositoryPostgres,
    opplysningstyper: Set<Opplysningstype<*>>,
) {
    Søknadsprosess().registrer()
    Meldekortprosess().registrer()
    Manuellprosess().registrer()
    opplysningRepository
        .lagreOpplysningstyper(
            opplysningstyper + fagsakIdOpplysningstype +
                Beregning.arbeidsdag +
                Beregning.arbeidstimer +
                Beregning.forbruk +
                Beregning.meldt +
                Beregning.meldeperiode +
                Beregning.utbetaling +
                Beregning.terskel +
                Beregning.forbruktEgenandel +
                hendelseTypeOpplysningstype,
        )
}

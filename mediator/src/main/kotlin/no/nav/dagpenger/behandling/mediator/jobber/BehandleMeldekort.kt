package no.nav.dagpenger.behandling.mediator.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.GlobalOpenTelemetry
import no.nav.dagpenger.behandling.mediator.meldekort.MeldekortBehandlingskø
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.minutes

internal class BehandleMeldekort(
    private val meldekortBehandlingskø: MeldekortBehandlingskø,
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
        private val tracer = GlobalOpenTelemetry.getTracer(BehandleMeldekort::class.java.name)
    }

    fun start() {
        fixedRateTimer(
            name = "Behandle meldekort",
            daemon = true,
            initialDelay = 1.minutes.inWholeMilliseconds,
            period = 1.minutes.inWholeMilliseconds,
            action = {
                val span = tracer.spanBuilder("behandle-meldekortkø").startSpan()
                try {
                    span.makeCurrent().use {
                        logger.info { "Starter behandling av meldekortkø" }
                        meldekortBehandlingskø.sendMeldekortTilBehandling()
                    }
                } catch (e: Exception) {
                    span.recordException(e)
                    logger.error(e) { "Behandle meldekort feilet" }
                } finally {
                    span.end()
                }
            },
        )
    }
}

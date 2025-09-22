package no.nav.dagpenger.behandling.mediator.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.meldekort.MeldekortBehandlingskø
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.minutes

internal class BehandleMeldekort(
    private val meldekortBehandlingskø: MeldekortBehandlingskø,
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    @WithSpan
    fun start() {
        fixedRateTimer(
            name = "Behandle meldekort",
            daemon = true,
            initialDelay = 1.minutes.inWholeMilliseconds,
            period = 1.minutes.inWholeMilliseconds,
            action = {
                try {
                    logger.info { "Starter behandling av meldekortkø" }
                    meldekortBehandlingskø.sendMeldekortTilBehandling(System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp")
                } catch (e: Exception) {
                    logger.error(e) { "Behandle meldekort feilet" }
                }
            },
        )
    }
}

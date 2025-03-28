package no.nav.dagpenger.behandling.mediator.jobber

import mu.KotlinLogging
import no.nav.dagpenger.behandling.mediator.meldekort.MeldekortBehandlingskø
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.minutes

internal class BehandleMeldekort(
    private val meldekortBehandlingskø: MeldekortBehandlingskø,
) {
    private val logger = KotlinLogging.logger {}

    fun start() {
        fixedRateTimer(
            name = "Behandle meldekort",
            daemon = true,
            initialDelay = 1.minutes.inWholeMilliseconds,
            period = 1.minutes.inWholeMilliseconds,
            action = {
                try {
                    if (System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp") {
                        meldekortBehandlingskø.sendMeldekortTilBehandling()
                    } else {
                        logger.info { "Behandle meldekort kjører ikke i prod" }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Behandle meldekort feilet" }
                }
            },
        )
    }
}

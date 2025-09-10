package no.nav.dagpenger.behandling.mediator.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.mediator.repository.VaktmesterPostgresRepo
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.minutes

internal object SlettFjernetOpplysninger {
    private val logger = KotlinLogging.logger {}

    fun slettOpplysninger(vaktmesterRepository: VaktmesterPostgresRepo) {
        fixedRateTimer(
            name = "Slett fjernet opplysninger",
            daemon = true,
            initialDelay = randomInitialDelay(),
            period = 10.minutes.inWholeMilliseconds,
            action = {
                try {
                    vaktmesterRepository.slettOpplysninger(antall = 1000)
                } catch (e: Exception) {
                    logger.error(e) { "Slett opplysninger jobben feilet" }
                }
            },
        )
    }
}

private fun randomInitialDelay() = Random.nextInt(1..15).minutes.inWholeMilliseconds

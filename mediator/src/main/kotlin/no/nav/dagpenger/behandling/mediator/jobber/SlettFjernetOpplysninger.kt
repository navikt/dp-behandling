package no.nav.dagpenger.behandling.mediator.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.mediator.repository.VaktmesterPostgresRepo
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal object SlettFjernetOpplysninger {
    private val logger = KotlinLogging.logger {}

    fun slettOpplysninger(vaktmesterRepository: VaktmesterPostgresRepo) {
        fixedRateTimer(
            name = "Slett fjernet opplysninger",
            daemon = true,
            initialDelay = randomInitialDelay(),
            period = 1.hours.inWholeMilliseconds,
            action = {
                try {
                    vaktmesterRepository.slettOpplysninger(antall = 10)
                } catch (e: Exception) {
                    logger.error(e) { "Slett opplysninger jobben feilet" }
                }
            },
        )
    }
}

private fun randomInitialDelay() = Random.nextInt(1..15).minutes.inWholeMilliseconds

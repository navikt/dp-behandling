package no.nav.dagpenger.mediator.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.mediator.BehandlingMetrikker.Companion.antallDagpengepersoner
import no.nav.dagpenger.mediator.repository.PersonRepository
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Oppdaterer `dagpenger_personer_antall`-metrikken jevnlig ved å telle unike personer i databasen,
 * fordelt på om de har rett på dagpenger nå.
 *
 * Kjøres uavhengig i hver pod og teller globalt i databasen, så alle podder vil rapportere
 * (omtrent) samme tall. Bruk `avg by(...)` eller `max by(...)`, ikke `sum(...)`, når metrikken
 * visualiseres. `initialDelay` har litt tilfeldighet for å unngå at alle podder treffer
 * databasen samtidig.
 */
internal object OppdaterMenneskerMetrikk {
    private val logger = KotlinLogging.logger {}
    private const val SYSTEM = "dp-sak"

    fun start(personRepository: PersonRepository) {
        fixedRateTimer(
            name = "Oppdater personer-metrikk",
            daemon = true,
            initialDelay = Random.nextInt(60, 180).seconds.inWholeMilliseconds,
            period = 15.minutes.inWholeMilliseconds,
            action = {
                try {
                    val antallPerStatus = personRepository.tellMenneskerPerRettighetstatus()
                    antallDagpengepersoner.labelValues(SYSTEM, "true").set((antallPerStatus[true] ?: 0L).toDouble())
                    antallDagpengepersoner.labelValues(SYSTEM, "false").set((antallPerStatus[false] ?: 0L).toDouble())
                } catch (e: Exception) {
                    logger.error(e) { "Oppdatering av personer-metrikk feilet" }
                }
            },
        )
    }
}

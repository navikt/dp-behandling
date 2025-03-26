package no.nav.dagpenger.behandling.mediator.api

import kotlinx.coroutines.delay
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Regelverkstype
import java.lang.System.currentTimeMillis
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

suspend fun ventPåBehandling(
    personRepository: PersonRepository,
    behandlingId: UUID,
    block: SjekkerBuilder.() -> Unit,
) {
    val sjekker = SjekkerBuilder().apply(block).build()
    waitForCondition {
        val behandling = hentBehandling(personRepository, behandlingId)
        sjekker.all { it.sjekk(behandling) }
    }
}

private suspend fun waitForCondition(
    timeout: Duration = 15.seconds,
    interval: Duration = 500.milliseconds,
    initialDelay: Duration? = null,
    checkCondition: suspend () -> Boolean,
): Boolean {
    if (initialDelay != null) {
        delay(initialDelay)
    }
    val startTime = currentTimeMillis()
    while (currentTimeMillis() - startTime < timeout.inWholeMilliseconds) {
        if (checkCondition()) return true
        delay(interval) // Wait before trying again
    }
    return false
}

class SjekkerBuilder {
    private val sjekker = mutableListOf<Sjekk>()

    fun sjekkAt(
        beskrivelse: String,
        sjekk: Behandling<Regelverkstype>.() -> Boolean,
    ) {
        sjekker.add(Sjekk(beskrivelse, sjekk))
    }

    internal fun build(): List<Sjekk> = sjekker
}

internal data class Sjekk(
    val beskrivelse: String,
    val sjekk: Behandling<Regelverkstype>.() -> Boolean,
)

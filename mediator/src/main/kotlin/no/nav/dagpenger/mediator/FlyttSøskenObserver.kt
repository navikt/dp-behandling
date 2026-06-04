package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.dagpenger.mediator.repository.PersonRepository
import no.nav.dagpenger.modell.BehandlingObservatør
import no.nav.dagpenger.modell.PersonObservatør
import java.util.UUID

class FlyttSøskenObserver(
    private val personRepository: PersonRepository,
) : PersonObservatør {
    private lateinit var flyttejobb: Flyttejobb

    override fun ferdig(event: BehandlingObservatør.BehandlingFerdig) {
        if (event.basertPåBehandling == null) return

        flyttejobb = Flyttejobb(event.behandlingId, event.ident!!)
    }

    fun ferdigstill(context: MessageContext) {
        if (!::flyttejobb.isInitialized) return
        val søsken = personRepository.finnSøsken(flyttejobb.behandlingId)

        søsken.forEach { behandlingId ->
            context.publish(
                JsonMessage
                    .newMessage(
                        "flytt_behandling",
                        mapOf(
                            "ident" to flyttejobb.ident,
                            "behandlingId" to behandlingId.toString(),
                            "nyBasertPåId" to flyttejobb.behandlingId.toString(),
                        ),
                    ).toJson(),
            )
        }
    }

    private data class Flyttejobb(
        val behandlingId: UUID,
        val ident: String,
    )
}

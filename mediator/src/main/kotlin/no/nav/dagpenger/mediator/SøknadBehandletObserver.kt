package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.modell.PersonObservatør
import no.nav.dagpenger.modell.hendelser.SøknadId
import java.time.LocalDate

/**
 * Lytter på ferdigstilte behandlinger av søknader, og publiserer en tynn "søknad_behandlet"-hendelse
 * med henvisning til søknadId og utfallet av behandlingen (avgjørelse og rettighetsperioder).
 */
internal class SøknadBehandletObserver : PersonObservatør {
    private val meldinger = mutableListOf<Pair<String, JsonMessage>>()

    override fun ferdig(event: BehandlingFerdig) {
        val søknadId = event.behandlingAv.eksternId as? SøknadId ?: return
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingFerdig" }

        meldinger.add(
            ident to
                JsonMessage.newMessage(
                    "søknad_behandlet",
                    mapOf(
                        "ident" to ident,
                        "behandlingId" to event.behandlingId,
                        "søknadId" to søknadId.id,
                        "førteTil" to event.avgjørelse.toString(),
                        "rettighetsperioder" to
                            event.rettighetsperioder.map {
                                mapOf(
                                    "fraOgMed" to it.fraOgMed,
                                    "tilOgMed" to it.tilOgMed.takeUnless { dato -> dato == LocalDate.MAX },
                                    "harRett" to it.harRett,
                                )
                            },
                    ),
                ),
        )
    }

    fun ferdigstill(context: MessageContext) {
        meldinger.forEach { (ident, melding) -> context.publish(ident, melding.toJson()) }
        meldinger.clear()
    }
}

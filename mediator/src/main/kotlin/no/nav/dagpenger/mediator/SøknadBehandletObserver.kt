package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingAvbrutt
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.modell.PersonObservatør
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.SøknadId
import java.time.LocalDate

/**
 * Lytter på ferdigstilte og avbrutte behandlinger av søknader, og publiserer tynne hendelser
 * ("søknad_behandlet"/"søknadsbehandling_avbrutt") med henvisning til søknadId. For ferdigstilte behandlinger
 * inkluderes også utfallet (avgjørelse og rettighetsperioder).
 */
internal class SøknadBehandletObserver : PersonObservatør {
    private val meldinger = mutableListOf<Pair<String, JsonMessage>>()

    override fun ferdig(event: BehandlingFerdig) {
        if (!event.behandlingAv.erSøknad()) return
        val søknadId = event.behandlingAv.eksternId as SøknadId
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingFerdig" }

        meldinger.add(
            ident to
                JsonMessage.newMessage(
                    "søknadsbehandling_ferdig",
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

    override fun avbrutt(event: BehandlingAvbrutt) {
        if (!event.behandlingAv.erSøknad()) return
        val søknadId = event.behandlingAv.eksternId as SøknadId
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingAvbrutt" }

        meldinger.add(
            ident to
                JsonMessage.newMessage(
                    "søknadsbehandling_avbrutt",
                    mapOf(
                        "ident" to ident,
                        "behandlingId" to event.behandlingId,
                        "søknadId" to søknadId.id,
                    ) + (event.årsak?.let { mapOf("årsak" to it) } ?: emptyMap()),
                ),
        )
    }

    fun ferdigstill(context: MessageContext) {
        meldinger.forEach { (ident, melding) -> context.publish(ident, melding.toJson()) }
        meldinger.clear()
    }

    private fun StartHendelse.erSøknad(): Boolean = eksternId is SøknadId
}

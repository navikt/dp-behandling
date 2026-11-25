package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.dagpenger.api.behandling.models.RettighetsperiodeDTO
import no.nav.dagpenger.api.behandling.models.SoknadsbehandlingAvbruttDTO
import no.nav.dagpenger.api.behandling.models.SoknadsbehandlingFerdigDTO
import no.nav.dagpenger.api.behandling.models.SoknadsbehandlingOpprettetDTO
import no.nav.dagpenger.mediator.api.tilAvgjørelseDTO
import no.nav.dagpenger.mediator.api.tilOpprinnelseDTO
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingAvbrutt
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingOpprettet
import no.nav.dagpenger.modell.PersonObservatør
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Rettighetsperiode
import java.time.LocalDate

/**
 * Publiserer tynne hendelser når behandlinger av søknader opprettes, ferdigstilles eller avbrytes.
 * For ferdigstilte behandlinger inkluderes også utfallet (avgjørelse og rettighetsperioder).
 *
 * Meldingene er typede DTOer generert av fabrikt fra `openapi/src/main/resources/behandling-api.yaml`
 * (dokumentert under `webhooks` i speccen).
 */
internal class SøknadBehandletObserver : PersonObservatør {
    private val meldinger = mutableListOf<Pair<String, JsonMessage>>()

    override fun opprettet(event: BehandlingOpprettet) {
        if (!event.hendelse.erSøknad()) return
        val søknadId = event.hendelse.eksternId as SøknadId
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingOpprettet" }

        val dto =
            SoknadsbehandlingOpprettetDTO(
                ident = ident,
                behandlingId = event.behandlingId,
                behandlingsskjedeId = event.behandlingskjedeId,
                søknadId = søknadId.id,
            )
        meldinger.add(ident to toJsonMessage("søknadsbehandling_opprettet", dto))
    }

    override fun ferdig(event: BehandlingFerdig) {
        if (!event.behandlingAv.erSøknad()) return
        val søknadId = event.behandlingAv.eksternId as SøknadId
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingFerdig" }

        val dto =
            SoknadsbehandlingFerdigDTO(
                ident = ident,
                behandlingId = event.behandlingId,
                behandlingsskjedeId = event.behandlingskjedeId,
                søknadId = søknadId.id,
                førteTil = event.avgjørelse.tilAvgjørelseDTO(),
                rettighetsperioder = event.rettighetsperioder.map { it.tilRettighetsperiodeDTO() },
            )
        meldinger.add(ident to toJsonMessage("søknadsbehandling_ferdig", dto))
    }

    override fun avbrutt(event: BehandlingAvbrutt) {
        if (!event.behandlingAv.erSøknad()) return
        val søknadId = event.behandlingAv.eksternId as SøknadId
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingAvbrutt" }

        val dto =
            SoknadsbehandlingAvbruttDTO(
                ident = ident,
                behandlingId = event.behandlingId,
                behandlingsskjedeId = event.behandlingskjedeId,
                søknadId = søknadId.id,
                årsak = event.årsak,
            )
        meldinger.add(ident to toJsonMessage("søknadsbehandling_avbrutt", dto))
    }

    fun ferdigstill(context: MessageContext) {
        meldinger.forEach { (ident, melding) -> context.publish(ident, melding.toJson()) }
        meldinger.clear()
    }

    private fun StartHendelse.erSøknad(): Boolean = eksternId is SøknadId

    private fun Rettighetsperiode.tilRettighetsperiodeDTO() =
        RettighetsperiodeDTO(
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed.takeUnless { it == LocalDate.MAX },
            harRett = harRett,
            opprinnelse = endret.tilOpprinnelseDTO(),
        )
}

package no.nav.dagpenger.behandling.mediator.melding

import java.util.UUID

internal interface MeldingRepository {
    fun lagreMelding(
        melding: Melding,
        ident: String,
        id: UUID,
        toJson: String,
    )

    fun markerSomBehandlet(meldingId: UUID): Int

    fun erBehandlet(meldingId: UUID): Boolean
}

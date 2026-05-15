package no.nav.dagpenger.regelverk.melding

import java.util.UUID

interface MeldingRepository {
    fun lagreMelding(
        melding: Melding,
        ident: String,
        id: UUID,
        toJson: String,
    )

    fun markerSomBehandlet(meldingId: UUID): Int

    fun erBehandlet(meldingId: UUID): Boolean
}

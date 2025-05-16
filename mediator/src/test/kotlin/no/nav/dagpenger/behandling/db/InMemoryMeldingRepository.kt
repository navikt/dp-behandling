package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.behandling.mediator.melding.Melding
import no.nav.dagpenger.behandling.mediator.melding.MeldingRepository
import java.util.UUID

internal class InMemoryMeldingRepository : MeldingRepository {
    private val meldingDb = mutableMapOf<UUID, MeldingDto>()

    override fun lagreMelding(
        melding: Melding,
        ident: String,
        id: UUID,
        toJson: String,
    ) {
        meldingDb[id] = MeldingDto(melding, MeldingStatus.MOTTATT)
    }

    override fun markerSomBehandlet(meldingId: UUID): Int {
        val melding = hentMelding(meldingId)
        melding.status = MeldingStatus.BEHANDLET
        meldingDb[meldingId] = melding
        return 1
    }

    override fun erBehandlet(meldingId: UUID): Boolean = meldingDb[meldingId]?.status == MeldingStatus.BEHANDLET

    private fun hentMelding(id: UUID) =
        (
            meldingDb[id]
                ?: throw IllegalArgumentException("Melding med id $id finnes ikke")
        )

    private data class MeldingDto(
        val kafkaMelding: Melding,
        var status: MeldingStatus,
    )

    private enum class MeldingStatus {
        MOTTATT,
        BEHANDLET,
    }
}

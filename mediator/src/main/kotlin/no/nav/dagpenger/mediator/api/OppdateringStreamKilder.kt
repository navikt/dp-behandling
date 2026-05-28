package no.nav.dagpenger.mediator.api

import no.nav.dagpenger.mediator.api.sse.SseInnslag
import no.nav.dagpenger.mediator.api.sse.SseKilde
import no.nav.dagpenger.mediator.repository.OppdateringRepository
import java.util.UUID

internal fun OppdateringRepository.oppdateringerForPerson(ident: String): SseKilde =
    SseKilde(
        defaultCursor = { sisteIdForIdent(ident) },
        hentInnslag = { cursor ->
            hentForIdent(ident, cursor).map { innslag ->
                SseInnslag(
                    id = innslag.id,
                    event = innslag.type,
                    data = innslag.payload,
                )
            }
        },
    )

internal fun OppdateringRepository.oppdateringerForBehandling(behandlingId: UUID): SseKilde =
    SseKilde(
        defaultCursor = { sisteIdForBehandling(behandlingId) },
        hentInnslag = { cursor ->
            hentForBehandling(behandlingId, cursor).map { innslag ->
                SseInnslag(
                    id = innslag.id,
                    event = innslag.type,
                    data = innslag.payload,
                )
            }
        },
    )

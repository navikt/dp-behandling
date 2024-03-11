package no.nav.dagpenger.behandling.mediator.repository

import no.nav.dagpenger.opplysning.Opplysning
import java.util.UUID

interface OpplysningRepository {
    fun hentOpplysning(opplysningId: UUID): Opplysning<*>?

    fun lagreOpplysning(opplysning: Opplysning<*>)

    fun lagreOpplysninger(opplysninger: List<Opplysning<*>>)
}

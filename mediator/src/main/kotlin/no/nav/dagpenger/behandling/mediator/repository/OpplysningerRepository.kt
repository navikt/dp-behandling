package no.nav.dagpenger.behandling.mediator.repository

import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.util.UUID

interface OpplysningerRepository {
    fun hentOpplysninger(opplysningerId: UUID): Opplysninger?

    fun lagreOpplysninger(opplysninger: Opplysninger)

    fun lagreOpplysninger(
        opplysninger: Opplysninger,
        unitOfWork: UnitOfWork<*>,
    )

    fun lagreOpplysningstyper(opplysningstyper: Collection<Opplysningstype<*>>): List<Int>
}

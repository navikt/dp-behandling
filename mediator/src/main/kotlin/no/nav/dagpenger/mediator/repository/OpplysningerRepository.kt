package no.nav.dagpenger.mediator.repository

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.util.UUID

interface OpplysningerRepository {
    fun hentOpplysninger(opplysningerId: UUID): Opplysninger?

    fun lagreOpplysninger(opplysninger: LesbarOpplysninger)

    fun lagreOpplysninger(
        opplysninger: List<LesbarOpplysninger>,
        unitOfWork: PostgresUnitOfWork,
    )

    fun lagreOpplysningstyper(opplysningstyper: Collection<Opplysningstype<*>>): List<Int>
}

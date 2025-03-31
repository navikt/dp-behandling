package no.nav.dagpenger.behandling.mediator.repository

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import java.util.UUID

interface AvklaringRepository {
    fun lagreAvklaringer(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    )

    fun hentAvklaringer(behandlingId: UUID): List<Avklaring>
}

interface BegrunnelseRepository {
    fun lagreBegrunnelse(
        opplysningId: UUID,
        begrunnelse: String,
    )
}

interface BehandlingRepository :
    AvklaringRepository,
    BegrunnelseRepository {
    fun hentBehandling(behandlingId: UUID): Behandling?

    fun lagre(behandling: Behandling)

    fun lagre(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    )
}

interface PersonRepository : BehandlingRepository {
    fun hent(ident: Ident): Person?

    fun lagre(person: Person)

    fun lagre(
        person: Person,
        unitOfWork: UnitOfWork<*>,
    )

    fun håndter(
        ident: Ident,
        handler: (Person) -> Unit,
    ): Person {
        val person = hent(ident) ?: Person(ident)
        handler(person)
        lagre(person)
        return person
    }
}

interface MeldekortRepository {
    fun lagre(meldekort: Meldekort)

    fun hentUbehandledeMeldekort(ident: Ident): List<Meldekort>

    fun hent(meldekortId: UUID): Meldekort?

    fun behandlingStartet(meldekortId: UUID)

    fun behandlet(meldekortId: Long)
}

interface UnitOfWork<S> {
    fun <T> inTransaction(block: (S) -> T): T

    fun rollback()

    fun commit()
}

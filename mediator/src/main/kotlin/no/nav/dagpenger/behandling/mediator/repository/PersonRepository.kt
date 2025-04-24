package no.nav.dagpenger.behandling.mediator.repository

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.opplysning.TemporalCollection
import java.time.LocalDateTime
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

    fun rettighetstatusFor(ident: Ident): TemporalCollection<Rettighetstatus>
}

interface MeldekortRepository {
    fun lagre(meldekort: Meldekort)

    fun hentUbehandledeMeldekort(): List<Meldekortstatus>

    fun hent(meldekortId: UUID): Meldekort?

    fun behandlingStartet(meldekortId: Long)

    fun markerSomFerdig(meldekortId: Long)

    data class Meldekortstatus(
        val meldekort: Meldekort,
        val påbegynt: LocalDateTime? = null,
        val ferdig: LocalDateTime? = null,
    ) {
        val erPåbegynt get() = påbegynt != null && ferdig == null
        val erFerdig get() = ferdig != null
    }
}

interface UnitOfWork<S> {
    fun <T> inTransaction(block: (S) -> T): T

    fun rollback()

    fun commit()
}

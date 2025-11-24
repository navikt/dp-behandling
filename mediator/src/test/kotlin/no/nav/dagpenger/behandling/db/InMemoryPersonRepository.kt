package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepository
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.mediator.repository.UnitOfWork
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.UtbetalingStatus
import no.nav.dagpenger.opplysning.TemporalCollection
import java.time.LocalDate
import java.util.UUID

class InMemoryPersonRepository :
    PersonRepository,
    BehandlingRepository {
    private val persondb = mutableMapOf<Ident, Person>()

    override fun hent(ident: Ident): Person? = persondb[ident]

    override fun hentBehandling(behandlingId: UUID): Behandling? =
        persondb.values
            .flatMap {
                it.behandlinger()
            }.find { it.behandlingId == behandlingId }

    override fun flyttBehandling(
        behandlingId: UUID,
        nyBasertPåId: UUID?,
    ) {
        TODO("Not yet implemented")
    }

    override fun lagre(behandling: Behandling) {
        // no-op
    }

    override fun lagre(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    ) {
        TODO("Not yet implemented")
    }

    override fun finnBehandlinger(
        tilstand: Behandling.TilstandType,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        block: (Behandling) -> Unit,
    ) {
        TODO("Not yet implemented")
    }

    override fun rettighetstatusFor(ident: Ident): TemporalCollection<Rettighetstatus> {
        TODO("Not yet implemented")
    }

    override fun lagreAvklaringer(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    ) {
        TODO("Not yet implemented")
    }

    override fun hentAvklaringer(behandlingId: UUID): List<Avklaring> {
        TODO("Not yet implemented")
    }

    override fun lagre(person: Person) {
        persondb[person.ident] = person
    }

    override fun lagre(
        person: Person,
        unitOfWork: UnitOfWork<*>,
    ) {
        TODO("Not yet implemented")
    }

    fun reset() {
        persondb.clear()
    }

    override fun lagreBegrunnelse(
        opplysningId: UUID,
        begrunnelse: String,
    ) {
        TODO("Not yet implemented")
    }

    override fun lagreUtbetalingStatus(utbetalingStatus: UtbetalingStatus) {
        TODO("Not yet implemented")
    }

    override fun hentUtbetalingStatus(behandlingId: UUID): UtbetalingStatus.Status {
        TODO("Not yet implemented")
    }
}

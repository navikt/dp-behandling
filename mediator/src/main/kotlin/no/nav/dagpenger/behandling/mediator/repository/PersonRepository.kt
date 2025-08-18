package no.nav.dagpenger.behandling.mediator.repository

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.opplysning.TemporalCollection
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.java

interface AvklaringRepository {
    @WithSpan
    fun lagreAvklaringer(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    )

    @WithSpan
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
    @WithSpan
    fun hentBehandling(
        @SpanAttribute
        behandlingId: UUID,
    ): Behandling?

    @WithSpan
    fun flyttBehandling(
        @SpanAttribute
        behandlingId: UUID,
        nyBasertPåId: UUID? = null,
    )

    @WithSpan
    fun lagre(behandling: Behandling)

    @WithSpan
    fun lagre(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    )
}

interface PersonRepository : BehandlingRepository {
    @WithSpan
    fun hent(ident: Ident): Person?

    @WithSpan
    fun lagre(person: Person)

    @WithSpan
    fun lagre(
        person: Person,
        unitOfWork: UnitOfWork<*>,
    )

    @WithSpan
    fun håndter(
        ident: Ident,
        handler: (Person) -> Unit,
    ): Person {
        val person = hent(ident) ?: Person(ident)
        medSpan("personHandler") {
            handler(person)
        }
        lagre(person)
        return person
    }

    fun rettighetstatusFor(ident: Ident): TemporalCollection<Rettighetstatus>

    private fun medSpan(
        string: String,
        block: () -> Unit,
    ) {
        val handlerSpan = tracer.spanBuilder(string).startSpan()
        try {
            handlerSpan.makeCurrent().use { block() }
        } finally {
            handlerSpan.end()
        }
    }

    private companion object {
        private val tracer = GlobalOpenTelemetry.getTracer(PersonRepository::class.java.name)
    }
}

interface MeldekortRepository {
    fun lagre(meldekort: Meldekort)

    fun hentMeldekortkø(): Meldekortkø

    fun hent(meldekortId: UUID): Meldekort?

    fun behandlingStartet(meldekortId: MeldekortId)

    fun markerSomFerdig(meldekortId: MeldekortId)

    data class Meldekortkø(
        val behandlingsklare: List<Meldekortstatus>,
        val underBehandling: List<Meldekortstatus>,
    )

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

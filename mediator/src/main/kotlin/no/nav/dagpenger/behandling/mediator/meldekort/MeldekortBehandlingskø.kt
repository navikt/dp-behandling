package no.nav.dagpenger.behandling.mediator.meldekort

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.withLoggingContext
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.db.medLås
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.TemporalCollection

class MeldekortBehandlingskø(
    private val meldekortRepository: MeldekortRepository,
    private val rapidsConnection: RapidsConnection,
) {
    companion object {
        private val låseNøkkel = 98769876
        private val logger = mu.KotlinLogging.logger {}
    }

    fun sendMeldekortTilBehandling() {
        // TODO: Ganske naiv førsteutgave av meldekort beregningskø.
        // Hente personer som har rettighet og har meldekort som ikke er behandlet
        // For hver meldekort, send meldekort til behandling

        sessionOf(dataSource)
            .use { session ->
                session.transaction { tx ->
                    tx.medLås(låseNøkkel) {
                        val rettighetstatuser: List<Pair<String, Rettighetstatus>> =
                            tx.run(
                                queryOf(
                                    // language=PostgreSQL
                                    """
                                    SELECT * FROM rettighetstatus        
                                    """.trimIndent(),
                                ).map { row ->
                                    val ident = row.string("ident")
                                    val virkningsdato = row.localDate("virkningsdato")
                                    val rettighet = row.boolean("har_rettighet")
                                    val behandlingId = row.uuid("behandling_id")
                                    Pair(ident, Rettighetstatus(virkningsdato, rettighet, behandlingId))
                                }.asList,
                            )

                        val personer: List<Person> =
                            rettighetstatuser.groupBy { it.first }.map { (ident, statuses) ->
                                val tempRettighetstatuser =
                                    TemporalCollection<Rettighetstatus>().apply {
                                        statuses.forEach { put(it.second.virkningsdato, it.second) }
                                    }
                                Person(ident, tempRettighetstatuser)
                            }
                        personer.forEach { person ->
                            val melderkort = meldekortRepository.hentUbehandledeMeldekort(person.ident.tilPersonIdentfikator())
                            melderkort.forEach { meldekort ->
                                val meldekortPeriode = meldekort.periode()

                                val skalBehandles =
                                    meldekortPeriode.any { dag ->
                                        person.rettighetstatuser.get(dag).utfall
                                    }

                                if (skalBehandles) {
                                    withLoggingContext(
                                        "meldekortId" to meldekort.id.toString(),
                                    ) {
                                        logger.info { "Publiserer beregn meldekort" }
                                        rapidsConnection.publish(
                                            meldekort.ident,
                                            JsonMessage
                                                .newMessage(
                                                    "beregn_meldekort",
                                                    mapOf(
                                                        "meldekortId" to meldekort.id,
                                                        "ident" to meldekort.ident,
                                                    ),
                                                ).toJson(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}

data class Person(
    val ident: String,
    val rettighetstatuser: TemporalCollection<Rettighetstatus> = TemporalCollection<Rettighetstatus>(),
)

package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import org.postgresql.util.PGobject
import java.util.UUID

class MeldekortRepositoryPostgres : MeldekortRepository {
    override fun lagre(meldekort: Meldekort) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagreMeldekort(meldekort)

                meldekort.dager.forEach { dag ->
                    tx.lagreMeldekortDager(meldekort, dag)
                }
            }
        }
    }

    override fun hent(meldekortId: UUID): MeldekortInnsendtHendelse? {
        TODO("Not yet implemented")
    }

    private fun TransactionalSession.lagreMeldekortDager(
        meldekort: Meldekort,
        dag: Dag,
    ) {
        run(
            queryOf(
                //language=PostgreSQL
                """
                INSERT INTO meldekort_dag (meldekort_id, meldt, dato) 
                VALUES (:meldekortId, :meldt, :dato)
                """.trimIndent(),
                mapOf(
                    "meldekortId" to meldekort.eksternMeldekortId,
                    "meldt" to dag.meldt,
                    "dato" to dag.dato,
                ),
            ).asUpdate,
        ).also {
            dag.aktiviteter.forEach { aktivitet ->
                this.lagreAktiviteter(meldekort, dag, aktivitet)
            }
        }
    }

    private fun TransactionalSession.lagreAktiviteter(
        meldekort: Meldekort,
        dag: Dag,
        aktivitet: MeldekortAktivitet,
    ) {
        run(
            queryOf(
                //language=PostgreSQL
                """
                INSERT INTO meldekort_aktivitet (meldekort_id, dato, type, timer) 
                VALUES (:meldekortId, :dato, :type, :timer)
                """.trimIndent(),
                mapOf(
                    "meldekortId" to meldekort.eksternMeldekortId,
                    "dato" to dag.dato,
                    "type" to aktivitet.type.name,
                    "timer" to
                        aktivitet.timer?.let { timer ->
                            PGobject().apply {
                                type = "interval"
                                value = timer.toString()
                            }
                        },
                ),
            ).asUpdate,
        )
    }
}

private fun TransactionalSession.lagreMeldekort(meldekort: Meldekort) {
    run(
        queryOf(
            //language=PostgreSQL
            """
            INSERT INTO meldekort (id, ident, meldekort_id, meldingsreferanse_id, korrigert_meldekort_id, innsendt_tidspunkt, fom, tom, kilde_ident, kilde_rolle)
            VALUES (:id, :ident, :meldekortId, :meldingReferanseId, :korrigertMeldekortId,  :innsendtTidspunkt, :fom, :tom, :kildeIdent, :kildeRolle)
            """.trimIndent(),
            mapOf(
                "id" to meldekort.id,
                "meldingReferanseId" to meldekort.meldingsreferanseId,
                "meldekortId" to meldekort.eksternMeldekortId,
                "ident" to meldekort.ident,
                "fom" to meldekort.fom,
                "tom" to meldekort.tom,
                "korrigertMeldekortId" to meldekort.korrigeringAv,
                "kildeIdent" to meldekort.kilde.ident,
                "kildeRolle" to meldekort.kilde.rolle,
                "innsendtTidspunkt" to meldekort.innsendtTidspunkt,
            ),
        ).asUpdate,
    )
}

package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import org.postgresql.util.PGobject

class MeldekortRepositoryPostgres : MeldekortRepository {
    override fun lagre(meldekortInnsendtHendelse: MeldekortInnsendtHendelse) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagreMeldekort(meldekortInnsendtHendelse)

                meldekortInnsendtHendelse.dager.forEach { dag ->
                    tx.lagreMeldekortDager(meldekortInnsendtHendelse, dag)
                }
            }
        }
    }

    private fun TransactionalSession.lagreMeldekortDager(
        meldekortInnsendtHendelse: MeldekortInnsendtHendelse,
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
                    "meldekortId" to meldekortInnsendtHendelse.meldekortId,
                    "meldt" to dag.meldt,
                    "dato" to dag.dato,
                ),
            ).asUpdate,
        ).also {
            dag.aktiviteter.forEach { aktivitet ->
                this.lagreAktiviteter(meldekortInnsendtHendelse, dag, aktivitet)
            }
        }
    }

    private fun TransactionalSession.lagreAktiviteter(
        meldekortInnsendtHendelse: MeldekortInnsendtHendelse,
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
                    "meldekortId" to meldekortInnsendtHendelse.meldekortId,
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

private fun TransactionalSession.lagreMeldekort(meldekortInnsendtHendelse: MeldekortInnsendtHendelse) {
    run(
        queryOf(
            //language=PostgreSQL
            """
            INSERT INTO meldekort_hendelse (id, ident, meldekort_id, meldingsreferanse_id, korrigert_meldekort_id, innsendt_tidspunkt, fom, tom, kilde_ident, kilde_rolle, opprettet)
            VALUES (:id, :ident, :meldekortId, :meldingReferanseId, :korrigertMeldekortId,  :innsendtTidspunkt, :fom, :tom, :kildeIdent, :kildeRolle, :opprettet)
            """.trimIndent(),
            mapOf(
                "id" to meldekortInnsendtHendelse.id,
                "meldingReferanseId" to meldekortInnsendtHendelse.meldingsreferanseId(),
                "meldekortId" to meldekortInnsendtHendelse.meldekortId,
                "ident" to meldekortInnsendtHendelse.ident(),
                "fom" to meldekortInnsendtHendelse.fom,
                "tom" to meldekortInnsendtHendelse.tom,
                "korrigertMeldekortId" to meldekortInnsendtHendelse.korrigeringAv,
                "kildeIdent" to meldekortInnsendtHendelse.kilde.ident,
                "kildeRolle" to meldekortInnsendtHendelse.kilde.rolle,
                "opprettet" to meldekortInnsendtHendelse.opprettet,
                "innsendtTidspunkt" to meldekortInnsendtHendelse.innsendtTidspunkt,
            ),
        ).asUpdate,
    )
}

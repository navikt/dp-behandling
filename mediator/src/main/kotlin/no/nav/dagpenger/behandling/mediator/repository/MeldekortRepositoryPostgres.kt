package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

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

    override fun hent(meldekortId: UUID) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT * FROM meldekort WHERE id = :meldekortId
                    """.trimIndent(),
                    mapOf(
                        "meldekortId" to meldekortId,
                    ),
                ).map {
                    Meldekort(
                        id = it.uuid("id"),
                        meldingsreferanseId = it.uuid("meldingsreferanse_id"),
                        ident = it.string("ident"),
                        eksternMeldekortId = it.long("meldekort_id"),
                        fom = it.localDate("fom"),
                        tom = it.localDate("tom"),
                        kilde =
                            MeldekortKilde(
                                rolle = it.string("kilde_rolle"),
                                ident = it.string("kilde_ident"),
                            ),
                        dager = session.hentDager(it.long("meldekort_id")),
                        innsendtTidspunkt = it.localDateTime("innsendt_tidspunkt"),
                        korrigeringAv = it.longOrNull("korrigert_meldekort_id"),
                    )
                }.asSingle,
            )
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

    private fun Session.hentDager(meldkortId: Long): List<Dag> =
        run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT * FROM meldekort_dag WHERE meldekort_id = :meldekortId
                """.trimIndent(),
                mapOf(
                    "meldekortId" to meldkortId,
                ),
            ).map {
                Dag(
                    dato = it.localDate("dato"),
                    meldt = it.boolean("meldt"),
                    aktiviteter = this.hentAktiviteter(meldkortId, it.localDate("dato")),
                )
            }.asList,
        )

    private fun Session.hentAktiviteter(
        meldkortId: Long,
        localDate: LocalDate,
    ): List<MeldekortAktivitet> =
        run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT dato, type, EXTRACT(epoch FROM timer) AS timer FROM meldekort_aktivitet WHERE meldekort_id = :meldekortId AND dato = :dato
                """.trimIndent(),
                mapOf(
                    "meldekortId" to meldkortId,
                    "dato" to localDate,
                ),
            ).map {
                MeldekortAktivitet(
                    type = AktivitetType.valueOf(it.string("type")),
                    timer = it.intOrNull("timer")?.seconds,
                )
            }.asList,
        )
}

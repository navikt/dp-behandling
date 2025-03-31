package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.LocalDateTime
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

    override fun hentUbehandledeMeldekort(ident: Ident): List<Meldekort> =
        sessionOf(dataSource).use { session ->
            session.run(
                //language=PostgreSQL
                queryOf(
                    """
                    SELECT *
                    FROM meldekort
                    WHERE behandling_startet IS NULL
                    """.trimIndent(),
                ).map { row ->
                    row.meldekort(session)
                }.asList,
            )
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
                ).map { row ->
                    row.meldekort(session)
                }.asSingle,
            )
        }

    override fun behandlingStartet(meldekortId: UUID) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        UPDATE meldekort SET behandling_startet = :startet WHERE id = :meldekortId
                        """.trimIndent(),
                        mapOf(
                            "meldekortId" to meldekortId,
                            "startet" to LocalDateTime.now(),
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun behandlet(meldekortId: Long) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        UPDATE meldekort SET behandling_ferdig = :startet WHERE meldekort_id = :meldekortId
                        """.trimIndent(),
                        mapOf(
                            "meldekortId" to meldekortId,
                            "startet" to LocalDateTime.now(),
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    private fun Row.meldekort(session: Session): Meldekort =
        Meldekort(
            id = uuid("id"),
            meldingsreferanseId = uuid("meldingsreferanse_id"),
            ident = string("ident"),
            eksternMeldekortId = long("meldekort_id"),
            fom = localDate("fom"),
            tom = localDate("tom"),
            kilde =
                MeldekortKilde(
                    rolle = string("kilde_rolle"),
                    ident = string("kilde_ident"),
                ),
            dager = session.hentDager(long("meldekort_id")),
            innsendtTidspunkt = localDateTime("innsendt_tidspunkt"),
            korrigeringAv = longOrNull("korrigert_meldekort_id"),
        )

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
                SELECT dato, type, EXTRACT(EPOCH FROM timer) AS timer FROM meldekort_aktivitet WHERE meldekort_id = :meldekortId AND dato = :dato
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

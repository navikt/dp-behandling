package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository.Meldekortkø
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository.Meldekortstatus
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
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

                meldekort.korrigeringAv?.let { korrigertMeldekortId ->
                    tx.markerSomKorrigert(
                        korrigertAvMeldekortId = meldekort.eksternMeldekortId,
                        originaltMeldekortId = korrigertMeldekortId,
                    )
                }

                meldekort.dager.forEach { dag ->
                    tx.lagreMeldekortDager(meldekort, dag)
                }
            }
        }
    }

    override fun hentMeldekortkø(): Meldekortkø {
        val meldekort =
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT DISTINCT ON (ident) *
                        FROM meldekort
                        WHERE behandling_ferdig IS NULL
                        AND korrigert_av_meldekort_id IS NULL
                        ORDER BY ident, fom, løpenummer DESC;
                        """.trimIndent(),
                    ).map { row ->
                        Meldekortstatus(
                            row.meldekort(session),
                            row.localDateTimeOrNull("behandling_startet"),
                            row.localDateTimeOrNull("behandling_ferdig"),
                        )
                    }.asList,
                )
            }

        val (påbegynt, behandlingsklar) = meldekort.partition { it.erPåbegynt }
        return Meldekortkø(behandlingsklar, påbegynt)
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

    override fun behandlingStartet(meldekortId: MeldekortId) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        UPDATE meldekort SET behandling_startet = :startet WHERE meldekort_id= :meldekortId
                        """.trimIndent(),
                        mapOf(
                            "meldekortId" to meldekortId.id,
                            "startet" to LocalDateTime.now(),
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun markerSomFerdig(meldekortId: MeldekortId) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        UPDATE meldekort SET behandling_ferdig = :ferdig WHERE meldekort_id = :meldekortId
                        """.trimIndent(),
                        mapOf(
                            "meldekortId" to meldekortId.id,
                            "ferdig" to LocalDateTime.now(),
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
            eksternMeldekortId = MeldekortId(string("meldekort_id")),
            fom = localDate("fom"),
            tom = localDate("tom"),
            kilde =
                MeldekortKilde(
                    rolle = string("kilde_rolle"),
                    ident = string("kilde_ident"),
                ),
            dager = session.hentDager(MeldekortId(string("meldekort_id"))),
            innsendtTidspunkt = localDateTime("innsendt_tidspunkt"),
            korrigeringAv = stringOrNull("korrigert_meldekort_id")?.let { MeldekortId(it) },
        )

    private fun TransactionalSession.markerSomKorrigert(
        korrigertAvMeldekortId: MeldekortId,
        originaltMeldekortId: MeldekortId,
    ) {
        run(
            queryOf(
                // language=PostgreSQL
                """
                UPDATE meldekort SET korrigert_av_meldekort_id = :korrigertAvMeldekortId WHERE meldekort_id = :originaltMeldekortId
                """.trimIndent(),
                mapOf(
                    "originaltMeldekortId" to originaltMeldekortId.id,
                    "korrigertAvMeldekortId" to korrigertAvMeldekortId.id,
                ),
            ).asUpdate,
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
                    "meldekortId" to meldekort.eksternMeldekortId.id,
                    "ident" to meldekort.ident,
                    "fom" to meldekort.fom,
                    "tom" to meldekort.tom,
                    "korrigertMeldekortId" to meldekort.korrigeringAv?.id,
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
                    "meldekortId" to meldekort.eksternMeldekortId.id,
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
                    "meldekortId" to meldekort.eksternMeldekortId.id,
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

    private fun Session.hentDager(meldkortId: MeldekortId): List<Dag> =
        run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT * FROM meldekort_dag WHERE meldekort_id = :meldekortId
                """.trimIndent(),
                mapOf(
                    "meldekortId" to meldkortId.id,
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
        meldkortId: MeldekortId,
        localDate: LocalDate,
    ): List<MeldekortAktivitet> =
        run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT dato, type, EXTRACT(EPOCH FROM timer) AS timer FROM meldekort_aktivitet WHERE meldekort_id = :meldekortId AND dato = :dato
                """.trimIndent(),
                mapOf(
                    "meldekortId" to meldkortId.id,
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

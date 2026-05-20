package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository.Meldekortkø
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository.Meldekortstatus
import no.nav.dagpenger.behandling.mediator.repository.PostgresUnitOfWork.Companion.transaction
import no.nav.dagpenger.behandling.modell.BehandlingObservatør
import no.nav.dagpenger.behandling.modell.PersonObservatør
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.dato.finnFørsteArbeidsdag
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

class MeldekortRepositoryPostgres(
    private val dataSource: DataSource,
) : MeldekortRepository {
    override fun lagre(meldekort: Meldekort) {
        transaction(dataSource) {
            lagreMeldekort(meldekort)

            meldekort.korrigeringAv?.let { korrigertMeldekortId ->
                markerSomKorrigert(
                    korrigertAvMeldekortId = meldekort.eksternMeldekortId,
                    originaltMeldekortId = korrigertMeldekortId,
                )
            }

            lagreMeldekortDager(meldekort, meldekort.dager)
        }
    }

    override fun hentMeldekortkø(kjøringsdato: LocalDate): Meldekortkø {
        val førsteVirkedag = finnFørsteArbeidsdag(kjøringsdato)

        val meldekort =
            sessionOf(dataSource).use { session ->
                val meldekortUtenDager =
                    session.run(
                        queryOf(
                            //language=PostgreSQL
                            """
                            SELECT DISTINCT ON (ident) *
                            FROM meldekort
                            WHERE behandling_ferdig IS NULL
                              AND korrigert_av_meldekort_id IS NULL
                              AND satt_på_vent IS NULL
                              AND CASE
                                      -- Meldekortet er innsendt etter tilOgMed (altså forsinket) - da skal det behandles umiddelbart
                                      WHEN meldedato > tom THEN TRUE 
                                      -- Første virkedag etter innsending har inntruffet (eller er i dag)
                                      ELSE meldedato <= :kjoringsdato AND :forsteVirkedag <= :kjoringsdato 
                                END IS TRUE
                            ORDER BY ident, fom, løpenummer DESC
                            LIMIT 1000
                            """.trimIndent(),
                            mapOf(
                                "forsteVirkedag" to førsteVirkedag,
                                "kjoringsdato" to kjøringsdato,
                            ),
                        ).map { row ->
                            Triple(
                                row.meldekort(),
                                row.localDateTimeOrNull("behandling_startet"),
                                row.localDateTimeOrNull("behandling_ferdig"),
                            )
                        }.asList,
                    )
                session
                    .medDager(meldekortUtenDager.map { it.first })
                    .mapIndexed { index, it ->
                        Meldekortstatus(
                            meldekort = it,
                            påbegynt = meldekortUtenDager[index].second,
                            ferdig = meldekortUtenDager[index].third,
                        )
                    }
            }

        val (påbegynt, behandlingsklar) = meldekort.partition { it.erPåbegynt }
        return Meldekortkø(behandlingsklar, påbegynt)
    }

    override fun hent(meldekortId: UUID) =
        sessionOf(dataSource).use { session ->
            val meldekortUtenDager =
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
                        row.meldekort()
                    }.asSingle,
                ) ?: return@use null
            session.medDager(listOf(meldekortUtenDager)).single()
        }

    override fun hentKorrigeringer(originale: List<MeldekortId>): List<Meldekort> =
        sessionOf(dataSource).use { session ->
            val meldekortUtenDager =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                SELECT m.* FROM meldekort m 
                INNER JOIN meldekort erstatning ON m.korrigert_av_meldekort_id = erstatning.meldekort_id
                WHERE m.meldekort_id = ANY(:originale) AND m.behandling_startet IS NULL
                """,
                        mapOf("originale" to originale.map { it.id }.toTypedArray()),
                    ).map { row ->
                        row.meldekort()
                    }.asList,
                )
            session.medDager(meldekortUtenDager)
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

    override fun settPåVent(meldekortId: MeldekortId) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        UPDATE meldekort SET satt_på_vent = :sattPaaVent WHERE meldekort_id = :meldekortId
                        """.trimIndent(),
                        mapOf(
                            "meldekortId" to meldekortId.id,
                            "sattPaaVent" to LocalDateTime.now(),
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun sluttMedDerreVentegreieneNåDa(ident: String) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        UPDATE meldekort SET satt_på_vent = NULL WHERE ident = :ident AND satt_på_vent IS NOT NULL
                        """.trimIndent(),
                        mapOf(
                            "ident" to ident,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun harMeldekort(eksternMeldekortId: MeldekortId): Boolean =
        sessionOf(dataSource).use { session ->
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT 1 FROM meldekort WHERE meldekort_id = :meldekortId
                        """.trimIndent(),
                        mapOf(
                            "meldekortId" to eksternMeldekortId.id,
                        ),
                    ).map { true }.asSingle,
                ) == true
        }

    private fun Row.meldekort(): MeldekortRad =
        MeldekortRad(
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
            innsendtTidspunkt = localDateTime("innsendt_tidspunkt"),
            korrigeringAv = stringOrNull("korrigert_meldekort_id")?.let { MeldekortId(it) },
            meldedato = localDate("meldedato"),
            kanSendesFra = localDate("kan_sendes_fra"),
        )

    private fun Session.medDager(meldekort: List<MeldekortRad>): List<Meldekort> {
        val dagerForAlleMeldekort = hentDager(meldekort)

        return meldekort.map { meldekort ->
            val dager = dagerForAlleMeldekort[meldekort.eksternMeldekortId] ?: emptyList()
            Meldekort(
                id = meldekort.id,
                meldingsreferanseId = meldekort.meldingsreferanseId,
                ident = meldekort.ident,
                eksternMeldekortId = meldekort.eksternMeldekortId,
                fom = meldekort.fom,
                tom = meldekort.tom,
                kilde = meldekort.kilde,
                dager = dager,
                innsendtTidspunkt = meldekort.innsendtTidspunkt,
                korrigeringAv = meldekort.korrigeringAv,
                meldedato = meldekort.meldedato,
                kanSendesFra = meldekort.kanSendesFra,
            )
        }
    }

    private fun Session.hentDager(meldekort: List<MeldekortRad>): Map<MeldekortId, List<Dag>> =
        run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT d.meldekort_id, d.dato, d.meldt, a.type, EXTRACT(EPOCH FROM a.timer) AS timer
                FROM meldekort_dag d
                LEFT JOIN meldekort_aktivitet a ON a.meldekort_id = d.meldekort_id AND a.dato = d.dato 
                WHERE d.meldekort_id = ANY(:meldekortId)
                """.trimIndent(),
                mapOf(
                    "meldekortId" to meldekort.map { it.eksternMeldekortId.id }.toTypedArray(),
                ),
            ).map { row ->
                val aktivitetType = row.stringOrNull("type")
                val aktivitet =
                    aktivitetType?.let {
                        MeldekortAktivitet(
                            type = AktivitetType.valueOf(it),
                            timer = row.intOrNull("timer")?.seconds,
                        )
                    }
                DagRad(
                    meldekortId = MeldekortId(row.string("meldekort_id")),
                    dato = row.localDate("dato"),
                    meldt = row.boolean("meldt"),
                    aktivitet = aktivitet,
                )
            }.asList,
        ).grupperPerMeldekort()

    private fun List<DagRad>.grupperPerMeldekort(): Map<MeldekortId, List<Dag>> =
        this
            .groupBy { it.meldekortId }
            .mapValues { (_, dager) -> dager.slåSammenDatoer() }

    private fun List<DagRad>.slåSammenDatoer(): List<Dag> =
        this
            .groupBy { it.dato }
            .map { (dato, rader) ->
                // sjekker at alle dager har samme 'meldt'-verdi da koden antar dette stemmer
                check(rader.all { it.meldt == rader.first().meldt }) { "Forventet at alle dager for $dato har samme 'meldt'-verdi" }
                Dag(
                    dato = dato,
                    meldt = rader.first().meldt,
                    aktiviteter = rader.mapNotNull { it.aktivitet },
                )
            }
}

private fun PostgresUnitOfWork.lagreMeldekort(meldekort: Meldekort) {
    session.run(
        queryOf(
            //language=PostgreSQL
            """
            INSERT INTO meldekort (id, ident, meldekort_id, meldingsreferanse_id, korrigert_meldekort_id, innsendt_tidspunkt, fom, tom, kilde_ident, kilde_rolle, meldedato, kan_sendes_fra)
            VALUES (:id, :ident, :meldekortId, :meldingReferanseId, :korrigertMeldekortId,  :innsendtTidspunkt, :fom, :tom, :kildeIdent, :kildeRolle, :meldedato, :kanSendesFra)
            ON CONFLICT (meldekort_id) DO NOTHING
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
                "meldedato" to meldekort.meldedato,
                "kanSendesFra" to meldekort.kanSendesFra,
            ),
        ).asUpdate,
    )
}

private fun PostgresUnitOfWork.markerSomKorrigert(
    korrigertAvMeldekortId: MeldekortId,
    originaltMeldekortId: MeldekortId,
) {
    session.run(
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

private fun PostgresUnitOfWork.lagreMeldekortDager(
    meldekort: Meldekort,
    dager: List<Dag>,
) {
    session
        .batchPreparedNamedStatement(
            //language=PostgreSQL
            """
            INSERT INTO meldekort_dag (meldekort_id, meldt, dato) 
            VALUES (:meldekortId, :meldt, :dato)
            """.trimIndent(),
            dager.map { dag ->
                mapOf(
                    "meldekortId" to meldekort.eksternMeldekortId.id,
                    "meldt" to dag.meldt,
                    "dato" to dag.dato,
                )
            },
        ).krevAtAntallRaderErNøyaktigLik(dager.size)

    lagreAktiviteter(
        meldekort,
        dager.flatMap { dag ->
            dag.aktiviteter.map { aktivitet ->
                dag to aktivitet
            }
        },
    )
}

private fun PostgresUnitOfWork.lagreAktiviteter(
    meldekort: Meldekort,
    aktiviteter: List<Pair<Dag, MeldekortAktivitet>>,
) {
    session
        .batchPreparedNamedStatement(
            //language=PostgreSQL
            """
            INSERT INTO meldekort_aktivitet (meldekort_id, dato, type, timer) 
            VALUES (:meldekortId, :dato, :type, :timer)
            """.trimIndent(),
            aktiviteter.map { (dag, aktivitet) ->
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
                )
            },
        ).krevAtAntallRaderErNøyaktigLik(aktiviteter.size)
}

private data class MeldekortRad(
    val id: UUID,
    val meldingsreferanseId: UUID,
    val ident: String,
    val eksternMeldekortId: MeldekortId,
    val fom: LocalDate,
    val tom: LocalDate,
    val kilde: MeldekortKilde,
    val innsendtTidspunkt: LocalDateTime,
    val korrigeringAv: MeldekortId?,
    val meldedato: LocalDate,
    val kanSendesFra: LocalDate,
)

private data class DagRad(
    val meldekortId: MeldekortId,
    val dato: LocalDate,
    val meldt: Boolean,
    val aktivitet: MeldekortAktivitet?,
)

class VentendeMeldekortDings(
    private val meldekortRepository: MeldekortRepository,
) : PersonObservatør {
    override fun ferdig(event: BehandlingObservatør.BehandlingFerdig) {
        if (event.rettighetsperioder.filter { it.endret }.none { it.harRett }) return

        // Om det finnes noen rettighetsperioder som har rett, så kan vi prøve å behandle meldekort på nytt
        meldekortRepository.sluttMedDerreVentegreieneNåDa(event.ident!!)
    }
}

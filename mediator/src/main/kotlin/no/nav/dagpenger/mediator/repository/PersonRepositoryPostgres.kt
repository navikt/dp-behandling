package no.nav.dagpenger.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.prometheus.metrics.model.snapshots.Labels
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.dagpenger.mediator.Metrikk
import no.nav.dagpenger.mediator.Metrikk.hentPersonTimer
import no.nav.dagpenger.mediator.Metrikk.lagrePersonMetrikk
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.Person
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.Rettighetstidslinje
import no.nav.dagpenger.opplysning.RegelverkIdent
import org.postgresql.util.PGobject
import java.time.LocalDate
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class PersonRepositoryPostgres(
    private val dbSession: DatabaseSession,
    private val behandlingRepository: BehandlingRepository,
) : PersonRepository,
    BehandlingRepository by behandlingRepository {
    private companion object {
        val logger = KotlinLogging.logger { }
    }

    @WithSpan
    override fun hent(ident: Ident) =
        dbSession.session { session ->
            val timer = TimeSource.Monotonic.markNow()
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT * FROM person WHERE ident = :ident FOR UPDATE
                        """.trimIndent(),
                        mapOf("ident" to ident.identifikator()),
                    ).map { row ->
                        val dbIdent = Ident(row.string("ident"))
                        val rettighetstidslinje = session.rettighetstidslinjeFor(dbIdent)
                        val behandlingskjeder = behandlingRepository.hentBehandlinger(dbIdent)
                        logger.info {
                            "Hentet person med ${behandlingskjeder.size} behandlingskjede(r) med ${behandlingskjeder.joinToString {
                                (it.dybde + 1)
                                    .toString()
                            }} behandling(er)"
                        }
                        Metrikk.registrerAntallBehandlinger(behandlingskjeder.size)
                        Person(dbIdent, behandlingskjeder, rettighetstidslinje)
                    }.asSingle,
                )?.also {
                    val antallBehandlinger = it.behandlinger().size.toString()
                    val metrikk = hentPersonTimer.labelValues(antallBehandlinger)
                    val tidBrukt = timer.elapsedNow()

                    if (tidBrukt.inWholeMilliseconds < 500) {
                        metrikk.observe(tidBrukt.toDouble(DurationUnit.SECONDS))
                    } else {
                        metrikk.observeWithExemplar(
                            tidBrukt.toDouble(DurationUnit.SECONDS),
                            Labels.of("antall_behandlinger", antallBehandlinger),
                        )
                    }
                }
        }

    @WithSpan
    override fun rettighetstatusFor(ident: Ident): Rettighetstidslinje =
        dbSession.session { session -> session.rettighetstidslinjeFor(ident) }

    @WithSpan
    override fun harIdent(ident: Ident): Boolean =
        dbSession.session { session ->
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT 1 FROM person WHERE ident = :ident
                        """.trimIndent(),
                        mapOf("ident" to ident.identifikator()),
                    ).map { row -> row.intOrNull(1) ?: 0 }.asSingle,
                ) == 1
        }

    private fun Session.rettighetstidslinjeFor(ident: Ident): Rettighetstidslinje =
        this
            .run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT * FROM rettighetstatus WHERE ident = :ident ORDER BY id
                    """.trimIndent(),
                    mapOf("ident" to ident.identifikator()),
                ).map { row ->
                    val regelverk = RegelverkIdent(row.string("regelverk"))
                    val status =
                        Rettighetstatus(
                            fraOgMed = row.localDate("fra_og_med"),
                            tilOgMed = row.tilOgMedDato(),
                            harRett = row.boolean("har_rettighet"),
                            behandlingId = row.uuid("behandling_id"),
                            behandlingskjedeId = row.uuid("behandlingskjede_id"),
                            opplysningId = row.uuid("opplysning_id"),
                        )
                    regelverk to status
                }.asList,
            ).groupBy({ it.first }, { it.second })
            .let { Rettighetstidslinje.fraPerioder(it) }

    private fun Row.tilOgMedDato(): LocalDate =
        when (string("til_og_med")) {
            "infinity" -> LocalDate.MAX
            "-infinity" -> LocalDate.MIN
            else -> localDate("til_og_med")
        }

    override fun lagre(person: Person) {
        lagrePersonMetrikk.time {
            dbSession.transaction {
                lagre(person, this)
            }
        }
    }

    override fun lagre(
        person: Person,
        unitOfWork: PostgresUnitOfWork,
    ) {
        unitOfWork.session.run(
            queryOf(
                //language=PostgreSQL
                """
                INSERT INTO person (ident) VALUES (:ident) ON CONFLICT DO NOTHING
                """.trimIndent(),
                mapOf("ident" to person.ident.identifikator()),
            ).asUpdate,
        )

        lagreRettighetshistorikk(unitOfWork, person.ident.identifikator(), person.rettighetstidslinje())

        behandlingRepository.lagre(person.ident, person.behandlinger(), unitOfWork)
    }

    override fun hentIdenterMedRettighetsperioder(år: Int): List<String> =
        dbSession.session { session ->
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT DISTINCT ident
                        FROM rettighetstatus
                        WHERE regelverk = 'Dagpenger'
                          AND har_rettighet = true
                          AND fra_og_med <= :tom
                          AND til_og_med >= :fom
                        """.trimIndent(),
                        mapOf(
                            "fom" to LocalDate.of(år, 1, 1),
                            "tom" to LocalDate.of(år, 12, 31),
                        ),
                    ).map { row ->
                        row.string("ident")
                    }.asList,
                )
        }

    @WithSpan
    override fun tellMenneskerPerRettighetstatus(): Map<Boolean, Long> =
        dbSession.session { session ->
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT har_rett, count(*) AS antall
                        FROM (
                            SELECT DISTINCT rs.ident,
                                   EXISTS (
                                       SELECT 1 FROM rettighetstatus r
                                       WHERE r.ident = rs.ident
                                         AND r.regelverk = 'Dagpenger'
                                         AND r.har_rettighet = true
                                         AND CURRENT_DATE BETWEEN r.fra_og_med AND r.til_og_med
                                   ) AS har_rett
                            FROM rettighetstatus rs
                            WHERE rs.regelverk = 'Dagpenger'
                        ) siste_status
                        GROUP BY har_rett
                        """.trimIndent(),
                    ).map { row ->
                        row.boolean("har_rett") to row.long("antall")
                    }.asList,
                ).toMap()
        }

    private fun lagreRettighetshistorikk(
        unitOfWork: PostgresUnitOfWork,
        ident: String,
        rettighetstidslinje: Rettighetstidslinje,
    ) {
        unitOfWork.session.run(
            queryOf(
                //language=PostgreSQL
                "DELETE FROM rettighetstatus WHERE ident = :ident",
                mapOf("ident" to ident),
            ).asUpdate,
        )

        val params =
            rettighetstidslinje.regelverk().flatMap { regelverk ->
                rettighetstidslinje.perioder(regelverk).map { periode ->
                    mapOf(
                        "ident" to ident,
                        "regelverk" to regelverk.ident,
                        "fraOgMed" to periode.fraOgMed,
                        "tilOgMed" to periode.tilOgMed.tilPostgresqlDato(),
                        "behandlingId" to periode.behandlingId,
                        "harRettighet" to periode.harRett,
                        "behandlingskjedeId" to periode.behandlingskjedeId,
                        "opplysningId" to periode.opplysningId,
                    )
                }
            }

        if (params.isEmpty()) return

        unitOfWork.session
            .batchPreparedNamedStatement(
                //language=PostgreSQL
                """
                INSERT INTO rettighetstatus
                    (ident, regelverk, fra_og_med, til_og_med, har_rettighet, behandling_id, behandlingskjede_id, opplysning_id)
                VALUES
                    (:ident, :regelverk, :fraOgMed, :tilOgMed, :harRettighet, :behandlingId, :behandlingskjedeId, :opplysningId)
                """.trimIndent(),
                params,
            ).krevAtAntallRaderErNøyaktigLik(params.size)
    }

    private fun LocalDate.tilPostgresqlDato(): Any =
        when (this) {
            LocalDate.MAX -> {
                PGobject().apply {
                    type = "date"
                    value = "infinity"
                }
            }

            LocalDate.MIN -> {
                PGobject().apply {
                    type = "date"
                    value = "-infinity"
                }
            }

            else -> {
                this
            }
        }
}

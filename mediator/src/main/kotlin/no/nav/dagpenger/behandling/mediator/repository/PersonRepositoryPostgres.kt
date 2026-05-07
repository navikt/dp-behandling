package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.prometheus.metrics.model.snapshots.Labels
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.mediator.Metrikk
import no.nav.dagpenger.behandling.mediator.Metrikk.hentPersonTimer
import no.nav.dagpenger.behandling.mediator.Metrikk.lagrePersonMetrikk
import no.nav.dagpenger.behandling.mediator.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.TemporalCollection
import java.time.LocalDate
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class PersonRepositoryPostgres(
    private val behandlingRepository: BehandlingRepository,
) : PersonRepository,
    BehandlingRepository by behandlingRepository {
    private companion object {
        val logger = KotlinLogging.logger { }
    }

    @WithSpan
    override fun hent(ident: Ident) =
        sessionOf(dataSource).use { session ->
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
                        val rettighetstatuser = session.rettighetstatusFor(dbIdent)
                        val behandlinger = behandlingRepository.hentBehandlinger(dbIdent)
                        logger.info { "Hentet person med ${behandlinger.size} behandlinger" }
                        Metrikk.registrerAntallBehandlinger(behandlinger.size)
                        Person(dbIdent, behandlinger, rettighetstatuser)
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
    override fun rettighetstatusFor(ident: Ident): TemporalCollection<Rettighetstatus> =
        sessionOf(dataSource).use { session -> session.rettighetstatusFor(ident) }

    @WithSpan
    override fun harIdent(ident: Ident): Boolean =
        sessionOf(dataSource).use { session ->
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

    private fun Session.rettighetstatusFor(ident: Ident): TemporalCollection<Rettighetstatus> =
        this
            .run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT * FROM rettighetstatus WHERE ident = :ident ORDER BY opprettet
                    """.trimIndent(),
                    mapOf("ident" to ident.identifikator()),
                ).map { row ->
                    val gjelderFra = row.localDate("gjelder_fra")
                    val virkningsdato = row.localDate("virkningsdato")
                    val utfall = row.boolean("har_rettighet")
                    val behandlingId = row.uuid("behandling_id")
                    val behandlingskjedeId = row.uuid("behandlingskjede_id")
                    Pair(gjelderFra, Rettighetstatus(virkningsdato, utfall, behandlingId, behandlingskjedeId))
                }.asList,
            ).let {
                TemporalCollection<Rettighetstatus>().apply {
                    it.forEach { pair -> put(pair.first, pair.second) }
                }
            }

    override fun lagre(person: Person) {
        lagrePersonMetrikk.time {
            PostgresUnitOfWork.transaction {
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
        unitOfWork.session.run(
            queryOf(
                //language=PostgreSQL
                "DELETE FROM rettighetstatus WHERE ident = :ident",
                mapOf("ident" to person.ident.identifikator()),
            ).asUpdate,
        )

        lagreRettighetshistorikk(unitOfWork, person.ident.identifikator(), person.rettighethistorikk())

        behandlingRepository.lagre(person.ident, person.behandlinger(), unitOfWork)
    }

    override fun hentIdenterMedRettighetsperioder(år: Int): List<String> =
        sessionOf(dataSource).use { session ->
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        WITH åretFør AS (
                            SELECT DISTINCT ON (ident) ident, har_rettighet
                            FROM rettighetstatus
                            WHERE gjelder_fra < :fom
                            ORDER BY ident, gjelder_fra DESC
                        )
                        SELECT DISTINCT ident
                        FROM rettighetstatus
                        WHERE har_rettighet = true
                          AND gjelder_fra BETWEEN :fom AND :tom
                        union
                        select ident
                        from åretFør
                        where har_rettighet = true
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

    private fun lagreRettighetshistorikk(
        unitOfWork: PostgresUnitOfWork,
        ident: String,
        rettighethistorikk: Map<LocalDate, Rettighetstatus>,
    ) {
        val params =
            rettighethistorikk
                .map { (gjelderFra, rettighetstatus) ->
                    mapOf(
                        "ident" to ident,
                        "gjelderFra" to gjelderFra,
                        "virkningsdato" to rettighetstatus.virkningsdato,
                        "behandlingId" to rettighetstatus.behandlingId,
                        "harRettighet" to rettighetstatus.utfall,
                        "behandlingskjedeId" to rettighetstatus.behandlingskjedeId,
                    )
                }

        unitOfWork.session
            .batchPreparedNamedStatement(
                //language=PostgreSQL
                """
                INSERT INTO rettighetstatus (ident, gjelder_fra, virkningsdato, har_rettighet, behandling_id, behandlingskjede_id)
                VALUES (:ident, :gjelderFra, :virkningsdato, :harRettighet, :behandlingId, :behandlingskjedeId)
                """.trimIndent(),
                params,
            ).krevAtAntallRaderErNøyaktigLik(params.size)
    }
}

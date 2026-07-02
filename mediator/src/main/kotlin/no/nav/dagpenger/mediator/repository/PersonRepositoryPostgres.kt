package no.nav.dagpenger.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.prometheus.metrics.model.snapshots.Labels
import kotliquery.Session
import kotliquery.queryOf
import no.nav.dagpenger.mediator.Metrikk
import no.nav.dagpenger.mediator.Metrikk.hentPersonTimer
import no.nav.dagpenger.mediator.Metrikk.lagrePersonMetrikk
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.Person
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.Utestengningsperiode
import no.nav.dagpenger.opplysning.TemporalCollection
import java.time.LocalDate
import java.util.UUID
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
                        SELECT p.ident, (
                            SELECT array_agg(pi_all.ident)
                            FROM person_ident pi_all
                            WHERE pi_all.person_id = p.person_id
                        ) AS alle_identer
                        FROM person p
                        INNER JOIN person_ident pi ON pi.ident = :ident AND pi.person_id = p.person_id
                        FOR UPDATE OF p
                        """.trimIndent(),
                        mapOf("ident" to ident.identifikator()),
                    ).map { row ->
                        val kanoniskIdent = row.string("ident")
                        val alleIdenter = row.array<String>("alle_identer").toList()
                        val aliaser = alleIdenter.filter { it != kanoniskIdent }
                        val dbIdent = Ident(kanoniskIdent, aliaser)
                        val rettighetstatuser = session.rettighetstatusFor(dbIdent)
                        val utestengninger = session.utestengningerFor(dbIdent)
                        val behandlinger = behandlingRepository.hentBehandlinger(dbIdent)
                        logger.info { "Hentet person med ${behandlinger.size} behandlinger" }
                        Metrikk.registrerAntallBehandlinger(behandlinger.size)
                        Person(dbIdent, behandlinger, rettighetstatuser, utestengninger)
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
        dbSession.session { session -> session.rettighetstatusFor(ident) }

    @WithSpan
    override fun erUtestengt(
        ident: Ident,
        dato: LocalDate,
    ): Boolean =
        dbSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT 1 FROM utestengning u
                    WHERE u.ident = ANY(
                        SELECT pi.ident FROM person_ident pi
                        WHERE pi.person_id = (
                            SELECT pi2.person_id FROM person_ident pi2 WHERE pi2.ident = :ident
                        )
                    )
                    AND u.fra_og_med <= :dato AND u.til_og_med >= :dato
                    LIMIT 1
                    """.trimIndent(),
                    mapOf("ident" to ident.identifikator(), "dato" to dato),
                ).map { 1 }.asSingle,
            ) == 1
        }

    @WithSpan
    override fun harIdent(ident: Ident): Boolean =
        dbSession.session { session ->
            session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT 1 FROM person_ident WHERE ident = :ident
                        """.trimIndent(),
                        mapOf("ident" to ident.identifikator()),
                    ).map { row -> row.intOrNull(1) ?: 0 }.asSingle,
                ) == 1
        }

    @WithSpan
    override fun merge(
        winner: Ident,
        loser: Ident,
    ) {
        dbSession.transaction {
            val winnerPersonId = session.finnPersonId(winner) ?: throw NotFoundException("Vinner-ident finnes ikke")
            val loserPersonId = session.finnPersonId(loser) ?: throw NotFoundException("Taper-ident finnes ikke")
            if (winnerPersonId == loserPersonId) {
                logger.info { "Merge no-op: $winnerPersonId og $loserPersonId peker allerede på samme person" }
                return@transaction
            }

            val winnerIdent = session.finnKanoniskIdent(winnerPersonId)
            val loserIdenter = session.finnAlleIdenter(loserPersonId)

            logger.info { "Merger person $loserPersonId inn i $winnerPersonId" }

            // Flytt alle loser-identer til winner sin person_id
            session.run(
                queryOf(
                    //language=PostgreSQL
                    "UPDATE person_ident SET person_id = :winner, er_gjeldende = FALSE WHERE person_id = :loser",
                    mapOf("winner" to winnerPersonId, "loser" to loserPersonId),
                ).asUpdate,
            )

            // Flytt alle FK-koblinger fra loser-identer til winner-identen
            loserIdenter.forEach { loserIdentStr ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        INSERT INTO person_behandling (ident, behandling_id)
                        SELECT :winner, behandling_id FROM person_behandling WHERE ident = :loser
                        ON CONFLICT DO NOTHING
                        """.trimIndent(),
                        mapOf("winner" to winnerIdent, "loser" to loserIdentStr),
                    ).asUpdate,
                )
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        "DELETE FROM person_behandling WHERE ident = :loser",
                        mapOf("loser" to loserIdentStr),
                    ).asUpdate,
                )
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        "UPDATE rettighetstatus SET ident = :winner WHERE ident = :loser",
                        mapOf("winner" to winnerIdent, "loser" to loserIdentStr),
                    ).asUpdate,
                )
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        "UPDATE utestengning SET ident = :winner WHERE ident = :loser",
                        mapOf("winner" to winnerIdent, "loser" to loserIdentStr),
                    ).asUpdate,
                )
            }

            // Beholder loser sin person-rad som anker for FK-integritet (meldekort_hendelse etc.)
            // Loser-raden er ikke lenger nåbar via person_ident (alle peker til winner)
        }
    }

    @WithSpan
    override fun split(
        loserIdent: Ident,
        fraIdent: Ident,
    ) {
        dbSession.transaction {
            val fraPersonId = session.finnPersonId(fraIdent) ?: throw NotFoundException("Fra-ident finnes ikke")
            val loserPersonId = session.finnPersonId(loserIdent) ?: throw NotFoundException("Loser-ident finnes ikke")
            if (loserPersonId != fraPersonId) {
                throw BadRequestException("Loser-ident tilhører ikke fra-personen — er de allerede splittet?")
            }
            val fraKanoniskIdent = session.finnKanoniskIdent(fraPersonId)

            // Finn behandlinger initiert av loserIdent (provenance via behandler_hendelse)
            val loserBehandlingIds =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT DISTINCT pb.behandling_id
                        FROM person_behandling pb
                        INNER JOIN behandler_hendelse_behandling bhb ON bhb.behandling_id = pb.behandling_id
                        INNER JOIN behandler_hendelse bh ON bh.melding_id = bhb.melding_id
                        WHERE bh.ident = :loserIdent AND pb.ident = :fraIdent
                        """.trimIndent(),
                        mapOf("loserIdent" to loserIdent.identifikator(), "fraIdent" to fraKanoniskIdent),
                    ).map { row -> row.uuid("behandling_id") }.asList,
                )

            logger.info { "Splitter loser fra person $fraPersonId med ${loserBehandlingIds.size} behandlinger" }

            // Opprett ny person-rad for loser-identen (kan allerede eksistere fra før merge)
            session.run(
                queryOf(
                    //language=PostgreSQL
                    "INSERT INTO person (ident) VALUES (:ident) ON CONFLICT (ident) DO NOTHING",
                    mapOf("ident" to loserIdent.identifikator()),
                ).asUpdate,
            )
            // Les person_id direkte fra person-tabellen — person_ident peker fortsatt til winner
            val nyPersonId =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        "SELECT person_id FROM person WHERE ident = :ident",
                        mapOf("ident" to loserIdent.identifikator()),
                    ).map { row -> row.uuid("person_id") }.asSingle,
                ) ?: error("Kunne ikke opprette person for loser-ident")

            // Flytt loser-identen til sin nye person_id
            session.run(
                queryOf(
                    //language=PostgreSQL
                    "UPDATE person_ident SET person_id = :nyPersonId, er_gjeldende = TRUE WHERE ident = :ident",
                    mapOf("nyPersonId" to nyPersonId, "ident" to loserIdent.identifikator()),
                ).asUpdate,
            )

            if (loserBehandlingIds.isNotEmpty()) {
                val behandlingsArray = loserBehandlingIds.toTypedArray()

                session.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        INSERT INTO person_behandling (ident, behandling_id)
                        SELECT :loser, behandling_id FROM person_behandling
                        WHERE ident = :fra AND behandling_id = ANY(:behandlinger)
                        ON CONFLICT DO NOTHING
                        """.trimIndent(),
                        mapOf("loser" to loserIdent.identifikator(), "fra" to fraKanoniskIdent, "behandlinger" to behandlingsArray),
                    ).asUpdate,
                )
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        "DELETE FROM person_behandling WHERE ident = :fra AND behandling_id = ANY(:behandlinger)",
                        mapOf("fra" to fraKanoniskIdent, "behandlinger" to behandlingsArray),
                    ).asUpdate,
                )
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        "UPDATE rettighetstatus SET ident = :loser WHERE ident = :fra AND behandling_id = ANY(:behandlinger)",
                        mapOf("loser" to loserIdent.identifikator(), "fra" to fraKanoniskIdent, "behandlinger" to behandlingsArray),
                    ).asUpdate,
                )
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        "UPDATE utestengning SET ident = :loser WHERE ident = :fra AND behandling_id = ANY(:behandlinger)",
                        mapOf("loser" to loserIdent.identifikator(), "fra" to fraKanoniskIdent, "behandlinger" to behandlingsArray),
                    ).asUpdate,
                )
            }
        }
    }

    private fun Session.finnPersonId(ident: Ident): UUID? =
        run(
            queryOf(
                //language=PostgreSQL
                "SELECT person_id FROM person_ident WHERE ident = :ident",
                mapOf("ident" to ident.identifikator()),
            ).map { it.uuid("person_id") }.asSingle,
        )

    private fun Session.finnKanoniskIdent(personId: UUID): String =
        run(
            queryOf(
                //language=PostgreSQL
                "SELECT ident FROM person WHERE person_id = :personId",
                mapOf("personId" to personId),
            ).map { it.string("ident") }.asSingle,
        ) ?: error("Fant ingen kanonisk ident for person_id $personId")

    private fun Session.finnAlleIdenter(personId: UUID): List<String> =
        run(
            queryOf(
                //language=PostgreSQL
                "SELECT ident FROM person_ident WHERE person_id = :personId",
                mapOf("personId" to personId),
            ).map { it.string("ident") }.asList,
        )

    // Private hjelpere for aliasoppslag
    //
    // Disse kalles alltid fra hent(), der Ident allerede er lastet med alle aliaser fra DB.
    // De bruker alleIdentifikatorer() direkte — ingen ekstra DB-rundtur.
    //
    // Offentlige metoder (erUtestengt, rettighetstatusFor) kan motta en Ident uten aliaser,
    // og gjør derfor et friskt oppslag via person_ident-subquery.

    private fun Session.rettighetstatusFor(ident: Ident): TemporalCollection<Rettighetstatus> =
        this
            .run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT * FROM rettighetstatus WHERE ident = ANY(:identer) ORDER BY opprettet
                    """.trimIndent(),
                    mapOf("identer" to ident.alleIdentifikatorer().toTypedArray()),
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

    private fun Session.utestengningerFor(ident: Ident): List<Utestengningsperiode> =
        this
            .run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT * FROM utestengning WHERE ident = ANY(:identer) ORDER BY fra_og_med
                    """.trimIndent(),
                    mapOf("identer" to ident.alleIdentifikatorer().toTypedArray()),
                ).map { row ->
                    Utestengningsperiode(
                        fraOgMed = row.localDate("fra_og_med"),
                        tilOgMed = row.localDate("til_og_med"),
                        behandlingId = row.uuid("behandling_id"),
                        behandlingskjedeId = row.uuid("behandlingskjede_id"),
                    )
                }.asList,
            )

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
        // Sørg for at person_ident-raden finnes for nye personer
        unitOfWork.session.run(
            queryOf(
                //language=PostgreSQL
                """
                INSERT INTO person_ident (ident, person_id)
                SELECT :ident, person_id FROM person WHERE ident = :ident
                ON CONFLICT DO NOTHING
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
        lagreUtestengninger(unitOfWork, person.ident.identifikator(), person.utestengninghistorikk())

        behandlingRepository.lagre(person.ident, person.behandlinger(), unitOfWork)
    }

    override fun hentIdenterMedRettighetsperioder(år: Int): List<String> =
        dbSession.session { session ->
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

    private fun lagreUtestengninger(
        unitOfWork: PostgresUnitOfWork,
        ident: String,
        utestengninger: List<Utestengningsperiode>,
    ) {
        if (utestengninger.isEmpty()) return
        val params =
            utestengninger.map {
                mapOf(
                    "ident" to ident,
                    "behandlingId" to it.behandlingId,
                    "behandlingskjedeId" to it.behandlingskjedeId,
                    "fraOgMed" to it.fraOgMed,
                    "tilOgMed" to it.tilOgMed,
                )
            }
        unitOfWork.session.batchPreparedNamedStatement(
            //language=PostgreSQL
            """
            INSERT INTO utestengning (ident, behandling_id, behandlingskjede_id, fra_og_med, til_og_med)
            VALUES (:ident, :behandlingId, :behandlingskjedeId, :fraOgMed, :tilOgMed)
            ON CONFLICT (ident, behandling_id) DO NOTHING
            """.trimIndent(),
            params,
        )
    }
}

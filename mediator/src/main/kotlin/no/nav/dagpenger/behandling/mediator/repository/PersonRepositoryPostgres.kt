package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.Metrikk
import no.nav.dagpenger.behandling.mediator.Metrikk.hentPersonTimer
import no.nav.dagpenger.behandling.mediator.Metrikk.lagrePersonMetrikk
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.TemporalCollection

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
            val timer = hentPersonTimer.startTimer()
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
                        val behandlinger = session.behandlingerFor(dbIdent)
                        logger.info { "Hentet person med ${behandlinger.size} behandlinger" }
                        Metrikk.registrerAntallBehandlinger(behandlinger.size)
                        Person(dbIdent, behandlinger, rettighetstatuser)
                    }.asSingle,
                ).also {
                    timer.observeDuration()
                }
        }

    private fun Session.behandlingerFor(ident: Ident) =
        this.run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT * FROM person_behandling WHERE ident IN (:ident) ORDER BY behandling_id 
                """.trimIndent(),
                mapOf("ident" to ident.alleIdentifikatorer().first()),
            ).map { row ->
                behandlingRepository.hentBehandling(row.uuid("behandling_id"))
            }.asList,
        )

    override fun rettighetstatusFor(ident: Ident): TemporalCollection<Rettighetstatus> =
        sessionOf(dataSource)
            .use { session ->
                session.rettighetstatusFor(ident)
            }

    private fun Session.rettighetstatusFor(ident: Ident): TemporalCollection<Rettighetstatus> =
        this
            .run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT * FROM rettighetstatus WHERE ident = :ident
                    """.trimIndent(),
                    mapOf("ident" to ident.identifikator()),
                ).map { row ->
                    val gjelderFra = row.localDateTime("gjelder_fra")
                    val virkningsdato = row.localDate("virkningsdato")
                    val utfall = row.boolean("har_rettighet")
                    val behandlingId = row.uuid("behandling_id")
                    Pair(gjelderFra, Rettighetstatus(virkningsdato, utfall, behandlingId))
                }.asList,
            ).let {
                TemporalCollection<Rettighetstatus>().apply {
                    it.forEach { pair -> put(pair.first, pair.second) }
                }
            }

    override fun lagre(person: Person) {
        lagrePersonMetrikk.time {
            val unitOfWork = PostgresUnitOfWork.transaction()
            lagre(person, unitOfWork)
            unitOfWork.commit()
        }
    }

    override fun lagre(
        person: Person,
        unitOfWork: UnitOfWork<*>,
    ) = lagre(person, unitOfWork as PostgresUnitOfWork)

    private fun lagre(
        person: Person,
        unitOfWork: PostgresUnitOfWork,
    ) = unitOfWork.inTransaction { tx ->
        tx.run(
            queryOf(
                //language=PostgreSQL
                """
                INSERT INTO person (ident) VALUES (:ident) ON CONFLICT DO NOTHING
                """.trimIndent(),
                mapOf("ident" to person.ident.identifikator()),
            ).asUpdate,
        )
        person.rettighethistorikk().forEach { gjelderFra, rettighetstatus ->
            tx.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO rettighetstatus (ident, gjelder_fra, virkningsdato, har_rettighet, behandling_id)
                    VALUES (:ident, :gjelderFra, :virkningsdato, :harRettighet, :behandlingId)
                    ON CONFLICT (behandling_id) DO NOTHING 
                    """.trimIndent(),
                    mapOf(
                        "ident" to person.ident.identifikator(),
                        "gjelderFra" to gjelderFra,
                        "virkningsdato" to rettighetstatus.virkningsdato,
                        "behandlingId" to rettighetstatus.behandlingId,
                        "harRettighet" to rettighetstatus.utfall,
                    ),
                ).asUpdate,
            )
        }
        person.behandlinger().forEach { behandling ->
            behandlingRepository.lagre(behandling, unitOfWork)

            tx.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO person_behandling (ident, behandling_id)
                    VALUES (:ident, :behandling_id)
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    mapOf(
                        "ident" to person.ident.identifikator(),
                        "behandling_id" to behandling.behandlingId,
                    ),
                ).asUpdate,
            )
        }
    }
}

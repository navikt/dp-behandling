package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.Metrikk.hentBehandlingTimer
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres.Companion.hentOpplysninger
import no.nav.dagpenger.behandling.modell.Arbeidssteg
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.EksternId
import no.nav.dagpenger.behandling.modell.hendelser.Hendelse
import no.nav.dagpenger.behandling.modell.hendelser.UtbetalingStatus
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosessregister.Companion.RegistrertForretningsprosess
import no.nav.dagpenger.opplysning.Saksbehandler
import java.time.LocalDate
import java.util.UUID

internal class BehandlingRepositoryPostgres(
    private val opplysningRepository: OpplysningerRepository,
    private val avklaringRepository: AvklaringRepository,
    private val kildeRepository: KildeRepository = KildeRepository(),
) : BehandlingRepository,
    AvklaringRepository by avklaringRepository {
    override fun hentBehandling(behandlingId: UUID): Behandling? =
        hentBehandlingTimer.time<Behandling?> {
            sessionOf(dataSource).use { session ->
                session.hentBehandling(behandlingId)
            }
        }

    override fun hentBehandlinger(behandlingIder: List<UUID>): List<Behandling> {
        if (behandlingIder.isEmpty()) return emptyList()
        return sessionOf(dataSource).use { session ->
            session.hentBehandlinger(behandlingIder)
        }
    }

    override fun flyttBehandling(
        behandlingId: UUID,
        nyBasertPåId: UUID?,
    ) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    "UPDATE behandling SET basert_på_behandling_id=:basertPaa WHERE behandling_id=:behandlingId",
                    mapOf(
                        "behandlingId" to behandlingId,
                        "basertPaa" to nyBasertPåId,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun Session.hentBehandling(behandlingId: UUID): Behandling? = hentBehandlinger(listOf(behandlingId)).singleOrNull()

    private fun Session.hentBehandlinger(behandlingIder: List<UUID>): List<Behandling> {
        if (behandlingIder.isEmpty()) return emptyList()

        // Finn basertPå-relasjoner for disse IDene
        val alleBehandlingIder =
            this
                .run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        with recursive behandlingkjede as (
                            -- ankerbehandlinger
                            select behandling_id, basert_på_behandling_id
                            from behandling
                            where behandling_id = ANY(:ider)
                            
                            union
                            -- rekursive behandlinger
                            select r.behandling_id, r.basert_på_behandling_id
                            from behandling r
                            join behandlingkjede bk on bk.basert_på_behandling_id = r.behandling_id
                        )
                        
                        select behandling_id, basert_på_behandling_id 
                        from behandlingkjede
                        """.trimIndent(),
                        mapOf("ider" to behandlingIder.toTypedArray()),
                    ).map { row -> row.uuidOrNull("behandling_id") }.asList,
                ).toSet()

        // Hent arbeidssteg for alle behandlinger i én spørring
        val arbeidsstegMap = mutableMapOf<Pair<UUID, Arbeidssteg.Oppgave>, Arbeidssteg>()
        this
            .run(
                queryOf(
                    // language=PostgreSQL
                    """
                    SELECT * FROM behandling_arbeidssteg WHERE behandling_id = ANY(:ider)
                    """.trimIndent(),
                    mapOf("ider" to alleBehandlingIder.toTypedArray()),
                ).map { row ->
                    val behandlingId = row.uuid("behandling_id")
                    val oppgave = Arbeidssteg.Oppgave.valueOf(row.string("oppgave"))
                    val arbeidssteg =
                        Arbeidssteg.rehydrer(
                            Arbeidssteg.TilstandType.valueOf(row.string("tilstand")),
                            oppgave,
                            row.stringOrNull("utført_av")?.let { Saksbehandler(it) },
                            row.localDateTimeOrNull("utført"),
                        )
                    arbeidsstegMap[behandlingId to oppgave] = arbeidssteg
                }.asList,
            )

        // Hent avklaringer for alle behandlinger i én spørring
        val avklaringerMap = hentAvklaringer(alleBehandlingIder)

        // Hent alle behandlinger i én spørring
        data class BehandlingRad(
            val behandlingId: UUID,
            val meldingId: UUID,
            val hendelseType: String,
            val ident: String,
            val eksternIdType: String,
            val eksternId: String,
            val skjedde: java.time.LocalDate,
            val forretningsprosess: String,
            val opprettet: java.time.LocalDateTime,
            val opplysningerId: UUID,
            val tilstand: String,
            val sistEndretTilstand: java.time.LocalDateTime,
            val basertPåBehandlingId: UUID?,
        )

        val behandlingRader =
            this
                .run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        SELECT *  
                        FROM behandling 
                        LEFT JOIN behandler_hendelse_behandling ON behandling.behandling_id = behandler_hendelse_behandling.behandling_id
                        LEFT JOIN behandler_hendelse ON behandler_hendelse.melding_id = behandler_hendelse_behandling.melding_id
                        LEFT JOIN behandling_opplysninger ON behandling.behandling_id = behandling_opplysninger.behandling_id                    
                        WHERE behandling.behandling_id = ANY(:ider) 
                        """.trimIndent(),
                        mapOf("ider" to alleBehandlingIder.toTypedArray()),
                    ).map { row ->
                        BehandlingRad(
                            behandlingId = row.uuid("behandling_id"),
                            meldingId = row.uuid("melding_id"),
                            hendelseType = row.string("hendelse_type"),
                            ident = row.string("ident"),
                            eksternIdType = row.string("ekstern_id_type"),
                            eksternId = row.string("ekstern_id"),
                            skjedde = row.localDate("skjedde"),
                            forretningsprosess = row.string("forretningsprosess"),
                            opprettet = row.localDateTime("opprettet"),
                            opplysningerId = row.uuid("opplysninger_id"),
                            tilstand = row.string("tilstand"),
                            sistEndretTilstand = row.localDateTime("sist_endret_tilstand"),
                            basertPåBehandlingId = row.uuidOrNull("basert_på_behandling_id"),
                        )
                    }.asList,
                ).associateBy { it.behandlingId }

        // Bygg behandlinger med korrekte basertPå-referanser
        val behandlingerMap = mutableMapOf<UUID, Behandling>()

        fun byggBehandling(
            id: UUID,
            opplysningerMap: Map<UUID, Opplysninger>,
        ): Behandling? {
            behandlingerMap[id]?.let { return it }
            val rad = behandlingRader[id] ?: return null

            val basertPå = rad.basertPåBehandlingId?.let { byggBehandling(it, opplysningerMap) }

            val behandling =
                Behandling.rehydrer(
                    behandlingId = rad.behandlingId,
                    behandler =
                        Hendelse(
                            meldingsreferanseId = rad.meldingId,
                            type = rad.hendelseType,
                            ident = rad.ident,
                            eksternId = EksternId.fromString(rad.eksternIdType, rad.eksternId),
                            skjedde = rad.skjedde,
                            forretningsprosess = RegistrertForretningsprosess.opprett(rad.forretningsprosess),
                            opprettet = rad.opprettet,
                        ),
                    gjeldendeOpplysninger = opplysningerMap.getValue(rad.opplysningerId),
                    basertPå = basertPå,
                    opprettet = rad.opprettet,
                    tilstand = Behandling.TilstandType.valueOf(rad.tilstand),
                    sistEndretTilstand = rad.sistEndretTilstand,
                    avklaringer = avklaringerMap[id] ?: emptyList(),
                    godkjent = arbeidsstegMap[id to Arbeidssteg.Oppgave.Godkjent] ?: Arbeidssteg(Arbeidssteg.Oppgave.Godkjent),
                    besluttet = arbeidsstegMap[id to Arbeidssteg.Oppgave.Besluttet] ?: Arbeidssteg(Arbeidssteg.Oppgave.Besluttet),
                )
            behandlingerMap[id] = behandling
            return behandling
        }

        val opplysningerMap =
            this
                .hentOpplysninger(
                    behandlingRader
                        .map { (_, value) -> value.opplysningerId }
                        .toSet(),
                )
        // Build only the originally requested behandlinger
        return behandlingIder.mapNotNull { byggBehandling(it, opplysningerMap) }
    }

    override fun lagre(behandling: Behandling) {
        val unitOfWork = PostgresUnitOfWork.transaction()
        lagre(behandling, unitOfWork)
        unitOfWork.commit()
    }

    override fun lagre(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    ) = lagre(behandling, unitOfWork as PostgresUnitOfWork)

    override fun finnBehandlinger(
        tilstand: Behandling.TilstandType,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        block: (Behandling) -> Unit,
    ) = sessionOf(dataSource).use { session ->
        session.forEach(
            queryOf(
                // language="PostgreSQL"
                """
                SELECT behandling_id
                FROM behandling
                WHERE tilstand = :tilstand
                  AND sist_endret_tilstand BETWEEN :fraOgMed AND :tilOgMed
                ORDER BY behandling_id
                """.trimIndent(),
                mapOf(
                    "tilstand" to tilstand.name,
                    "fraOgMed" to fraOgMed.atStartOfDay(),
                    "tilOgMed" to tilOgMed.plusDays(1).atStartOfDay(),
                ),
            ),
        ) {
            val hentBehandling = session.hentBehandling(it.uuid("behandling_id"))!!
            block(hentBehandling)
        }
    }

    private fun lagre(
        behandling: Behandling,
        unitOfWork: PostgresUnitOfWork,
    ) {
        unitOfWork.inTransaction { tx ->
            tx.run(
                queryOf(
                    // language=PostgreSQL
                    """
                        INSERT INTO behandler_hendelse (ident, melding_id, ekstern_id_type, ekstern_id, hendelse_type, skjedde, forretningsprosess) 
                        VALUES (:ident, :melding_id, :ekstern_id_type, :ekstern_id, :hendelse_type, :skjedde, :forretningsprosess) ON CONFLICT DO NOTHING 
                    """.trimMargin(),
                    mapOf(
                        "ident" to behandling.behandler.ident,
                        "melding_id" to behandling.behandler.meldingsreferanseId,
                        "ekstern_id_type" to behandling.behandler.eksternId.type,
                        "ekstern_id" to behandling.behandler.eksternId.id,
                        "hendelse_type" to behandling.behandler.type,
                        "skjedde" to behandling.behandler.skjedde,
                        "forretningsprosess" to behandling.behandler.forretningsprosess.navn,
                    ),
                ).asUpdate,
            )
            tx.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    INSERT INTO behandling (behandling_id, tilstand, sist_endret_tilstand, basert_på_behandling_id)
                    VALUES (:id, :tilstand, :sisteEndretTilstand, :basertPaaBehandlingId)
                    ON CONFLICT (behandling_id) DO UPDATE SET tilstand                = :tilstand,
                                                              sist_endret_tilstand    = :sisteEndretTilstand,
                                                              basert_på_behandling_id = :basertPaaBehandlingId
                    """.trimIndent(),
                    mapOf(
                        "id" to behandling.behandlingId,
                        "tilstand" to behandling.tilstand().first.name,
                        "sisteEndretTilstand" to behandling.tilstand().second,
                        "basertPaaBehandlingId" to behandling.basertPå?.behandlingId,
                    ),
                ).asUpdate,
            )
            tx.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO behandling_tilstand (behandling_id, tilstand, endret)
                    VALUES (:behandling_id, :tilstand, :endret)
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    mapOf(
                        "behandling_id" to behandling.behandlingId,
                        "tilstand" to behandling.tilstand().first.name,
                        "endret" to behandling.tilstand().second,
                    ),
                ).asUpdate,
            )
            tx.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    INSERT INTO behandler_hendelse_behandling (behandling_id, melding_id) 
                    VALUES (:behandling_id, :melding_id) ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    mapOf(
                        "behandling_id" to behandling.behandlingId,
                        "melding_id" to behandling.behandler.meldingsreferanseId,
                    ),
                ).asUpdate,
            )

            // TODO: kan vi unngå hardkoding her?
            tx.lageArbeidssteg(behandling.behandlingId, behandling.godkjent)
            tx.lageArbeidssteg(behandling.behandlingId, behandling.besluttet)

            opplysningRepository.lagreOpplysninger(behandling.opplysninger() as Opplysninger, unitOfWork)

            tx.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    INSERT INTO behandling_opplysninger (opplysninger_id, behandling_id) 
                    VALUES (:opplysninger_id, :behandling_id) ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    mapOf(
                        "opplysninger_id" to behandling.opplysninger().id,
                        "behandling_id" to behandling.behandlingId,
                    ),
                ).asUpdate,
            )

            avklaringRepository.lagreAvklaringer(behandling, unitOfWork)
        }
    }

    private fun Session.lageArbeidssteg(
        behandlingId: UUID,
        arbeidssteg: Arbeidssteg,
    ) {
        run(
            when (arbeidssteg.tilstandType) {
                Arbeidssteg.TilstandType.IkkeUtført -> {
                    queryOf(
                        //language=PostgreSQL
                        """DELETE FROM behandling_arbeidssteg WHERE behandling_id = :behandling_id AND oppgave = :oppgave""",
                        mapOf("behandling_id" to behandlingId, "oppgave" to arbeidssteg.oppgave.name),
                    ).asUpdate
                }

                Arbeidssteg.TilstandType.Utført -> {
                    queryOf(
                        // language=PostgreSQL
                        """
                        INSERT INTO behandling_arbeidssteg(behandling_id, oppgave, tilstand, utført_av, utført) 
                        VALUES (:behandling_id, :oppgave, :tilstand, :utfort_av, :utfort) ON CONFLICT (behandling_id, oppgave) DO UPDATE SET tilstand = :tilstand, utført_av = :utfort_av, utført = :utfort 
                        """.trimIndent(),
                        mapOf(
                            "behandling_id" to behandlingId,
                            "oppgave" to arbeidssteg.oppgave.name,
                            "tilstand" to arbeidssteg.tilstandType.name,
                            "utfort_av" to arbeidssteg.utførtAv.ident,
                            "utfort" to arbeidssteg.utført,
                        ),
                    ).asUpdate
                }
            },
        )
    }

    override fun lagreBegrunnelse(
        opplysningId: UUID,
        begrunnelse: String,
    ) {
        sessionOf(dataSource).use {
            val kildeId =
                it.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT o.kilde_id
                        FROM opplysning o 
                        WHERE o.id = :opplysningId
                        """.trimIndent(),
                        mapOf(
                            "opplysningId" to opplysningId,
                        ),
                    ).map { it.uuid("kilde_id") }.asSingle,
                ) ?: throw IllegalArgumentException("Fant ikke kilde for opplysning $opplysningId")

            kildeRepository.lagreBegrunnelse(kildeId, begrunnelse)
        }
    }

    override fun lagreUtbetalingStatus(utbetalingStatus: UtbetalingStatus) {
        sessionOf(dataSource).use { session ->
            session.transaction {
                it.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        INSERT INTO utbetaling_status (behandling_id, status, behandlet_hendelse_id, endret) 
                        VALUES (:behandling_id, :status, :behandlet_hendelse_id, :endret)
                        ON CONFLICT (behandling_id, behandlet_hendelse_id) DO UPDATE SET status = :status, endret = :endret
                        """.trimIndent(),
                        mapOf(
                            "behandling_id" to utbetalingStatus.behandlingId,
                            "status" to utbetalingStatus.status.name,
                            "behandlet_hendelse_id" to utbetalingStatus.behandletHendelseId,
                            "endret" to utbetalingStatus.opprettet,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hentUtbetalingStatus(behandlingId: UUID): UtbetalingStatus.Status {
        sessionOf(dataSource).use { session ->
            return session
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT status 
                        FROM utbetaling_status 
                        WHERE behandling_id = :behandling_id
                        """.trimIndent(),
                        mapOf(
                            "behandling_id" to behandlingId,
                        ),
                    ).map { it.string("status") }.asSingle,
                )?.let { UtbetalingStatus.Status.valueOf(it) }
                ?: throw IllegalArgumentException("Fant ikke utbetalingstatus for behandling $behandlingId")
        }
    }
}

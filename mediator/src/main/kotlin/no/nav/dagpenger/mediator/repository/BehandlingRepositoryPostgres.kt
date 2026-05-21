package no.nav.dagpenger.mediator.repository

import kotliquery.Session
import kotliquery.queryOf
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.mediator.Metrikk.hentBehandlingTimer
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.mediator.repository.OpplysningerRepositoryPostgres.Companion.hentOpplysninger
import no.nav.dagpenger.modell.Arbeidssteg
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Behandlingkjede
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.hendelser.EksternId
import no.nav.dagpenger.modell.hendelser.Hendelse
import no.nav.dagpenger.modell.hendelser.UtbetalingStatus
import no.nav.dagpenger.modell.somKjede
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.opplysning.Saksbehandler
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.groupBy

internal class BehandlingRepositoryPostgres(
    private val dbSession: DatabaseSession,
    private val opplysningRepository: OpplysningerRepository,
    private val avklaringRepository: AvklaringRepository,
    private val kildeRepository: KildeRepository,
    private val prosessregister: Prosessregister,
) : BehandlingRepository,
    AvklaringRepository by avklaringRepository {
    override fun hentBehandling(behandlingId: UUID): Behandling? =
        hentBehandlingTimer.time<Behandling?> {
            dbSession.session { session ->
                session.hentBehandling(behandlingId)
            }
        }

    override fun hentBehandlinger(ident: Ident): List<Behandlingkjede> =
        dbSession.session { session ->
            session.hentBehandlinger(HentBehandling.AlleForIdent(ident))
        }

    override fun flyttBehandling(
        behandlingId: UUID,
        nyBasertPåId: UUID?,
    ) {
        dbSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    "UPDATE behandling SET basert_på_behandling_id = :basertPaa WHERE behandling_id = :behandlingId",
                    mapOf(
                        "behandlingId" to behandlingId,
                        "basertPaa" to nyBasertPåId,
                    ),
                ).asUpdate,
            )
        }
    }

    private fun Session.hentBehandling(behandlingId: UUID): Behandling? {
        // når vi henter en enkelt behandling så får vi maksimalt én kjede tilbake
        val kjede = hentBehandlinger(HentBehandling.Behandling(behandlingId)).singleOrNull() ?: return null
        return kjede.single { it.behandlingId == behandlingId }
    }

    private fun byggCTEForIdent(ident: Ident): Pair<String, Map<String, Any>> {
        // language=PostgreSQL
        val cte =
            """
            with recursive 
            personens_behandlinger as (
                select b.behandling_id, b.basert_på_behandling_id
                from behandling b
                inner join person_behandling pb on pb.behandling_id = b.behandling_id 
                where pb.ident = ANY(:identer)
            ), 
            behandlingkjede as (
                -- rotbehandlinger, de som ikke peker på noen andre
                select b.behandling_id, b.basert_på_behandling_id, 0 as dybde
                from personens_behandlinger b
                where b.basert_på_behandling_id is null
                
                union all
                -- rekursive behandlinger. avstanden øker jo lenger fremover i kjeden vi går
                select r.behandling_id, r.basert_på_behandling_id, bk.dybde + 1
                from personens_behandlinger r
                join behandlingkjede bk on bk.behandling_id = r.basert_på_behandling_id
            )
            """.trimIndent()

        return cte to mapOf("identer" to ident.alleIdentifikatorer().toTypedArray())
    }

    private fun byggCTEForBehandling(behandlingId: UUID): Pair<String, Map<String, Any>> {
        // language=PostgreSQL
        val cte =
            """
            with recursive behandlingkjede as (
                select behandling_id, basert_på_behandling_id, 0 as dybde
                from behandling
                where behandling_id = :behandlingId
            
                union all
            
                -- rekursive behandlinger. avstanden minsker jo lenger bakover i kjeden vi beveger oss, slik at roten har lavest dybde
                select r.behandling_id, r.basert_på_behandling_id, bk.dybde - 1
                from behandling r
                join behandlingkjede bk on bk.basert_på_behandling_id = r.behandling_id
            )
            """.trimIndent()
        return cte to mapOf("behandlingId" to behandlingId)
    }

    private sealed interface HentBehandling {
        data class AlleForIdent(
            val ident: Ident,
        ) : HentBehandling

        data class Behandling(
            val behandlingId: UUID,
        ) : HentBehandling
    }

    private fun Session.lagBehandlingRad(valg: HentBehandling): List<BehandlingRad> {
        // Finn basertPå-relasjoner for disse IDene og sorterer dem
        // topologisk slik at den første/eldste behandlingen i kjeden kommer først

        val (cteForBehandlinger, params) =
            when (valg) {
                is HentBehandling.AlleForIdent -> byggCTEForIdent(valg.ident)
                is HentBehandling.Behandling -> byggCTEForBehandling(valg.behandlingId)
            }

        val behandlingRader =
            this
                .run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        $cteForBehandlinger
                        
                        select b.*, bhb.*, bh.*, bo.*
                        from behandlingkjede bk
                        inner join behandling b on bk.behandling_id = b.behandling_id
                        left join behandler_hendelse_behandling bhb on b.behandling_id = bhb.behandling_id
                        left join behandler_hendelse bh on bh.melding_id = bhb.melding_id
                        left join behandling_opplysninger bo on b.behandling_id = bo.behandling_id     
                        order by dybde
                        """.trimIndent(),
                        params,
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
                ).also { liste ->
                    val unike = liste.distinctBy { it.behandlingId }
                    check(liste.size == unike.size) { "spørringen forventer at det ikke er duplikater" }
                }
        return behandlingRader
    }

    private fun Session.lagArbeidsstegMap(alleBehandlingIder: Set<UUID>): Map<Pair<UUID, Arbeidssteg.Oppgave>, Arbeidssteg> {
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
        return arbeidsstegMap
    }

    private fun Session.hentBehandlinger(valg: HentBehandling): List<Behandlingkjede> {
        val behandlingRader = lagBehandlingRad(valg)
        val alleBehandlingIder = behandlingRader.map { it.behandlingId }.toSet()

        val arbeidsstegMap = lagArbeidsstegMap(alleBehandlingIder)

        // Hent avklaringer for alle behandlinger i én spørring
        val avklaringerMap = hentAvklaringer(alleBehandlingIder)

        val opplysningerMap =
            this
                .hentOpplysninger(
                    kildeRepository,
                    behandlingRader
                        .map { it.opplysningerId }
                        .toSet(),
                )

        return lagBehandlingKjede(behandlingRader, avklaringerMap, opplysningerMap, arbeidsstegMap)
    }

    private fun lagBehandlingKjede(
        behandlingRader: List<BehandlingRad>,
        avklaringerMap: Map<UUID, List<Avklaring>>,
        opplysningerMap: Map<UUID, Opplysninger>,
        arbeidsstegMap: Map<Pair<UUID, Arbeidssteg.Oppgave>, Arbeidssteg>,
    ): List<Behandlingkjede> {
        // Bygg behandlinger med korrekte basertPå-referanser
        val behandlingerMap = linkedMapOf<UUID, Behandling>()
        return behandlingRader
            .map { rad ->
                check(rad.behandlingId !in behandlingerMap) { "skal ikke finnes fra før" }
                val basertPå =
                    rad.basertPåBehandlingId?.let {
                        checkNotNull(behandlingerMap[it]) {
                            "skal kunne finne en rehydret behandling med $it fordi den er tidligere i kjeden"
                        }
                    }
                Behandling
                    .rehydrer(
                        behandlingId = rad.behandlingId,
                        behandler =
                            Hendelse(
                                meldingsreferanseId = rad.meldingId,
                                type = rad.hendelseType,
                                ident = rad.ident,
                                eksternId = EksternId.fromString(rad.eksternIdType, rad.eksternId),
                                skjedde = rad.skjedde,
                                forretningsprosess = prosessregister.opprett(rad.forretningsprosess),
                                opprettet = rad.opprettet,
                            ),
                        gjeldendeOpplysninger = opplysningerMap.getValue(rad.opplysningerId),
                        basertPå = basertPå,
                        opprettet = rad.opprettet,
                        tilstand = Behandling.TilstandType.valueOf(rad.tilstand),
                        sistEndretTilstand = rad.sistEndretTilstand,
                        avklaringer = avklaringerMap[rad.behandlingId] ?: emptyList(),
                        godkjent =
                            arbeidsstegMap[rad.behandlingId to Arbeidssteg.Oppgave.Godkjent] ?: Arbeidssteg(
                                Arbeidssteg.Oppgave.Godkjent,
                            ),
                        besluttet =
                            arbeidsstegMap[rad.behandlingId to Arbeidssteg.Oppgave.Besluttet]
                                ?: Arbeidssteg(Arbeidssteg.Oppgave.Besluttet),
                    ).also {
                        behandlingerMap[it.behandlingId] = it
                    }
            }.groupBy { it.behandlingskjedeId }
            .map { (_, behandlinger) -> behandlinger.somKjede() }
    }

    // Hent alle behandlinger i én spørring
    private data class BehandlingRad(
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

    override fun finnBehandlinger(
        tilstand: Behandling.TilstandType,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        block: (Behandling) -> Unit,
    ) = dbSession.session { session ->
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

    override fun lagre(
        ident: Ident,
        behandlinger: List<Behandling>,
        unitOfWork: PostgresUnitOfWork,
    ) {
        lagreBehandlingHendelse(unitOfWork, behandlinger)
        lagreBehandlinger(unitOfWork, behandlinger)
        lagrePersonBehandlingkoblinger(unitOfWork, ident, behandlinger)
        lagreBehandlingTilstand(unitOfWork, behandlinger)
        lagreBehandlerHendelse(unitOfWork, behandlinger)
        lagreBehandlingArbeidssted(unitOfWork, behandlinger)
        lagreOpplysninger(unitOfWork, behandlinger)

        avklaringRepository.lagreAvklaringer(
            behandlinger
                .flatMap { behandling ->
                    behandling.avklaringer().map { behandling to it }
                },
            unitOfWork,
        )
    }

    private fun lagreOpplysninger(
        unitOfWork: PostgresUnitOfWork,
        behandlinger: List<Behandling>,
    ) {
        opplysningRepository.lagreOpplysninger(behandlinger.map { it.opplysninger }, unitOfWork)

        val params =
            behandlinger.map { behandling ->
                mapOf(
                    "opplysninger_id" to behandling.opplysninger().id,
                    "behandling_id" to behandling.behandlingId,
                )
            }
        unitOfWork.session.batchPreparedNamedStatement(
            // language=PostgreSQL
            """
            INSERT INTO behandling_opplysninger (opplysninger_id, behandling_id) 
            VALUES (:opplysninger_id, :behandling_id) ON CONFLICT DO NOTHING
            """.trimIndent(),
            params,
        )
    }

    private fun lagreBehandlingArbeidssted(
        unitOfWork: PostgresUnitOfWork,
        behandlinger: List<Behandling>,
    ) {
        val arbeidsstegene =
            behandlinger
                .flatMap { behandling ->
                    listOf(behandling.godkjent, behandling.besluttet).map { behandling to it }
                }

        val deleteParams =
            arbeidsstegene
                .mapNotNull { (behandling, arbeidssteg) ->
                    when (arbeidssteg.tilstandType) {
                        Arbeidssteg.TilstandType.IkkeUtført ->
                            mapOf(
                                "behandling_id" to behandling.behandlingId,
                                "oppgave" to arbeidssteg.oppgave.name,
                            )
                        Arbeidssteg.TilstandType.Utført -> null
                    }
                }
        val insertParams =
            arbeidsstegene
                .mapNotNull { (behandling, arbeidssteg) ->
                    when (arbeidssteg.tilstandType) {
                        Arbeidssteg.TilstandType.IkkeUtført -> null
                        Arbeidssteg.TilstandType.Utført ->
                            mapOf(
                                "behandling_id" to behandling.behandlingId,
                                "oppgave" to arbeidssteg.oppgave.name,
                                "tilstand" to arbeidssteg.tilstandType.name,
                                "utfort_av" to arbeidssteg.utførtAv.ident,
                                "utfort" to arbeidssteg.utført,
                            )
                    }
                }

        unitOfWork.session
            .batchPreparedNamedStatement(
                //language=PostgreSQL
                """DELETE FROM behandling_arbeidssteg WHERE behandling_id = :behandling_id AND oppgave = :oppgave""",
                deleteParams,
            )

        unitOfWork.session
            .batchPreparedNamedStatement(
                // language=PostgreSQL
                """
                INSERT INTO behandling_arbeidssteg(behandling_id, oppgave, tilstand, utført_av, utført) 
                VALUES (:behandling_id, :oppgave, :tilstand, :utfort_av, :utfort) 
                ON CONFLICT (behandling_id, oppgave) DO UPDATE SET tilstand = :tilstand, utført_av = :utfort_av, utført = :utfort 
                """.trimIndent(),
                insertParams,
            ).krevAtAntallRaderErNøyaktigLik(insertParams.size)
    }

    private fun lagreBehandlerHendelse(
        unitOfWork: PostgresUnitOfWork,
        behandlinger: List<Behandling>,
    ) {
        val params =
            behandlinger.map { behandling ->
                mapOf(
                    "behandling_id" to behandling.behandlingId,
                    "melding_id" to behandling.behandler.meldingsreferanseId,
                )
            }
        unitOfWork.session.batchPreparedNamedStatement(
            // language=PostgreSQL
            """
            INSERT INTO behandler_hendelse_behandling (behandling_id, melding_id) 
            VALUES (:behandling_id, :melding_id) ON CONFLICT DO NOTHING
            """.trimIndent(),
            params,
        )
    }

    private fun lagreBehandlingTilstand(
        unitOfWork: PostgresUnitOfWork,
        behandlinger: List<Behandling>,
    ) {
        val params =
            behandlinger.map { behandling ->
                mapOf(
                    "behandling_id" to behandling.behandlingId,
                    "tilstand" to behandling.tilstand().first.name,
                    "endret" to behandling.tilstand().second,
                )
            }
        unitOfWork.session.batchPreparedNamedStatement(
            //language=PostgreSQL
            """
            INSERT INTO behandling_tilstand (behandling_id, tilstand, endret)
            VALUES (:behandling_id, :tilstand, :endret)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            params,
        )
    }

    private fun lagreBehandlinger(
        unitOfWork: PostgresUnitOfWork,
        behandlinger: List<Behandling>,
    ) {
        val params =
            behandlinger.map { behandling ->
                mapOf(
                    "id" to behandling.behandlingId,
                    "tilstand" to behandling.tilstand().first.name,
                    "sisteEndretTilstand" to behandling.tilstand().second,
                    "basertPaaBehandlingId" to behandling.basertPå?.behandlingId,
                )
            }
        unitOfWork.session
            .batchPreparedNamedStatement(
                // language=PostgreSQL
                """
                INSERT INTO behandling (behandling_id, tilstand, sist_endret_tilstand, basert_på_behandling_id)
                VALUES (:id, :tilstand, :sisteEndretTilstand, :basertPaaBehandlingId)
                ON CONFLICT (behandling_id) DO UPDATE SET tilstand                = :tilstand,
                                                          sist_endret_tilstand    = :sisteEndretTilstand,
                                                          basert_på_behandling_id = :basertPaaBehandlingId
                """.trimIndent(),
                params,
            ).krevAtAntallRaderErNøyaktigLik(params.size)
    }

    private fun lagreBehandlingHendelse(
        unitOfWork: PostgresUnitOfWork,
        behandlinger: List<Behandling>,
    ) {
        val params =
            behandlinger.map { behandling ->
                mapOf(
                    "ident" to behandling.behandler.ident,
                    "melding_id" to behandling.behandler.meldingsreferanseId,
                    "ekstern_id_type" to behandling.behandler.eksternId.type,
                    "ekstern_id" to behandling.behandler.eksternId.id,
                    "hendelse_type" to behandling.behandler.type,
                    "skjedde" to behandling.behandler.skjedde,
                    "forretningsprosess" to behandling.behandler.forretningsprosess.navn,
                )
            }

        unitOfWork.session
            .batchPreparedNamedStatement(
                // language=PostgreSQL
                """
                INSERT INTO behandler_hendelse (ident, melding_id, ekstern_id_type, ekstern_id, hendelse_type, skjedde, forretningsprosess) 
                VALUES (:ident, :melding_id, :ekstern_id_type, :ekstern_id, :hendelse_type, :skjedde, :forretningsprosess) 
                ON CONFLICT DO NOTHING 
                """.trimMargin(),
                params,
            )
    }

    private fun lagrePersonBehandlingkoblinger(
        unitOfWork: PostgresUnitOfWork,
        ident: Ident,
        behandlinger: List<Behandling>,
    ) {
        val params =
            behandlinger.map { behandling ->
                mapOf(
                    "ident" to ident.identifikator(),
                    "behandling_id" to behandling.behandlingId,
                )
            }
        unitOfWork.session
            .batchPreparedNamedStatement(
                //language=PostgreSQL
                """
                INSERT INTO person_behandling (ident, behandling_id)
                VALUES (:ident, :behandling_id)
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                params,
            )
    }

    override fun lagreBegrunnelse(
        opplysningId: UUID,
        begrunnelse: String,
    ) {
        dbSession.session {
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
        dbSession.session { session ->
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

    override fun hentUtbetalingStatus(behandlingId: UUID): UtbetalingStatus.Status =
        dbSession.session { session ->
            session
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

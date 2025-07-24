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
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.RegistrertForretningsprosess
import no.nav.dagpenger.opplysning.Saksbehandler
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

    private fun Session.hentBehandling(behandlingId: UUID): Behandling? =
        this.run(
            queryOf(
                // language=PostgreSQL
                """
                SELECT *  
                FROM behandling 
                LEFT JOIN behandler_hendelse_behandling ON behandling.behandling_id = behandler_hendelse_behandling.behandling_id
                LEFT JOIN behandler_hendelse ON behandler_hendelse.melding_id = behandler_hendelse_behandling.melding_id
                LEFT JOIN behandling_opplysninger ON behandling.behandling_id = behandling_opplysninger.behandling_id                    
                WHERE behandling.behandling_id = :id 
                """.trimIndent(),
                mapOf(
                    "id" to behandlingId,
                ),
            ).map { row ->
                val basertPåBehandlingId = row.uuidOrNull("basert_på_behandling_id")
                val basertPåBehandling = basertPåBehandlingId?.let { id -> this.hentBehandling(id) }

                Behandling.rehydrer(
                    behandlingId = row.uuid("behandling_id"),
                    behandler =
                        Hendelse(
                            meldingsreferanseId = row.uuid("melding_id"),
                            type = row.string("hendelse_type"),
                            ident = row.string("ident"),
                            eksternId =
                                EksternId.fromString(
                                    row.string("ekstern_id_type"),
                                    row.string("ekstern_id"),
                                ),
                            skjedde = row.localDate("skjedde"),
                            forretningsprosess = RegistrertForretningsprosess.opprett(row.string("forretningsprosess")),
                            opprettet = row.localDateTime("opprettet"),
                        ),
                    gjeldendeOpplysninger = this.hentOpplysninger(row.uuid("opplysninger_id")),
                    basertPå = basertPåBehandling,
                    tilstand = Behandling.TilstandType.valueOf(row.string("tilstand")),
                    sistEndretTilstand = row.localDateTime("sist_endret_tilstand"),
                    avklaringer = hentAvklaringer(behandlingId),
                    godkjent = this.hentArbeidssteg(behandlingId, Arbeidssteg.Oppgave.Godkjent),
                    besluttet = this.hentArbeidssteg(behandlingId, Arbeidssteg.Oppgave.Besluttet),
                )
            }.asSingle,
        )

    private fun Session.hentArbeidssteg(
        behandlingId: UUID,
        oppgave: Arbeidssteg.Oppgave,
    ): Arbeidssteg =
        this.run(
            queryOf(
                //language=PostgreSQL
                """
                SELECT * 
                FROM behandling_arbeidssteg 
                WHERE behandling_id = :behandling_id
                AND oppgave = :oppgave
                """.trimIndent(),
                mapOf(
                    "behandling_id" to behandlingId,
                    "oppgave" to oppgave.name,
                ),
            ).map { row ->
                Arbeidssteg.rehydrer(
                    Arbeidssteg.TilstandType.valueOf(row.string("tilstand")),
                    Arbeidssteg.Oppgave.valueOf(row.string("oppgave")),
                    row.stringOrNull("utført_av")?.let { Saksbehandler(it) },
                    row.localDateTimeOrNull("utført"),
                )
            }.asSingle,
        ) ?: Arbeidssteg(oppgave)

    override fun lagre(behandling: Behandling) {
        val unitOfWork = PostgresUnitOfWork.transaction()
        lagre(behandling, unitOfWork)
        unitOfWork.commit()
    }

    override fun lagre(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    ) = lagre(behandling, unitOfWork as PostgresUnitOfWork)

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
                Arbeidssteg.TilstandType.IkkeUtført ->
                    queryOf(
                        //language=PostgreSQL
                        """DELETE FROM behandling_arbeidssteg WHERE behandling_id = :behandling_id AND oppgave = :oppgave""",
                        mapOf("behandling_id" to behandlingId, "oppgave" to arbeidssteg.oppgave.name),
                    ).asUpdate

                Arbeidssteg.TilstandType.Utført ->
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
}

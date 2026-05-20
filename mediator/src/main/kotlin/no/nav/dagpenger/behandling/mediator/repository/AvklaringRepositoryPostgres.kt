package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.queryOf
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.avklaring.Avklaring.Endring.Avbrutt
import no.nav.dagpenger.avklaring.Avklaring.Endring.Avklart
import no.nav.dagpenger.avklaring.Avklaring.Endring.UnderBehandling
import no.nav.dagpenger.behandling.mediator.db.DatabaseSession
import no.nav.dagpenger.behandling.mediator.objectMapper
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryObserver.NyAvklaringHendelse
import no.nav.dagpenger.behandling.mediator.repository.JsonSerde.Companion.serde
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDateTime
import java.util.UUID

internal class AvklaringRepositoryPostgres(
    private val dbSession: DatabaseSession,
    private val kildeRepository: KildeRepository,
    observatører: List<AvklaringRepositoryObserver> = emptyList(),
) : AvklaringRepository {
    private val observatører = observatører.toMutableList()

    fun registerObserver(observer: AvklaringRepositoryObserver) {
        observatører.add(observer)
    }

    override fun hentAvklaringer(behandlingId: UUID) = hentAvklaringer(setOf(behandlingId))[behandlingId] ?: emptyList()

    override fun hentAvklaringer(behandlingIder: Set<UUID>): Map<UUID, List<Avklaring>> {
        if (behandlingIder.isEmpty()) return emptyMap()

        return dbSession.session { session ->
            val avklaringer =
                session.run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        SELECT a.behandling_id,
                               a.id                                               AS avklaring_id,
                               a.kode,
                               a.tittel,
                               a.beskrivelse,
                               a.kan_kvitteres,
                               a.kan_avbrytes,

                               JSON_AGG(
                               JSON_BUILD_OBJECT(
                                       'endring_id', ae.endring_id,
                                       'endret', ae.endret,
                                       'type', ae.type,
                                       'kilde_id', ae.kilde_id,
                                       'begrunnelse', ae.begrunnelse
                               )
                               ORDER BY ae.endret
                                       ) FILTER (WHERE ae.endring_id IS NOT NULL) AS endringer

                        FROM avklaring a
                                 LEFT JOIN avklaring_endring ae ON a.id = ae.avklaring_id
                        WHERE a.behandling_id = ANY(:behandling_ider)
                        GROUP BY a.behandling_id, a.id
                        """.trimIndent(),
                        mapOf(
                            "behandling_ider" to
                                session.connection.underlying.createArrayOf(
                                    "uuid",
                                    behandlingIder.toTypedArray(),
                                ),
                        ),
                    ).map { row ->
                        val endringerJson = endringSerde.fromJson(row.stringOrNull("endringer") ?: "[]")
                        Pair(
                            row.uuid("behandling_id"),
                            Triple(
                                row.uuid("avklaring_id"),
                                endringerJson,
                                Avklaringkode(
                                    kode = row.string("kode"),
                                    tittel = row.string("tittel"),
                                    beskrivelse = row.string("beskrivelse"),
                                    kanKvitteres = row.boolean("kan_kvitteres"),
                                    kanAvbrytes = row.boolean("kan_avbrytes"),
                                ),
                            ),
                        )
                    }.asList,
                )

            val alleKildeIder =
                avklaringer
                    .flatMap { (_, triple) ->
                        triple.second.mapNotNull { it.kilde_id }
                    }.distinct()

            val kilder = kildeRepository.hentKilder(alleKildeIder, session)

            // Grupper avklaringer per behandling
            avklaringer
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, avklaringerForBehandling) ->
                    avklaringerForBehandling.map { (avklaringId, endringerJson, kode) ->
                        Avklaring.rehydrer(
                            id = avklaringId,
                            kode = kode,
                            historikk = endringerJson.map { it.somHistorikk(kilder) }.toMutableList(),
                        )
                    }
                }
        }
    }

    override fun lagreAvklaringer(
        avklaringer: List<Pair<Behandling, Avklaring>>,
        unitOfWork: PostgresUnitOfWork,
    ) {
        val unikeAvklaringer = avklaringer.map { it.second }.distinctBy { it.id }

        lagreAlleAvklaringer(avklaringer, unitOfWork)
        lagreKilder(unikeAvklaringer, unitOfWork)
        lagreNyeEndringer(unikeAvklaringer, unitOfWork)

        // Emit observer-events utledet fra nye endringer — før markerLagret
        avklaringer.forEach { (behandling, avklaring) ->
            val ident = behandling.behandler.ident
            val kontekst = behandling.toSpesifikkKontekst()

            if (avklaring.erNy) {
                emitNyAvklaring(ident, kontekst, avklaring)
            }

            if (avklaring.nyeEndringer.isNotEmpty()) {
                emitEndretAvklaring(ident, kontekst, avklaring)
            }
        }

        unikeAvklaringer.forEach { it.markerLagret() }
    }

    private fun lagreAlleAvklaringer(
        avklaringer: List<Pair<Behandling, Avklaring>>,
        unitOfWork: PostgresUnitOfWork,
    ) {
        BatchStatement(
            // language=PostgreSQL
            """
            INSERT INTO avklaring (id, behandling_id, kode, tittel, beskrivelse, kan_kvitteres, kan_avbrytes)
            VALUES (:avklaring_id, :behandling_id, :kode, :tittel, :beskrivelse, :kanKvitteres, :kanAvbrytes)
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
            avklaringer.map { (behandling: Behandling, avklaring: Avklaring) ->
                val avklaringskode = avklaring.kode
                mapOf(
                    "avklaring_id" to avklaring.id,
                    "behandling_id" to behandling.behandlingId,
                    "kode" to avklaringskode.kode,
                    "tittel" to avklaringskode.tittel,
                    "beskrivelse" to avklaringskode.beskrivelse,
                    "kanKvitteres" to avklaringskode.kanKvitteres,
                    "kanAvbrytes" to avklaringskode.kanAvbrytes,
                )
            },
        ).run(unitOfWork.session)
    }

    private fun lagreKilder(
        avklaringer: Collection<Avklaring>,
        unitOfWork: PostgresUnitOfWork,
    ) {
        val alleKilder =
            avklaringer
                .flatMap { it.endringer.filterIsInstance<Avklart>().map { it.avklartAv } }
                .distinctBy { it.id }

        if (alleKilder.isNotEmpty()) {
            kildeRepository.lagreKilder(alleKilder, unitOfWork.session)
        }
    }

    private fun lagreNyeEndringer(
        avklaringer: Collection<Avklaring>,
        unitOfWork: PostgresUnitOfWork,
    ) {
        val alleEndringer =
            avklaringer.flatMap { avklaring ->
                avklaring.nyeEndringer.map { endring ->
                    val kildeId = (endring as? Avklart)?.avklartAv?.id
                    mapOf(
                        "endring_id" to endring.id,
                        "avklaring_id" to avklaring.id,
                        "endret" to endring.endret,
                        "endring_type" to
                            when (endring) {
                                is UnderBehandling -> "UnderBehandling"
                                is Avklart -> "Avklart"
                                is Avbrutt -> "Avbrutt"
                            },
                        "kilde_id" to kildeId,
                        "begrunnelse" to (endring as? Avklart)?.begrunnelse,
                    )
                }
            }

        if (alleEndringer.isNotEmpty()) {
            BatchStatement(
                // language=PostgreSQL
                """
                INSERT INTO avklaring_endring (endring_id, avklaring_id, endret, type, kilde_id, begrunnelse)
                VALUES (:endring_id, :avklaring_id, :endret, :endring_type, :kilde_id, :begrunnelse)
                """.trimIndent(),
                alleEndringer,
            ).run(unitOfWork.session)
        }
    }

    private data class RawEndringJson(
        val endring_id: UUID,
        val endret: LocalDateTime,
        val type: String,
        val kilde_id: UUID?,
        val begrunnelse: String?,
    ) {
        fun somHistorikk(kilder: Map<UUID, Kilde>) =
            when (EndringType.valueOf(type)) {
                EndringType.UnderBehandling -> UnderBehandling(endring_id, endret)
                EndringType.Avbrutt -> Avbrutt(endring_id, endret)
                EndringType.Avklart -> {
                    val kilde = kilder[kilde_id!!] ?: Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("DIGIDAG"))
                    Avklart(endring_id, kilde, begrunnelse ?: "", endret)
                }
            }
    }

    private fun emitNyAvklaring(
        ident: String,
        toSpesifikkKontekst: Behandling.BehandlingKontekst,
        avklaring: Avklaring,
    ) {
        observatører.forEach {
            it.nyAvklaring(
                NyAvklaringHendelse(
                    ident,
                    toSpesifikkKontekst,
                    avklaring,
                ),
            )
        }
    }

    private fun emitEndretAvklaring(
        ident: String,
        kontekst: Behandling.BehandlingKontekst,
        avklaring: Avklaring,
    ) {
        observatører.forEach {
            it.endretAvklaring(
                AvklaringRepositoryObserver.EndretAvklaringHendelse(
                    ident,
                    kontekst,
                    avklaring,
                ),
            )
        }
    }

    private enum class EndringType {
        UnderBehandling,
        Avklart,
        Avbrutt,
    }

    private companion object {
        val endringSerde = objectMapper.serde<List<RawEndringJson>>()
    }
}

interface AvklaringRepositoryObserver {
    fun nyAvklaring(nyAvklaringHendelse: NyAvklaringHendelse)

    data class NyAvklaringHendelse(
        val ident: String,
        val kontekst: Behandling.BehandlingKontekst,
        val avklaring: Avklaring,
    )

    fun endretAvklaring(endretAvklaringHendelse: EndretAvklaringHendelse)

    data class EndretAvklaringHendelse(
        val ident: String,
        val kontekst: Behandling.BehandlingKontekst,
        val avklaring: Avklaring,
    )
}

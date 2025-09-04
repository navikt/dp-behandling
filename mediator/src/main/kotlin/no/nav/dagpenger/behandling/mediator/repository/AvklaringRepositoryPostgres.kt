package no.nav.dagpenger.behandling.mediator.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.avklaring.Avklaring.Endring.Avbrutt
import no.nav.dagpenger.avklaring.Avklaring.Endring.Avklart
import no.nav.dagpenger.avklaring.Avklaring.Endring.UnderBehandling
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryObserver.NyAvklaringHendelse
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDateTime
import java.util.UUID

internal class AvklaringRepositoryPostgres private constructor(
    private val observatører: MutableList<AvklaringRepositoryObserver> = mutableListOf(),
    private val kildeRepository: KildeRepository = KildeRepository(),
) : AvklaringRepository {
    constructor(vararg observatører: AvklaringRepositoryObserver) : this(observatører.toMutableList())

    fun registerObserver(observer: AvklaringRepositoryObserver) {
        observatører.add(observer)
    }

    override fun lagreAvklaringer(
        behandling: Behandling,
        unitOfWork: UnitOfWork<*>,
    ) {
        lagre(behandling, unitOfWork as PostgresUnitOfWork)
    }

    override fun hentAvklaringer(behandlingId: UUID) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    SELECT a.id                                               AS avklaring_id,
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
                    WHERE a.behandling_id = :behandling_id
                    GROUP BY a.id
                    """.trimIndent(),
                    mapOf(
                        "behandling_id" to behandlingId,
                    ),
                ).map { row ->
                    val endringerJson = objectMapper.readValue<List<RawEndringJson>>(row.stringOrNull("endringer") ?: "[]")
                    val kildeIder = endringerJson.mapNotNull { it.kilde_id }
                    val kilder = kildeRepository.hentKilder(kildeIder, session)

                    Avklaring.rehydrer(
                        id = row.uuid("avklaring_id"),
                        kode =
                            Avklaringkode(
                                kode = row.string("kode"),
                                tittel = row.string("tittel"),
                                beskrivelse = row.string("beskrivelse"),
                                kanKvitteres = row.boolean("kan_kvitteres"),
                                kanAvbrytes = row.boolean("kan_avbrytes"),
                            ),
                        historikk = endringerJson.map { it.somHistorikk(kilder) }.toMutableList(),
                    )
                }.asList,
            )
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

    private fun lagre(
        behandling: Behandling,
        unitOfWork: PostgresUnitOfWork,
    ) {
        val avklaringer = behandling.avklaringer()
        val nyeAvklaringer = mutableListOf<Avklaring>()

        unitOfWork.inTransaction { tx ->
            avklaringer.forEach { avklaring ->
                val avklaringskode = avklaring.kode
                val lagret =
                    tx.run(
                        queryOf(
                            // language=PostgreSQL
                            """
                            INSERT INTO avklaring (id, behandling_id, kode, tittel, beskrivelse, kan_kvitteres, kan_avbrytes)
                            VALUES (:avklaring_id, :behandling_id, :kode, :tittel, :beskrivelse, :kanKvitteres, :kanAvbrytes)
                            ON CONFLICT (id) DO NOTHING
                            """.trimIndent(),
                            mapOf(
                                "avklaring_id" to avklaring.id,
                                "behandling_id" to behandling.behandlingId,
                                "kode" to avklaringskode.kode,
                                "tittel" to avklaringskode.tittel,
                                "beskrivelse" to avklaringskode.beskrivelse,
                                "kanKvitteres" to avklaringskode.kanKvitteres,
                                "kanAvbrytes" to avklaringskode.kanAvbrytes,
                            ),
                        ).asUpdate,
                    )

                val endringerLagret =
                    avklaring.endringer.map { endring ->
                        val kildeId =
                            when (endring) {
                                is Avklart -> {
                                    endring.avklartAv.let { kilde ->
                                        kildeRepository.lagreKilde(kilde, tx)
                                        kilde.id
                                    }
                                }

                                else -> null
                            }

                        tx.run(
                            queryOf(
                                // language=PostgreSQL
                                """
                                INSERT INTO avklaring_endring (endring_id, avklaring_id, endret, type, kilde_id, begrunnelse)
                                VALUES (:endring_id, :avklaring_id, :endret, :endring_type, :kilde_id, :begrunnelse)
                                ON CONFLICT DO NOTHING
                                """.trimIndent(),
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
                                ),
                            ).asUpdate,
                        )
                    }

                if (lagret != 0) nyeAvklaringer.add(avklaring)
                // if (endringerLagret.any { it == 1 }) TODO("Avklaringen er endret")
            }
        }
        nyeAvklaringer.forEach {
            emitNyAvklaring(
                behandling.behandler.ident,
                behandling.toSpesifikkKontekst(),
                it,
            )
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

    private enum class EndringType {
        UnderBehandling,
        Avklart,
        Avbrutt,
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

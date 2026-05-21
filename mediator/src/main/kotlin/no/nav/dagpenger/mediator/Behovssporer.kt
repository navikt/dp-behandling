package no.nav.dagpenger.mediator

import io.github.oshai.kotlinlogging.KotlinLogging
import io.prometheus.metrics.core.metrics.GaugeWithCallback
import kotliquery.queryOf
import no.nav.dagpenger.mediator.db.DatabaseSession
import java.util.UUID

internal class Behovssporer(
    private val dbSession: DatabaseSession,
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    init {
        registrerGauge()
    }

    enum class Kilde {
        Kafka,
        Api,
    }

    fun behovSendt(
        behandlingId: UUID,
        behov: List<String>,
        kilde: Kilde,
    ) {
        if (behov.isEmpty()) return
        dbSession.session { session ->
            session.transaction { tx ->
                behov.forEach { behovNavn ->
                    tx.run(
                        queryOf(
                            // language=PostgreSQL
                            """
                            INSERT INTO behandling_aktive_behov (behandling_id, behov, opprettet, status, kilde)
                            VALUES (:behandlingId, :behov, NOW(), 'pending', :kilde)
                            ON CONFLICT (behandling_id, behov) DO UPDATE SET
                                opprettet = NOW(),
                                status = 'pending',
                                kilde = :kilde
                            """.trimIndent(),
                            mapOf(
                                "behandlingId" to behandlingId,
                                "behov" to behovNavn,
                                "kilde" to kilde.name.lowercase(),
                            ),
                        ).asUpdate,
                    )
                }
            }
        }
        logger.info { "Behov sendt for behandlingId=$behandlingId: ${behov.joinToString()}, kilde=$kilde" }
    }

    fun behovLøst(
        behandlingId: UUID,
        vararg behov: String,
    ) {
        if (behov.isEmpty()) return
        dbSession.session { session ->
            session.transaction { tx ->
                behov.forEach { behovNavn ->
                    // Hent opprettet-tidspunkt og kilde for histogram
                    val rad =
                        tx.run(
                            queryOf(
                                // language=PostgreSQL
                                """
                                SELECT opprettet, kilde FROM behandling_aktive_behov
                                WHERE behandling_id = :behandlingId AND behov = :behov
                                """.trimIndent(),
                                mapOf("behandlingId" to behandlingId, "behov" to behovNavn),
                            ).map { row ->
                                row.localDateTime("opprettet") to row.string("kilde")
                            }.asSingle,
                        )

                    // Slett raden
                    tx.run(
                        queryOf(
                            // language=PostgreSQL
                            """
                            DELETE FROM behandling_aktive_behov
                            WHERE behandling_id = :behandlingId AND behov = :behov
                            """.trimIndent(),
                            mapOf("behandlingId" to behandlingId, "behov" to behovNavn),
                        ).asUpdate,
                    )

                    // Observer histogram hvis vi hadde en rad
                    if (rad != null) {
                        val (opprettet, kilde) = rad
                        val sekunder =
                            java.time.Duration
                                .between(opprettet, java.time.LocalDateTime.now())
                                .toMillis() / 1000.0
                        BehandlingMetrikker.behovLosningstid.labelValues(behovNavn, kilde).observe(sekunder)
                        logger.info { "Behov løst for behandlingId=$behandlingId: $behovNavn (${sekunder}s, kilde=$kilde)" }
                    } else {
                        logger.info { "Behov løst for behandlingId=$behandlingId: $behovNavn (ingen aktiv rad funnet)" }
                    }
                }
            }
        }
    }

    fun hentAktiveBehov(behandlingId: UUID): List<AktivtBehov> =
        dbSession.session { session ->
            session.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    SELECT behov, kilde, opprettet, status
                    FROM behandling_aktive_behov
                    WHERE behandling_id = :behandlingId
                    """.trimIndent(),
                    mapOf("behandlingId" to behandlingId),
                ).map { row ->
                    AktivtBehov(
                        behov = row.string("behov"),
                        kilde = Kilde.entries.first { it.name.equals(row.string("kilde"), ignoreCase = true) },
                        opprettet = row.localDateTime("opprettet"),
                        status = row.string("status"),
                    )
                }.asList,
            )
        }

    data class AktivtBehov(
        val behov: String,
        val kilde: Kilde,
        val opprettet: java.time.LocalDateTime,
        val status: String,
    )

    private fun registrerGauge() {
        try {
            GaugeWithCallback
                .builder()
                .name("dp_behandling_aktive_behov")
                .help("Antall aktive (uløste) behov per behovtype og kilde")
                .labelNames("behov", "kilde")
                .callback { callback ->
                    try {
                        dbSession.session { session ->
                            session.run(
                                queryOf(
                                    // language=PostgreSQL
                                    """
                                    SELECT behov, kilde, COUNT(*) as antall
                                    FROM behandling_aktive_behov
                                    WHERE status = 'pending'
                                    GROUP BY behov, kilde
                                    """.trimIndent(),
                                ).map { row ->
                                    callback.call(
                                        row.long("antall").toDouble(),
                                        row.string("behov"),
                                        row.string("kilde"),
                                    )
                                }.asList,
                            )
                        }
                    } catch (_: Exception) {
                        // Ignorer feil ved scrape — DB kan være midlertidig utilgjengelig
                    }
                }.register()
        } catch (_: Exception) {
            // Allerede registrert (kan skje i tester)
        }
    }
}

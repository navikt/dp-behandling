package no.nav.dagpenger.behandling.mediator.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.mottak.SakRepository.Behandling
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType
import no.nav.dagpenger.regel.OpplysningsTyper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

internal class ArenaOppgaveMottak(
    rapidsConnection: RapidsConnection,
    private val sakRepository: SakRepositoryPostgres,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("op_type", "U") }
                validate { it.requireKey("pos") }
                validate { it.require("op_ts", JsonNode::asArenaDato) }
                validate { it.require("after.REG_DATO", JsonNode::asArenaDato) }
                validate { it.require("after.MOD_DATO", JsonNode::asArenaDato) }
                validate {
                    it.requireKey(
                        "after.SAK_ID",
                        "after.DESCRIPTION",
                        "after.OPPGAVETYPE_BESKRIVELSE",
                        "after.ENDRET_AV",
                    )
                }
                // Ignorer oppgaver som ikke er tildelt benk
                validate {
                    it.require("before.USERNAME") {
                        if (it.isNull) {
                            throw IllegalArgumentException("Oppgaven må være tildelt en benk")
                        }
                    }
                    it.require("after.USERNAME") {
                        if (it.isNull) {
                            throw IllegalArgumentException("Oppgaven må være tildelt en benk")
                        }
                    }
                }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val sakId = packet["after.SAK_ID"].toString()
        withLoggingContext("sakId" to sakId) {
            logger.info { "Mottok oppgave fra Arena, vurderer om noe skal avbrytes" }

            val behandling = sakRepository.finnBehandling(sakId.toInt())
            if (behandling == null) {
                logger.info { "Fant ingen behandling for sakId=$sakId, det er sannsynligvis en oppgaver som har blitt laget i Arena." }
                return
            }

            sikkerlogg.info { "Fant behandling for sakId=$sakId, pakke=${packet.toJson()}" }

            if (behandling.tilstand in tilstanderSomKanIgnoreres) {
                logger.info { "Behandling ${behandling.behandlingId} er allerede i tilstand ${behandling.tilstand}, ignorerer oppgave." }
                return
            }

            val beskrivelse = packet["after.OPPGAVETYPE_BESKRIVELSE"].asText()
            val endretAv = packet["after.ENDRET_AV"].asText()

            if (endretAv == "ARBLINJE") {
                logger.info { "Oppgaven er ikke tildelt en saksbehandler enda, ignorerer." }
                return
            }

            if (packet["after.USERNAME"].asText().length == 4) {
                logger.info { "Oppgaven er tildelt en benk. Skal ikke avbrytes." }
                return
            }

            logger.info {
                """
                |Publiserer avbrytmelding for ${behandling.behandlingId} i tilstand ${behandling.tilstand}, 
                |mottok oppgave av type=$beskrivelse
                """.trimMargin()
            }

            val avbrytMelding =
                JsonMessage.newMessage(
                    "avbryt_behandling",
                    mapOf(
                        "ident" to behandling.ident,
                        "behandlingId" to behandling.behandlingId.toString(),
                        "årsak" to "Oppgaven er endret i Arena",
                    ),
                )
            context.publish(behandling.ident, avbrytMelding.toJson())
        }
    }

    private companion object {
        private val tilstanderSomKanIgnoreres =
            setOf(
                TilstandType.Ferdig,
                TilstandType.Avbrutt,
            )
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.ArenaOppgaveMottak")
    }
}

private var arenaDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")

private fun JsonNode.asArenaDato(): LocalDateTime = asText().let { LocalDateTime.parse(it, arenaDateFormatter) }

interface SakRepository {
    fun finnBehandling(fagsakId: Int): Behandling?

    data class Behandling(
        val ident: String,
        val behandlingId: UUID,
        val tilstand: TilstandType,
    )
}

internal class SakRepositoryPostgres : SakRepository {
    override fun finnBehandling(fagsakId: Int): Behandling? =
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT bo.behandling_id, pb.ident, b.tilstand
                    FROM opplysningstabell
                             LEFT JOIN behandling_opplysninger bo ON opplysningstabell.opplysninger_id = bo.opplysninger_id
                             LEFT JOIN behandling b ON bo.behandling_id = b.behandling_id
                             LEFT JOIN person_behandling pb ON b.behandling_id = pb.behandling_id
                    WHERE type_uuid = :typeUuid
                      AND verdi_heltall = :fagsakId
                    """.trimIndent(),
                    mapOf("fagsakId" to fagsakId, "typeUuid" to OpplysningsTyper.FagsakIdId.uuid),
                ).map { row ->
                    Behandling(
                        row.string("ident"),
                        row.uuid("behandling_id"),
                        TilstandType.valueOf(row.string("tilstand")),
                    )
                }.asSingle,
            )
        }
}

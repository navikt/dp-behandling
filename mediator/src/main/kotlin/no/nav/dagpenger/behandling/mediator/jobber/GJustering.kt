package no.nav.dagpenger.behandling.mediator.jobber

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepository
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.grunnbelop.Regel
import no.nav.dagpenger.grunnbelop.forDato
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import no.nav.dagpenger.regel.OpplysningsTyper
import java.math.BigDecimal
import java.time.LocalDate

internal class GJustering(
    private val behandlingRepository: BehandlingRepository,
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.GJustering")

        private val åpneTilstander =
            setOf(
                Behandling.TilstandType.UnderBehandling,
                Behandling.TilstandType.TilGodkjenning,
                Behandling.TilstandType.ForslagTilVedtak,
                Behandling.TilstandType.Redigert,
                Behandling.TilstandType.TilBeslutning,
            )
    }

    fun startGjustering(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        rapid: MessageContext,
    ) {
        logger.info { "Starter G-justering for perioden $fraOgMed–$tilOgMed" }
        var antallRekjørt = 0
        var antallOmgjort = 0

        behandlingRepository.finnBehandlingerForGJustering(fraOgMed, tilOgMed) { behandling ->
            val tilstand = behandling.tilstand().first
            val ident = behandling.behandler.ident

            when (tilstand) {
                Behandling.TilstandType.Ferdig -> {
                    val nyttGrunnbeløp = finnNyttGrunnbeløp(fraOgMed)
                    val melding = lagOmgjøringsMelding(ident, fraOgMed, nyttGrunnbeløp)
                    rapid.publish(ident, melding.toJson())
                    antallOmgjort++
                    sikkerlogg.info {
                        "Publiserer omgjør_behandling for behandlingId=${behandling.behandlingId}, ident=$ident, nyttGrunnbeløp=$nyttGrunnbeløp"
                    }
                }

                in åpneTilstander -> {
                    val melding = lagRekjørMelding(ident, behandling)
                    rapid.publish(ident, melding.toJson())
                    antallRekjørt++
                    sikkerlogg.info { "Publiserer rekjør_behandling for behandlingId=${behandling.behandlingId}, ident=$ident" }
                }

                else -> {
                    logger.warn { "Ukjent tilstand $tilstand for behandling ${behandling.behandlingId} – hopper over" }
                }
            }
        }

        logger.info { "G-justering ferdig: $antallRekjørt rekjørt, $antallOmgjort omgjort" }
    }

    private fun lagRekjørMelding(
        ident: String,
        behandling: Behandling,
    ): JsonMessage =
        JsonMessage.newMessage(
            "rekjør_behandling",
            mapOf(
                "ident" to ident,
                "behandlingId" to behandling.behandlingId.toString(),
                "oppfriskOpplysningIder" to listOf(OpplysningsTyper.GrunnbeløpForGrunnlagId.uuid.toString()),
            ),
        )

    private fun lagOmgjøringsMelding(
        ident: String,
        fraOgMed: LocalDate,
        nyttGrunnbeløp: BigDecimal,
    ): JsonMessage =
        JsonMessage.newMessage(
            "omgjør_behandling",
            mapOf(
                "ident" to ident,
                "gjelderDato" to fraOgMed.toString(),
                "initialOpplysninger" to
                    listOf(
                        mapOf(
                            "opplysningstype" to OpplysningsTyper.GrunnbeløpForGrunnlagId.uuid.toString(),
                            "verdi" to nyttGrunnbeløp.toPlainString(),
                            "gyldigFraOgMed" to fraOgMed.toString(),
                        ),
                    ),
            ),
        )

    private fun finnNyttGrunnbeløp(fraOgMed: LocalDate): BigDecimal =
        getGrunnbeløpForRegel(Regel.Grunnlag)
            .forDato(fraOgMed)
            .verdi
}

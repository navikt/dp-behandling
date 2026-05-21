package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepository
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.ferietillegg.Behov.OpptjeningsårFerietillegg
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.forbruk
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal class BehovsløserForbruksdagerMottak(
    rapidsConnection: RapidsConnection,
    private val repository: BehandlingRepository,
) : River.PacketListener {
    private companion object {
        private val log = KotlinLogging.logger {}
        const val BEHOV = "AntallDagerForbrukt"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", listOf(BEHOV))
                    it.forbid("@løsning")
                }
                validate {
                    it.requireKey("ident", BEHOV, "$BEHOV.$OpptjeningsårFerietillegg")
                    it.interestedIn("@behovId")
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
        val fnr = packet["ident"].asString()
        val opptjeningsår = packet[BEHOV][OpptjeningsårFerietillegg].asInt()

        withLoggingContext(
            "behovId" to packet["@behovId"].asString(),
        ) {
            log.info { "Skal løse behov '$BEHOV'" }

            val kjeder = repository.hentBehandlinger(Ident(fnr))

            val sisteFerdigeBehandlingIHverKjede =
                kjeder.mapNotNull {
                    it.nesteSomKanBaseresPå
                }

            val antallForbruksdager = finnAntallDagerForbruk(sisteFerdigeBehandlingIHverKjede, opptjeningsår)

            val løsning =
                mapOf(
                    "verdi" to antallForbruksdager,
                    "gyldigFraOgMed" to LocalDate.of(opptjeningsår, 1, 1),
                )

            packet["@løsning"] =
                mapOf(
                    BEHOV to løsning,
                )

            log.info { "har løst behov '$BEHOV'" }
            context.publish(packet.toJson())
        }
    }
}

fun finnAntallDagerForbruk(
    behandlinger: List<Behandling>,
    opptjeningsår: Int,
): Long {
    val gyldighetsperiode =
        Gyldighetsperiode(
            LocalDate.of(opptjeningsår, 1, 1),
            LocalDate.of(opptjeningsår, 12, 31),
        )
    return behandlinger
        .asSequence()
        .flatMap { it.opplysninger().finnAlle(forbruk) }
        .filter { it.verdi }
        .mapNotNull { it.gyldighetsperiode.overlapp(gyldighetsperiode) }
        .sumOf { ChronoUnit.DAYS.between(it.fraOgMed, it.tilOgMed) + 1 }
}

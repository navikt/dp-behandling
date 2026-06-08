package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.mediator.repository.PersonRepository
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.regel.Behov
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt

internal class BehovsløserErUtestengtMottak(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
) : River.PacketListener {
    private companion object {
        private val log = KotlinLogging.logger {}
        val BEHOV = Behov.ErUtestengt
        val DATO_KEY = "$BEHOV.${Søknadstidspunkt.prøvingsdato.navn}"
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
                    it.requireKey("ident", BEHOV, DATO_KEY)
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
        val ident = packet["ident"].asText()
        val dato = packet[DATO_KEY].asLocalDate()

        withLoggingContext("behovId" to packet["@behovId"].asText()) {
            log.info { "Skal løse behov '$BEHOV'" }

            val erUtestengt = personRepository.erUtestengt(Ident(ident), dato)

            packet["@løsning"] = mapOf(BEHOV to mapOf("verdi" to erUtestengt))

            log.info { "Løste behov '$BEHOV' med erUtestengt=$erUtestengt" }
            context.publish(packet.toJson())
        }
    }
}

package no.nav.dagpenger.utestengning.mottak

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
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.regelverk.RettighetstidslinjeOppslag
import no.nav.dagpenger.utestengning.RegelverkUtestengning

/**
 * Svarer ut behovet "ErUtestengt", som stilles av regelverk (f.eks. Dagpenger) som har et vilkår mot
 * utestengning. Selve behovsnavnet/dato-nøkkelen er kun en Kafka-kontrakt (en streng), og duplikeres
 * derfor her bevisst i stedet for å dra inn en avhengighet til modulen som stiller spørsmålet.
 */
class BehovsløserErUtestengtMottak(
    rapidsConnection: RapidsConnection,
    private val rettighetstidslinjeOppslag: RettighetstidslinjeOppslag,
) : River.PacketListener {
    private companion object {
        private val log = KotlinLogging.logger {}
        const val BEHOV = "ErUtestengt"
        const val DATO_KEY = "$BEHOV.Prøvingsdato"
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
        val ident = packet["ident"].asString()
        val dato = packet[DATO_KEY].asLocalDate()

        withLoggingContext("behovId" to packet["@behovId"].asString()) {
            log.info { "Skal løse behov '$BEHOV'" }

            val erUtestengt = rettighetstidslinjeOppslag.hent(Ident(ident)).harRett(RegelverkUtestengning, dato)

            packet["@løsning"] = mapOf(BEHOV to mapOf("verdi" to erUtestengt))

            log.info { "Løste behov '$BEHOV' med erUtestengt=$erUtestengt" }
            context.publish(packet.toJson())
        }
    }
}

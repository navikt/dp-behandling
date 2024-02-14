package no.nav.dagpenger.vedtak.mediator.mottak

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.vedtak.mediator.HendelseMediator
import no.nav.dagpenger.vedtak.mediator.IHendelseMediator
import no.nav.dagpenger.vedtak.mediator.melding.HendelseMessage
import no.nav.dagpenger.vedtak.modell.hendelser.OpplysningSvar
import no.nav.dagpenger.vedtak.modell.hendelser.OpplysningSvar.Tilstand
import no.nav.dagpenger.vedtak.modell.hendelser.OpplysningSvarHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate

internal class OpplysningSvarMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: HendelseMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("ident") }
            validate { it.requireKey("@løsning") }
            validate { it.requireKey("behandlingId") }
            validate { it.interestedIn("@id", "@opprettet") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info { "Mottok svar på en opplysning" }
        val message = OpplysningSvarMessage(packet)
        message.behandle(hendelseMediator, context)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        logger.error { problems }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal class OpplysningSvarMessage(private val packet: JsonMessage) : HendelseMessage(packet) {
    private val hendelse
        get() =
            OpplysningSvarHendelse(
                id,
                ident,
                behandlingId = packet["behandlingId"].asUUID(),
                opplysninger = opplysning,
            )
    override val ident get() = packet["ident"].asText()

    private val logger = KotlinLogging.logger {}

    private val opplysning =
        mutableListOf<OpplysningSvar<*>>().apply {
            packet["@løsning"].fields().forEach { (typeNavn, verdi) ->
                val type = Opplysningstype.typer.single { sadf -> sadf.id == typeNavn }
                val opplysning =
                    @Suppress("UNCHECKED_CAST")
                    when (type.datatype) {
                        Dato -> opplysningSvar(type as Opplysningstype<LocalDate>, verdi.asLocalDate())
                        Heltall -> opplysningSvar(type as Opplysningstype<Int>, verdi.asInt())
                        Desimaltall -> opplysningSvar(type as Opplysningstype<Double>, verdi.asDouble())
                        Boolsk -> opplysningSvar(type as Opplysningstype<Boolean>, verdi.asBoolean())
                    }
                add(opplysning)
            }
        }

    private fun <T : Comparable<T>> opplysningSvar(
        type: Opplysningstype<T>,
        verdi: T,
    ) = OpplysningSvar(
        opplysningstype = type,
        verdi = verdi,
        tilstand = Tilstand.Hypotese,
    )

    override fun behandle(
        mediator: IHendelseMediator,
        context: MessageContext,
    ) {
        withLoggingContext(hendelse.kontekstMap()) {
            logger.info { "Behandler svar på opplysning" }
            mediator.behandle(hendelse, this, context)
        }
    }
}

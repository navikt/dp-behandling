package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.mediator.asUUID
import no.nav.dagpenger.mediator.melding.HåndterbarKafkaMelding
import no.nav.dagpenger.modell.hendelser.EndreOpplysningHendelse
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.OpplysningIkkeFunnetException
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerbegrunnelse
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import java.time.LocalDateTime
import java.util.UUID

internal class EndreOpplysningMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: IMessageMediator,
    private val opplysningstyper: Set<Opplysningstype<*>>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAllOrAny("@behov", listOf("EndreOpplysning")) }
                precondition { it.requireValue("@final", true) }
                validate { it.requireKey("ident", "behandlingId", "opplysningId", "behovId", "saksbehandler", "begrunnelse") }
                validate { it.interestedIn("@id", "@opprettet", "verdi", "gyldigFraOgMed", "gyldigTilOgMed") }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val opplysningId = packet["opplysningId"].asUUID()
        val behovId = packet["behovId"].asString()

        withLoggingContext(
            "behovId" to behovId,
            "behandlingId" to behandlingId.toString(),
        ) {
            logger.info { "Mottok behov for å endre opplysning $opplysningId" }

            val opplysningstype =
                opplysningstyper.singleOrNull { it.behovId == behovId }
                    ?: throw IllegalArgumentException("Ukjent opplysningstype: $behovId")

            try {
                EndreOpplysningMessage(packet, opplysningstype).behandle(messageMediator, context)
            } catch (e: OpplysningIkkeFunnetException) {
                logger.warn(
                    e,
                ) { "Opplysning med id=$opplysningId ble ikke funnet og kan derfor ikke endres for behandlingId=$behandlingId" }
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal class EndreOpplysningMessage(
    packet: JsonMessage,
    opplysningstype: Opplysningstype<*>,
) : HåndterbarKafkaMelding(packet) {
    override val ident: String = packet["ident"].asString()
    val behovId: String = opplysningstype.behovId

    private val hendelse =
        EndreOpplysningHendelse(
            meldingsreferanseId = UUID.fromString(packet.id),
            ident = ident,
            behandlingId = packet["behandlingId"].asUUID(),
            opplysningId = packet["opplysningId"].asUUID(),
            verdi = packet["verdi"].takeUnless { it.isMissingNode || it.isNull }?.let { JsonMapper(it).map(opplysningstype.datatype) },
            gyldighetsperiode = packet.gyldighetsperiode(),
            kilde =
                Saksbehandlerkilde(
                    meldingsreferanseId = UUID.fromString(packet.id),
                    opprettet = packet["@opprettet"].asLocalDateTime(),
                    saksbehandler = Saksbehandler(packet["saksbehandler"].asString()),
                    begrunnelse = Saksbehandlerbegrunnelse(packet["begrunnelse"].asString()),
                ),
            opprettet = LocalDateTime.now(),
        )

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }

    private companion object {
        fun JsonMessage.gyldighetsperiode(): Gyldighetsperiode? {
            val fraOgMed = this["gyldigFraOgMed"].takeUnless { it.isMissingNode || it.isNull }?.asLocalDate() ?: return null
            val tilOgMed = this["gyldigTilOgMed"].takeUnless { it.isMissingNode || it.isNull }?.asLocalDate()
            return tilOgMed?.let { Gyldighetsperiode(fraOgMed, it) } ?: Gyldighetsperiode(fraOgMed)
        }
    }
}

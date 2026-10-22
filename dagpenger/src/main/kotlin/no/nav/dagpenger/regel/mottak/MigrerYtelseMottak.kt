package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.modell.hendelser.ManuellId
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Systemaktør
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.hendelse.MigrertYtelse
import no.nav.dagpenger.regel.hendelse.Tilleggsopplysning
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.melding.KafkaMelding
import no.nav.dagpenger.uuid.UUIDv7
import tools.jackson.databind.JsonNode
import java.math.BigDecimal

class MigrerYtelseMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMottaker: HendelseMottaker,
    private val opplysningstyper: Set<Opplysningstype<*>>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "migrer_ytelse") }
                validate {
                    it.requireKey(
                        "ident",
                        "innvilgetFraOgMed",
                        "innvilgedeStønadsuker",
                        "gjenståendeStønadsdager",
                        "dagsats",
                        "antallBarn",
                        "vanligArbeidstidPerUke",
                        "grunnlag",
                    )
                    it.interestedIn("innvilgetTilOgMed", "egenandel", "kravTilArbeidstidsreduksjon", "opplysninger")
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
        logger.info { "Mottok melding om migrering av ytelse" }
        val message = MigrerYtelseMessage(packet, opplysningstyper)
        hendelseMottaker.behandle(message.hendelse, message, context)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.error { problems }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

class MigrerYtelseMessage(
    packet: JsonMessage,
    opplysningstyper: Set<Opplysningstype<*>>,
) : KafkaMelding(packet) {
    override val ident: String = packet["ident"].asString()

    internal val hendelse =
        MigrerYtelseHendelse(
            meldingsreferanseId = packet.id.toUUID(),
            ident = ident,
            eksternId = ManuellId(UUIDv7.ny()),
            opprettet = opprettet,
            ytelse =
                MigrertYtelse(
                    innvilgetFraOgMed = packet["innvilgetFraOgMed"].asLocalDate(),
                    innvilgetTilOgMed = packet["innvilgetTilOgMed"].asOptionalLocalDate(),
                    innvilgedeStønadsuker = packet["innvilgedeStønadsuker"].asInt(),
                    gjenståendeStønadsdager = packet["gjenståendeStønadsdager"].asInt(),
                    dagsats = packet["dagsats"].asString().toBigDecimal(),
                    antallBarn = packet["antallBarn"].asInt(),
                    vanligArbeidstidPerUke = packet["vanligArbeidstidPerUke"].asDouble(),
                    grunnlag = packet["grunnlag"].asText().toBigDecimal(),
                    egenandel =
                        packet["egenandel"].takeUnless { it.isMissingNode || it.isNull }?.asText()?.toBigDecimal() ?: BigDecimal.ZERO,
                    kravTilArbeidstidsreduksjon =
                        packet["kravTilArbeidstidsreduksjon"].takeUnless { it.isMissingNode || it.isNull }?.asDouble() ?: 50.0,
                ),
            tilleggsopplysninger = packet["opplysninger"].tilleggsopplysninger(opplysningstyper),
            opprettetAv = Systemaktør.dpSak,
        )
}

// Format: { "<behovId>": verdi }. Kun enkle datatyper støttes.
private fun JsonNode.tilleggsopplysninger(opplysningstyper: Set<Opplysningstype<*>>): List<Tilleggsopplysning<*>> {
    if (isMissingNode || isNull) return emptyList()
    require(isObject) { "opplysninger må være et objekt med behovId som nøkkel" }
    return properties().map { (behovId, verdi) ->
        val type =
            opplysningstyper.singleOrNull { it.behovId == behovId }
                ?: throw IllegalArgumentException("Ukjent opplysningstype: $behovId")
        type.tilleggsopplysning(verdi)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> Opplysningstype<T>.tilleggsopplysning(verdi: JsonNode): Tilleggsopplysning<T> {
    fun JsonNode.krev(
        gyldig: Boolean,
        forventet: String,
    ) = require(gyldig) { "Opplysning $behovId må være $forventet" }

    val konvertert: Any =
        when (datatype) {
            Boolsk -> verdi.also { it.krev(it.isBoolean, "boolsk") }.asBoolean()
            Heltall -> verdi.also { it.krev(it.isIntegralNumber, "heltall") }.asInt()
            Desimaltall -> verdi.also { it.krev(it.isNumber, "tall") }.asDouble()
            Penger -> verdi.also { it.krev(it.isNumber || it.isString, "beløp") }.asString().let { Beløp(it.toBigDecimal()) }
            Dato -> verdi.also { it.krev(it.isString, "dato (ISO-8601)") }.asLocalDate()
            Tekst -> verdi.also { it.krev(it.isString, "tekst") }.asString()
            else -> throw IllegalArgumentException("Opplysning $behovId har datatype ${datatype.javaClass.simpleName} som ikke støttes")
        }
    return Tilleggsopplysning(this, konvertert as T)
}

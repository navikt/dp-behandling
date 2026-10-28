package no.nav.dagpenger.regel.mottak

import com.fasterxml.jackson.annotation.JsonProperty
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
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Systemaktør
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.hendelse.MigrertYtelse
import no.nav.dagpenger.regel.hendelse.Tilleggsopplysning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.Egenandel
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.melding.KafkaMelding
import no.nav.dagpenger.uuid.UUIDv7
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

private val migreringSvarSerializer = jsonMapper {}

internal data class Migreringsavbrudd(
    val kode: String,
    val melding: String,
    val detaljer: List<String> = emptyList(),
)

internal data class MigreringAvbruttDto(
    val sakId: Int,
    val arsak: Migreringsavbrudd,
) {
    @get:JsonProperty("@event_name")
    val eventName = "migrering_avbrutt"

    @get:JsonProperty("@id")
    val id = UUID.randomUUID().toString()

    @get:JsonProperty("@opprettet")
    val opprettet = LocalDateTime.now().toString()
}

internal data class TekniskMigreringFullførtDto(
    val sakId: Int,
) {
    @get:JsonProperty("@event_name")
    val eventName = "migrering_behandling_opprettet"

    @get:JsonProperty("@id")
    val id = UUID.randomUUID().toString()

    @get:JsonProperty("@opprettet")
    val opprettet = LocalDateTime.now().toString()
}

internal fun MessageContext.avbrytMigrering(
    sakId: Int,
    arsak: Migreringsavbrudd,
) {
    publish(migreringSvarSerializer.writeValueAsString(MigreringAvbruttDto(sakId, arsak)))
}

internal fun MessageContext.fullførMigrering(sakId: Int) {
    publish(migreringSvarSerializer.writeValueAsString(TekniskMigreringFullførtDto(sakId)))
}

class MigrerYtelseMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMottaker: HendelseMottaker,
    private val opplysningstyper: Set<Opplysningstype<*>>,
) : River.PacketListener {
    private val påkrevdeOpplysningstyper =
        setOf(
            Alderskrav.fødselsdato,
            Dagpengegrunnlag.grunnlag,
            Opphold.oppfyllerKravet,
            Dagpengeperiode.ordinærPeriode,
            Dagpengeperiode.antallStønadsdager,
            DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg,
            DagpengenesStørrelse.antallBarn,
            Vanligarbeidstid.fastsattVanligArbeidstid,
            Egenandel.egenandel,
            TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon,
            Samordning.samordnetDagsats,
            Samordning.skalSamordnes,
            DagpengenesStørrelse.harSamordnet,
            Samordning.utfallEtterSamordning,
            SamordingUtenforFolketrygden.dagsatsSamordnetUtenforFolketrygden,
            SamordingUtenforFolketrygden.andreYtelser,
        )

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("migrering_startet")) }
                validate {
                    it.requireKey(
                        "ident",
                        "sakId",
                        "innvilgetFraOgMed",
                    )
                    it.interestedIn("innvilgetTilOgMed", "initiertAv", "opplysninger")
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
        val sakId = packet["sakId"].asInt()
        if (sakId == 13699878) {
            logger.info { "Skipper migrering med sakId $sakId" }
            context.avbrytMigrering(
                sakId,
                Migreringsavbrudd(
                    kode = "SKIPPER_MIGRERING",
                    melding = "Hopper over migrering vi ikke klarte å behandle",
                ),
            )
            return
        }

        val manglendeOpplysninger = validerOpplysninger(packet)

        if (manglendeOpplysninger.isNotEmpty()) {
            logger.info { "Migrering av sakId $sakId mangler opplysninger ${manglendeOpplysninger.map { it.behovId }}" }
            context.avbrytMigrering(
                sakId,
                Migreringsavbrudd(
                    kode = "MANGLENDE_OPPLYSNINGER",
                    melding = "Mangler påkrevde opplysninger",
                    detaljer = manglendeOpplysninger.map { it.behovId },
                ),
            )
            return
        }

        val message = MigrerYtelseMessage(packet, opplysningstyper)
        hendelseMottaker.behandle(message.hendelse, message, context)
        logger.info { "Migrering av sakId $sakId opprettet behandling ok" }
        context.fullførMigrering(sakId)
    }

    fun validerOpplysninger(packet: JsonMessage): List<Opplysningstype<*>> {
        val mottatteTyper =
            packet["opplysninger"]
                .tilleggsopplysninger(opplysningstyper)
                .map { it.opplysningstype }
                .toSet()
        return påkrevdeOpplysningstyper.filterNot { it in mottatteTyper }
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
                    sakId = packet["sakId"].asInt(),
                ),
            tilleggsopplysninger = packet["opplysninger"].tilleggsopplysninger(opplysningstyper),
            opprettetAv = packet["initiertAv"].asStringOpt().getOrNull()?.let { Saksbehandler(it) } ?: Systemaktør.dpSak,
        )
}

internal fun JsonNode.tilleggsopplysninger(opplysningstyper: Set<Opplysningstype<*>>): List<Tilleggsopplysning<*>> {
    if (isMissingNode || isNull) return emptyList()
    return when {
        isObject -> {
            properties().toList().map { (behovId, verdi) ->
                val type =
                    opplysningstyper.singleOrNull { it.behovId == behovId }
                        ?: throw IllegalArgumentException("Ukjent opplysningstype: $behovId")
                type.tilleggsopplysning(verdi)
            }
        }

        isArray -> {
            values().map { it.tilleggsopplysning(opplysningstyper) }
        }

        else -> {
            throw IllegalArgumentException("opplysninger må være et objekt eller en liste")
        }
    }
}

private fun JsonNode.tilleggsopplysning(opplysningstyper: Set<Opplysningstype<*>>): Tilleggsopplysning<*> {
    require(isObject) { "Hver opplysning må være et objekt" }
    val uuid =
        get("uuid")
            ?.takeIf { it.isString }
            ?.asString()
            ?.let(UUID::fromString)
            ?: throw IllegalArgumentException("Opplysning mangler gyldig uuid")
    val type =
        opplysningstyper.singleOrNull { it.id.uuid == uuid }
            ?: throw IllegalArgumentException("Ukjent opplysningstype: $uuid")
    val verdi = get("verdi") ?: throw IllegalArgumentException("Opplysning $uuid mangler verdi")
    require(!verdi.isNull) { "Opplysning $uuid mangler verdi" }
    val gyldigFraOgMed =
        get("gyldigFraOgMed")
            ?.takeIf { it.isString }
            ?.asLocalDate()
            ?: throw IllegalArgumentException("Opplysning $uuid mangler gyldig gyldigFraOgMed")
    val gyldigTilOgMed =
        get("gyldigTilOgMed")
            ?.takeUnless { it.isMissingNode || it.isNull }
            ?.also { require(it.isString) { "Opplysning $uuid har ugyldig gyldigTilOgMed" } }
            ?.asLocalDate()
            ?: LocalDate.MAX
    return type.tilleggsopplysning(verdi, Gyldighetsperiode(gyldigFraOgMed, gyldigTilOgMed))
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> Opplysningstype<T>.tilleggsopplysning(
    verdi: JsonNode,
    gyldighetsperiode: Gyldighetsperiode? = null,
): Tilleggsopplysning<*> {
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
    return Tilleggsopplysning(this, konvertert as T, gyldighetsperiode)
}

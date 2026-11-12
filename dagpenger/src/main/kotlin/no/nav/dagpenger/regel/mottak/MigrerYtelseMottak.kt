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
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.modell.hendelser.MigreringId
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Regelkjøringsrapport
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Systemaktør
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.hendelse.MigrertYtelse
import no.nav.dagpenger.regel.hendelse.Tilleggsopplysning
import no.nav.dagpenger.regel.prosess.Migreringsprosess
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.melding.KafkaMelding
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import no.nav.dagpenger.inntekt.v1.Inntekt as InntektV1

private val migreringSvarSerializer = jsonMapper { addModule(kotlinModule()) }

class MigrerYtelseMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMottaker: HendelseMottaker,
    private val opplysningstyper: Set<Opplysningstype<*>>,
    private val simulator: (MigrerYtelseHendelse, Opplysninger) -> Regelkjøringsrapport = { hendelse, faktagrunnlag ->
        hendelse.forretningsprosess.simulerMigrering(faktagrunnlag)
    },
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("migrering_startet")) }
                validate {
                    it.requireKey(
                        "ident",
                        "sakId",
                        "migreringTidspunkt",
                        "migreringId",
                        "opplysninger",
                    )
                    it.interestedIn("innvilgetTilOgMed", "initiertAv", "simulering")
                    it.forbid("manglendeOpplysninger", "rapport", "simulerteOpplysninger")
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
        if (sakId == 13820459) {
            logger.info { "Skipper migrering med sakId $sakId" }
            return
        }

        val message = MigrerYtelseMessage(packet, opplysningstyper)
        val faktagrunnlag = message.hendelse.lagFaktagrunnlag()
        val simulering = packet["simulering"].asBoolean(false)

        val rapport =
            if (simulering) {
                try {
                    simulator(message.hendelse, faktagrunnlag)
                } catch (_: Exception) {
                    logger.error { "Simulering av migrering feilet" }
                    packet["simulerteOpplysninger"] = emptyList<Any>()
                    context.publish(packet.toJson())
                    return
                }
            } else {
                message.hendelse.forretningsprosess
                    .regelkjøring(faktagrunnlag)
                    .evaluer()
            }

        val manglendeOpplysninger =
            rapport.mangler
                .distinctBy { it.id.uuid }
                .map { ManglendeMigreringsopplysning(it.id.uuid.toString(), it.navn) }
                .sortedBy { it.uuid }

        if (!rapport.erFerdig() || manglendeOpplysninger.isNotEmpty()) {
            logger.info { "Migrering av sakId $sakId mangler opplysninger $manglendeOpplysninger" }
            packet["manglendeOpplysninger"] =
                manglendeOpplysninger
            context.publish(packet.toJson())
            return
        }

        if (simulering) {
            logger.info { "Migrering skal kun simuleres" }
            packet["simulerteOpplysninger"] =
                faktagrunnlag.simulerteOpplysninger()
            context.publish(packet.toJson())
            return
        }

        hendelseMottaker.behandle(message.hendelse, message, context)
        logger.info { "Migrering av sakId $sakId sendt start behandling hendelse" }
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

    val migreringTidspunkt = packet["migreringTidspunkt"].asLocalDate()
    private val tilleggsopplysninger = packet["opplysninger"].tilleggsopplysninger(migreringTidspunkt, opplysningstyper)

    internal val hendelse =
        MigrerYtelseHendelse(
            meldingsreferanseId = packet.id.toUUID(),
            ident = ident,
            eksternId = MigreringId(packet["migreringId"].asString()),
            opprettet = opprettet,
            ytelse =
                MigrertYtelse(
                    migreringTidspunkt = packet["migreringTidspunkt"].asLocalDate(),
                    innvilgetTilOgMed = packet["innvilgetTilOgMed"].asOptionalLocalDate(),
                    sakId = packet["sakId"].asInt(),
                ),
            tilleggsopplysninger = tilleggsopplysninger,
            opprettetAv = packet["initiertAv"].asStringOpt().getOrNull()?.let { Saksbehandler(it) } ?: Systemaktør.dpSak,
        )
}

internal data class ManglendeMigreringsopplysning(
    val uuid: String,
    val navn: String,
)

internal fun JsonNode.tilleggsopplysninger(
    migreringTidspunkt: LocalDate,
    opplysningstyper: Set<Opplysningstype<*>>,
): List<Tilleggsopplysning<*>> {
    require(isArray) { "opplysninger må være en liste" }
    return values().map { it.tilleggsopplysning(migreringTidspunkt, opplysningstyper) }
}

private fun JsonNode.tilleggsopplysning(
    migreringTidspunkt: LocalDate,
    opplysningstyper: Set<Opplysningstype<*>>,
): Tilleggsopplysning<*> {
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
    val erMigreringssaldo = type in Migreringsprosess.migreringssaldoer
    val saldodato = migreringTidspunkt.minusDays(1)
    val gyldigFraOgMed =
        get("gyldigFraOgMed")
            ?.takeIf { it.isString }
            ?.asLocalDate()
            ?: if (erMigreringssaldo) saldodato else migreringTidspunkt
    val gyldigTilOgMed =
        get("gyldigTilOgMed")
            ?.takeUnless { it.isMissingNode || it.isNull }
            ?.also { require(it.isString) { "Opplysning $uuid har ugyldig gyldigTilOgMed" } }
            ?.asLocalDate()
            ?: if (erMigreringssaldo) saldodato else LocalDate.MAX
    if (erMigreringssaldo) {
        require(gyldigTilOgMed == saldodato) { "Opplysning $uuid har ugyldig saldoperiode" }
    }
    val opplysning = type.tilleggsopplysning(verdi, Gyldighetsperiode(gyldigFraOgMed, gyldigTilOgMed))
    if (erMigreringssaldo) {
        when (val saldo = opplysning.verdi) {
            is Int -> require(saldo >= 0) { "Opplysning $uuid har ugyldig saldo" }
            is Beløp -> require(saldo >= Beløp(0)) { "Opplysning $uuid har ugyldig saldo" }
            is LocalDate -> require(saldo.isBefore(migreringTidspunkt)) { "Opplysning $uuid har ugyldig forbruksdato" }
        }
    }
    return opplysning
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
            Boolsk -> {
                verdi.also { it.krev(it.isBoolean, "boolsk") }.asBoolean()
            }

            Heltall -> {
                verdi.also { it.krev(it.isIntegralNumber, "heltall") }.asInt()
            }

            Desimaltall -> {
                verdi.also { it.krev(it.isNumber, "tall") }.asDouble()
            }

            Penger -> {
                verdi.also { it.krev(it.isNumber || it.isString, "beløp") }.asString().let { Beløp(it.toBigDecimal()) }
            }

            Dato -> {
                verdi.also { it.krev(it.isString, "dato (ISO-8601)") }.asLocalDate()
            }

            Tekst -> {
                verdi.also { it.krev(it.isString, "tekst") }.asString()
            }

            BarnDatatype -> {
                verdi.krev(verdi.isObject && verdi["barn"]?.isArray == true, "et objekt med en barneliste")
                migreringSvarSerializer.treeToValue(verdi, BarnListe::class.java)
            }

            InntektDataType -> {
                verdi.krev(verdi.isObject && verdi["inntektsListe"]?.isArray == true, "et inntektsobjekt med inntektsListe")
                Inntekt(
                    InntektV1(
                        inntektsId = verdi["inntektsId"].asString(),
                        sisteAvsluttendeKalenderMåned = YearMonth.parse(verdi["sisteAvsluttendeKalenderMåned"].asString()),
                        inntektsListe =
                            verdi["inntektsListe"].toList().map { måned ->
                                require(måned["klassifiserteInntekter"]?.isArray == true) { "Inntektsmåned mangler klassifiserteInntekter" }
                                KlassifisertInntektMåned(
                                    årMåned = YearMonth.parse(måned["årMåned"].asString()),
                                    klassifiserteInntekter =
                                        måned["klassifiserteInntekter"].toList().map { inntekt ->
                                            require(inntekt["beløp"]?.isNumber == true) { "Inntektsbeløp må være et tall" }
                                            KlassifisertInntekt(
                                                beløp = inntekt["beløp"].decimalValue(),
                                                inntektKlasse = InntektKlasse.valueOf(inntekt["inntektKlasse"].asString()),
                                            )
                                        },
                                    harAvvik = måned["harAvvik"].asBoolean(),
                                )
                            },
                        manueltRedigert = verdi["manueltRedigert"]?.asBoolean() ?: false,
                    ),
                )
            }

            else -> {
                throw IllegalArgumentException("Opplysning $behovId har datatype ${datatype.javaClass.simpleName} som ikke støttes")
            }
        }
    return Tilleggsopplysning(this, konvertert as T, gyldighetsperiode)
}

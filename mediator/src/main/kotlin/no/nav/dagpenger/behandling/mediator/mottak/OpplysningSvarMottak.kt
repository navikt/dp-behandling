package no.nav.dagpenger.behandling.mediator.mottak

import com.fasterxml.jackson.databind.JsonNode
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
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.OpplysningSvarBygger
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.barnMapper
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.mediator.mottak.SvarStrategi.Svar
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvar
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvar.Tilstand
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.DuplikateOpplysningerException
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.OpplysningIkkeFunnetException
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerbegrunnelse
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Ulid
import java.time.LocalDate
import java.util.UUID

internal class OpplysningSvarMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
    private val opplysningstyper: Set<Opplysningstype<*>>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireValue("@final", true) }
                precondition { it.requireValue("@opplysningsbehov", true) }
                validate { it.requireKey("ident") }
                validate { it.requireKey("@løsning") }
                validate { it.requireKey("behandlingId") }
                validate { it.interestedIn("@utledetAv") }
                validate { it.interestedIn("@standardverdi") }
                validate { it.interestedIn("@forventetPeriode") }
                validate { it.requireValue("@final", true) }
                validate { it.interestedIn("@id", "@opprettet", "@behovId") }
            }.register(this)
    }

    private val skipBehovId = emptyList<String>()
    private val skipBehandlingsId =
        listOf(
            "019d1a61-9502-7650-93eb-564b98af7af7",
        )

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behovId = packet["@behovId"].asText()
        val behandlingId = packet["behandlingId"].asUUID()
        addOtelAttributes(behovId, behandlingId)

        withLoggingContext(
            "behovId" to behovId.toString(),
            "behandlingId" to behandlingId.toString(),
        ) {
            if (skipBehovId.contains(behovId)) {
                logger.info { "Mottok svar på en opplysning som skal ignoreres" }
                return
            }
            if (skipBehandlingsId.contains(behandlingId.toString())) {
                logger.info { "Mottok svar på en behandling som skal ignoreres" }
                return
            }
            logger.info { "Mottok svar på en opplysning" }
            val message = OpplysningSvarMessage(packet, opplysningstyper)
            message.behandle(messageMediator, context)
        }
    }

    private fun addOtelAttributes(
        behovId: String,
        behandlingId: UUID,
    ) {
        Span.current().apply {
            setAttribute("app.river", name())
            setAttribute("app.behovId", behovId)
            setAttribute("app.behandlingId", behandlingId.toString())
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal class OpplysningSvarMessage(
    private val packet: JsonMessage,
    private val opplysningstyper: Set<Opplysningstype<out Any>>,
) : KafkaMelding(packet) {
    private val hendelse
        get() =
            OpplysningSvarHendelse(
                id,
                ident,
                behandlingId = packet["behandlingId"].asUUID(),
                opplysninger = opplysning,
                opprettet,
            )
    override val ident get() = packet["ident"].asText()

    private val logger = KotlinLogging.logger {}
    private val sikkerLogger = KotlinLogging.logger("tjenestekall.OpplysningSvarMessage")

    private val opplysning =
        mutableListOf<OpplysningSvar<*>>().apply {
            packet["@løsning"].properties().forEach { (typeNavn, løsning) ->
                logger.info { "Tok i mot opplysning av $typeNavn" }

                val opplysningstype =
                    runCatching { opplysningstyper.single { it.behovId == typeNavn || typeNavn in it.utgåtteBehovId } }.getOrElse {
                        throw IllegalArgumentException("Ukjent opplysningstype: $typeNavn")
                    }

                val svarer = lagSvarer(typeNavn, løsning)
                val kilde =
                    when (løsning.has("@kilde")) {
                        true -> {
                            val ident =
                                løsning["@kilde"]["saksbehandler"]?.asText() ?: throw IllegalArgumentException("Mangler saksbehandler")
                            Saksbehandlerkilde(
                                meldingsreferanseId = packet["@id"].asUUID(),
                                opprettet = packet["@opprettet"].asLocalDateTime(),
                                saksbehandler = Saksbehandler(ident),
                                begrunnelse = Saksbehandlerbegrunnelse(løsning["@kilde"]["begrunnelse"].asText()),
                            )
                        }

                        false -> {
                            Systemkilde(
                                meldingsreferanseId = packet["@id"].asUUID(),
                                opprettet = packet["@opprettet"].asLocalDateTime(),
                            )
                        }
                    }

                val utledetAv = packet["@utledetAv"][typeNavn]?.map { it.asUUID() } ?: emptyList()

                svarer.forEach { svar ->
                    logger.info {
                        "Løsning for opplysning $typeNavn med ${svarer.size} periode(r). " +
                            "Gyldighetsperiode=${svar.gyldighetsperiode}"
                    }
                    val opplysningSvarBygger =
                        OpplysningSvarBygger(
                            opplysningstype,
                            JsonMapper(typeNavn, svar.verdi),
                            kilde,
                            svar.tilstand,
                            svar.gyldighetsperiode,
                            utledetAv,
                        )
                    add(opplysningSvarBygger.opplysningSvar())
                }

                // Fyll hull med standardverdi for opplysningstyper som har det
                fyllHullMedStandardverdi(typeNavn, opplysningstype, svarer, kilde)?.let { addAll(it) }
            }
        }

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        withLoggingContext(hendelse.kontekstMap()) {
            logger.info { "Behandler svar på opplysninger: ${hendelse.opplysninger.map { it.opplysningstype.behovId }}" }
            try {
                mediator.behandle(hendelse, this, context)
            } catch (e: OpplysningIkkeFunnetException) {
                logger.error(e) {
                    "Kan ikke håndtere ${hendelse.javaClass.simpleName} fordi en opplysning ikke ble funnet"
                }
            } catch (e: DuplikateOpplysningerException) {
                logger.error(e) {
                    "Kan ikke håndtere ${hendelse.javaClass.simpleName} fordi det er duplikater av opplysningstyper i opplysningene."
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> fyllHullMedStandardverdi(
        typeNavn: String,
        opplysningstype: Opplysningstype<T>,
        svarer: List<Svar>,
        kilde: Kilde,
    ): List<OpplysningSvar<T>>? {
        val standardverdi = opplysningstype.standardverdi ?: return null
        val forventetPeriode = packet["@forventetPeriode"][typeNavn] ?: return null
        if (forventetPeriode.isMissingNode) return null

        val forventetFra = forventetPeriode["fraOgMed"]?.asLocalDate() ?: return null
        val forventetTil = forventetPeriode["tilOgMed"]?.asLocalDate() ?: return null

        // Svar uten eksplisitt gyldighetsperiode dekker hele forventet periode
        if (svarer.any { it.gyldighetsperiode == null }) return null

        val perioderMedSvar =
            svarer
                .mapNotNull { it.gyldighetsperiode }
                .sortedBy { it.fraOgMed }

        val hull = finnHull(forventetFra, forventetTil, perioderMedSvar)
        if (hull.isEmpty()) return null

        logger.info {
            "Fyller ${hull.size} hull med standardverdi for $typeNavn (${opplysningstype.behovId})"
        }

        return hull.map { (fra, til) ->
            OpplysningSvar(
                opplysningstype = opplysningstype,
                verdi = standardverdi,
                tilstand = Tilstand.Hypotese,
                kilde = kilde,
                gyldighetsperiode = Gyldighetsperiode(fra, til),
            )
        }
    }

    private companion object {
        private val svarStrategier = listOf(KomplekstSvar, ListeSvar, EnkeltSvar)

        fun lagSvarer(
            typeNavn: String,
            jsonNode: JsonNode,
        ): List<Svar> = svarStrategier.firstNotNullOf { it.svar(typeNavn, jsonNode) }

        fun finnHull(
            forventetFra: LocalDate,
            forventetTil: LocalDate,
            perioder: List<Gyldighetsperiode>,
        ): List<Pair<LocalDate, LocalDate>> {
            if (perioder.isEmpty()) return listOf(forventetFra to forventetTil)

            val hull = mutableListOf<Pair<LocalDate, LocalDate>>()
            var gjeldendeDato = forventetFra

            for (periode in perioder) {
                if (gjeldendeDato < periode.fraOgMed) {
                    hull.add(gjeldendeDato to periode.fraOgMed.minusDays(1))
                }
                if (periode.tilOgMed >= gjeldendeDato) {
                    // Guard mot overflow når tilOgMed er LocalDate.MAX eller forbi forventetTil
                    if (periode.tilOgMed >= forventetTil) {
                        gjeldendeDato = forventetTil.plusDays(1)
                        break
                    }
                    gjeldendeDato = periode.tilOgMed.plusDays(1)
                }
            }

            if (gjeldendeDato <= forventetTil) {
                hull.add(gjeldendeDato to forventetTil)
            }

            return hull
        }
    }
}

private fun interface SvarStrategi {
    fun svar(
        typeNavn: String,
        svar: JsonNode,
    ): List<Svar>?

    data class Svar(
        val verdi: JsonNode,
        val tilstand: Tilstand,
        val gyldigFraOgMed: LocalDate? = null,
        val gyldigTilOgMed: LocalDate? = null,
    ) {
        val gyldighetsperiode by lazy {
            if (gyldigFraOgMed != null && gyldigTilOgMed != null) {
                Gyldighetsperiode(gyldigFraOgMed, gyldigTilOgMed)
            } else if (gyldigFraOgMed != null && gyldigTilOgMed == null) {
                Gyldighetsperiode(gyldigFraOgMed)
            } else if (gyldigTilOgMed != null) {
                Gyldighetsperiode(tilOgMed = gyldigTilOgMed)
            } else {
                null
            }
        }
    }
}

private object KomplekstSvar : SvarStrategi {
    override fun svar(
        typeNavn: String,
        svar: JsonNode,
    ): List<Svar>? {
        if (!svar.isObject) return null
        return listOf(
            Svar(
                svar["verdi"],
                svar["status"]?.asText()?.let { Tilstand.valueOf(it) } ?: Tilstand.Faktum,
                svar["gyldigFraOgMed"]?.asLocalDate(),
                svar["gyldigTilOgMed"]?.asLocalDate(),
            ),
        )
    }
}

private object ListeSvar : SvarStrategi {
    override fun svar(
        typeNavn: String,
        svar: JsonNode,
    ): List<Svar>? {
        if (!svar.isArray) return null
        if (!svar.all { it.isObject && it.has("verdi") }) return null
        return svar.map { item ->
            Svar(
                item["verdi"],
                item["status"]?.asText()?.let { Tilstand.valueOf(it) } ?: Tilstand.Faktum,
                item["gyldigFraOgMed"]?.asLocalDate(),
                item["gyldigTilOgMed"]?.asLocalDate(),
            )
        }
    }
}

private object EnkeltSvar : SvarStrategi {
    override fun svar(
        typeNavn: String,
        svar: JsonNode,
    ): List<Svar>? {
        if (svar.isObject) return null
        return listOf(Svar(svar, Tilstand.Faktum))
    }
}

@Suppress("UNCHECKED_CAST")
private class JsonMapper(
    private val typeNavn: String,
    private val verdi: JsonNode,
) : OpplysningSvarBygger.VerdiMapper {
    override fun <T : Any> map(datatype: Datatype<T>): T =
        when (datatype) {
            Dato -> {
                verdi.asLocalDate() as T
            }

            Heltall -> {
                verdi.asInt() as T
            }

            Desimaltall -> {
                verdi.asDouble() as T
            }

            Boolsk -> {
                verdi.asBoolean() as T
            }

            ULID -> {
                Ulid(verdi.asText()) as T
            }

            Penger -> {
                Beløp(verdi.asText().toBigDecimal()) as T
            }

            BarnDatatype -> {
                barnMapper(typeNavn, verdi) as T
            }

            InntektDataType -> {
                Inntekt(
                    objectMapper.convertValue(verdi, no.nav.dagpenger.inntekt.v1.Inntekt::class.java),
                ) as T
            }

            Tekst -> {
                verdi.asText() as T
            }

            PeriodeDataType -> {
                TODO()
            }
        }
}

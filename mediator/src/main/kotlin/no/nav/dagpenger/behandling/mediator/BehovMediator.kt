package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse
import no.nav.dagpenger.aktivitetslogg.aktivitet.Behov

class BehovMediator {
    private companion object {
        val logger = KotlinLogging.logger { }
        val sikkerlogg = KotlinLogging.logger("tjenestekall.BehovMediator")
    }

    @WithSpan
    internal fun håndter(
        context: MessageContext,
        hendelse: AktivitetsloggHendelse,
    ) {
        hendelse.kontekster().forEach { if (!it.harFunksjonelleFeilEllerVerre()) håndter(context, it.behov()) }
    }

    private fun håndter(
        context: MessageContext,
        behov: List<Behov>,
    ) {
        behov
            .groupBy { it.kontekst() }
            .grupperBehovTilDetaljer()
            .forEach { (kontekst, behovMap) ->
                mutableMapOf<String, Any>()
                    .apply {
                        // Denne lar oss filtrere ut pakker fra andre system som ber om løsning på de samme behovene vi bruker
                        put("@opplysningsbehov", true)

                        // Legg til kontekst
                        putAll(kontekst)

                        // Legg til hvert behov + detaljer
                        behovMap.forEach { (behovNavn, detaljer) ->
                            val avhengigheter = detaljer.filterNot { it.key.startsWith("@") }
                            put(behovNavn, avhengigheter)
                        }

                        // TODO: Flat ut alle kontekster rett på root i behovet. Dette er for å være kompatibel med gamle behovløsere
                        @Suppress("UNCHECKED_CAST")
                        behovMap
                            .values
                            .forEach { behov ->
                                putAll(behov.filterNot { it.key == "@utledetAv" } as Map<String, Any>)
                            }

                        put("@utledetAv", behovMap.entries.associate { (behovNavn, detaljer) -> behovNavn to detaljer["@utledetAv"] })
                        put(
                            "@utledetAvRegelsett",
                            behovMap.entries.associate { (behovNavn, detaljer) -> behovNavn to detaljer["@utledetAvRegelsett"] },
                        )
                    }.let {
                        JsonMessage
                            .newNeed(behovMap.keys, it + erFinal(behovMap.size))
                            .also { message -> message.interestedIn("@behovId") }
                    }.also {
                        val behovId = it["@behovId"].asUUID().toString()
                        withLoggingContext("behovId" to behovId) {
                            sikkerlogg.info { "sender behov for ${behovMap.keys}:\n${it.toJson()}}" }
                            logger.info { "sender behov for ${behovMap.keys}" }

                            leggPåOtelTracing(behovId, behovMap)

                            context.publish(it.toJson())
                        }
                    }
            }
    }

    private fun erFinal(antallBehov: Int) =
        if (antallBehov == 1) {
            mapOf("@final" to true)
        } else {
            emptyMap()
        }

    private fun leggPåOtelTracing(
        behovId: String,
        behovMap: Map<String, Map<String, Any?>>,
    ) {
        val currentSpan = Span.current()
        currentSpan.setAttribute("app.behovId", behovId)
        behovMap.keys.forEach { behovNavn ->
            currentSpan.addEvent(
                "Publiserer behov",
                Attributes.of(AttributeKey.stringKey("behov"), behovNavn, AttributeKey.stringKey("behovId"), behovId),
            )
        }
    }

    private fun Map<Map<String, String>, List<Behov>>.grupperBehovTilDetaljer() =
        mapValues { (kontekst, behovliste) ->
            behovliste
                .groupBy({ it.type.name }, { it.detaljer() })
                .ikkeTillatUnikeDetaljerPåSammeBehov(kontekst, behovliste)
        }

    private fun <K : Any> Map<K, List<Map<String, Any?>>>.ikkeTillatUnikeDetaljerPåSammeBehov(
        kontekst: Map<String, String>,
        behovliste: List<Behov>,
    ) = mapValues { (_, detaljerList) ->
        // tillater duplikate detaljer-maps, så lenge de er like
        detaljerList
            .distinct()
            .also { detaljer ->
                require(detaljer.size == 1) {
                    sikkerlogg.error {
                        "Forsøkte å sende duplikate behov på kontekst " +
                            kontekst.entries.joinToString { "${it.key}=${it.value}" }
                        "Kan ikke produsere samme behov på samme kontekst med ulike detaljer. " +
                            "Forsøkte å be om ${behovliste.joinToString { it.type.name }}"
                    }
                }
            }.single()
    }
}

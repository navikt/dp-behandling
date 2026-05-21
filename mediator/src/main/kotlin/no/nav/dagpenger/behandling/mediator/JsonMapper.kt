package no.nav.dagpenger.behandling.mediator

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.blackbird.BlackbirdModule
import tools.jackson.module.kotlin.KotlinModule
import java.time.YearMonth
import no.nav.dagpenger.inntekt.v1.Inntekt as InntektV1

val objectMapper: ObjectMapper =
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(BlackbirdModule())
        .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
        .build()

fun InntektV1.tilJsonNode(): JsonNode =
    objectMapper.valueToTree(
        mapOf(
            "inntektsId" to inntektsId,
            "inntektsListe" to
                inntektsListe.map { måned ->
                    mapOf(
                        "årMåned" to måned.årMåned.toString(),
                        "klassifiserteInntekter" to
                            måned.klassifiserteInntekter.map { klassifisertInntekt ->
                                mapOf(
                                    "beløp" to klassifisertInntekt.beløp,
                                    "inntektKlasse" to klassifisertInntekt.inntektKlasse.name,
                                )
                            },
                        "harAvvik" to måned.harAvvik,
                    )
                },
            "manueltRedigert" to manueltRedigert,
            "sisteAvsluttendeKalenderMåned" to sisteAvsluttendeKalenderMåned.toString(),
        ),
    )

fun JsonNode.tilInntektV1(): InntektV1 =
    InntektV1(
        inntektsId = this["inntektsId"].asString(),
        inntektsListe =
            this["inntektsListe"].toList().map { måned ->
                KlassifisertInntektMåned(
                    årMåned = YearMonth.parse(måned["årMåned"].asString()),
                    klassifiserteInntekter =
                        måned["klassifiserteInntekter"].toList().map { klassifisertInntekt ->
                            KlassifisertInntekt(
                                beløp = klassifisertInntekt["beløp"].decimalValue(),
                                inntektKlasse = InntektKlasse.valueOf(klassifisertInntekt["inntektKlasse"].asString()),
                            )
                        },
                    harAvvik = måned["harAvvik"].asBoolean(),
                )
            },
        manueltRedigert = this["manueltRedigert"]?.takeUnless { it.isMissingNode || it.isNull }?.asBoolean() ?: false,
        sisteAvsluttendeKalenderMåned = YearMonth.parse(this["sisteAvsluttendeKalenderMåned"].asString()),
    )

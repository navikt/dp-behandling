package no.nav.dagpenger.mediator.api

import io.ktor.http.HttpStatusCode
import java.net.URI

sealed class BehandlingException(
    val httpStatus: HttpStatusCode,
    val type: URI,
    val title: String,
    val extensions: Map<String, Any?> = emptyMap(),
    message: String = title,
) : RuntimeException(message)

class UgyldigBehandlingstilstandException(
    nåværendeTilstand: String,
    operasjon: String,
) : BehandlingException(
        httpStatus = HttpStatusCode.Conflict,
        type = URI("urn:error:behandling:ugyldig_tilstand"),
        title = "Behandlingen er ikke i korrekt tilstand for denne operasjonen",
        extensions =
            mapOf(
                "nåværendeTilstand" to nåværendeTilstand,
                "operasjon" to operasjon,
            ),
    )

class RedigeringPågårException :
    BehandlingException(
        httpStatus = HttpStatusCode.Conflict,
        type = URI("urn:error:behandling:redigering_paagar"),
        title = "Behandlingen har en pågående redigering",
        extensions = mapOf("nåværendeTilstand" to "Redigert"),
    )

class BehandlingIkkeRedigerbareException(
    nåværendeTilstand: String,
) : BehandlingException(
        httpStatus = HttpStatusCode.Conflict,
        type = URI("urn:error:behandling:ikke_redigerbar"),
        title = "Behandlingen er ikke i redigerbar tilstand",
        extensions = mapOf("nåværendeTilstand" to nåværendeTilstand),
    )

class OpplysningIkkeRedigerbareException(
    opplysningstype: String,
) : BehandlingException(
        httpStatus = HttpStatusCode.UnprocessableEntity,
        type = URI("urn:error:behandling:opplysning_ikke_redigerbar"),
        title = "Opplysningstypen kan ikke redigeres",
        extensions = mapOf("opplysningstype" to opplysningstype),
    )

class UgyldigPeriodeException(
    gyldigFraOgMed: String?,
    gyldigTilOgMed: String?,
) : BehandlingException(
        httpStatus = HttpStatusCode.UnprocessableEntity,
        type = URI("urn:error:behandling:ugyldig_periode"),
        title = "Til og med dato kan ikke være før fra og med dato",
        extensions =
            mapOf(
                "gyldigFraOgMed" to gyldigFraOgMed,
                "gyldigTilOgMed" to gyldigTilOgMed,
            ),
    )

class PåfølgendePeriodeMåFjernesFørstException :
    BehandlingException(
        httpStatus = HttpStatusCode.Conflict,
        type = URI("urn:error:behandling:paafolgende_periode_maa_fjernes_foerst"),
        title = "De påfølgende periodene må fjernes først",
    )

class KanIkkeEndrePrøvingsdatoException :
    BehandlingException(
        httpStatus = HttpStatusCode.UnprocessableEntity,
        type = URI("urn:error:behandling:kan_ikke_endre_proevingsdato"),
        title = "Kan ikke endre prøvingsdato på en behandling som er basert på en tidligere behandling",
    )

class PrøvingsdatoFørSøknadsdatoException(
    prøvingsdato: String,
    søknadsdato: String,
) : BehandlingException(
        httpStatus = HttpStatusCode.UnprocessableEntity,
        type = URI("urn:error:behandling:proevingsdato_foer_soeknadsdato"),
        title = "Prøvingsdato kan ikke settes før søknadsdato",
        extensions =
            mapOf(
                "prøvingsdato" to prøvingsdato,
                "søknadsdato" to søknadsdato,
            ),
    )

package no.nav.dagpenger.behandling.helpers.scenario.assertions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate
import java.util.UUID

internal class BehandlingsresultatAssertions(
    val klump: JsonNode,
) {
    val basertPå: UUID? get() = klump["basertPå"]?.asUUID()
    val rettighetsperioder: List<Rettighetsperiode> = objectMapper.treeToValue(klump["rettighetsperioder"])
    val opplysninger: JsonNode = klump["opplysninger"]

    fun opplysninger(opplysningstype: Opplysningstype<*>): List<Opplysningsperiode> {
        val opplysninger =
            klump["opplysninger"].singleOrNull { it["opplysningTypeId"].asUUID() == opplysningstype.id.uuid } ?: return emptyList()

        return objectMapper.treeToValue(opplysninger["perioder"])
    }
}

internal data class Rettighetsperiode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
    val harRett: Boolean,
    val endret: Boolean,
)

internal data class Opplysningsperiode(
    val id: UUID,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val verdi: Opplysningsverdi,
    val status: Periodestatus,
) {
    data class Opplysningsverdi(
        val datatype: String,
        val verdi: Any,
    )

    enum class Periodestatus {
        Ny,
        Arvet,
    }
}

package no.nav.dagpenger.scenario.assertions

import no.nav.dagpenger.mediator.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.mediator.asUUID
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.opplysning.Opplysningstype
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.treeToValue
import java.time.LocalDate
import java.util.UUID

internal class BehandlingsresultatAssertions(
    private val klump: JsonNode,
) {
    val basertPå: UUID? get() = klump["basertPå"]?.asUUID()
    val behandlingId: UUID get() = klump["behandlingId"].asUUID()
    val behandlingskjedeId: UUID get() = klump["behandlingskjedeId"].asUUID()
    val rettighetsperioder: List<RettighetsperiodeDTO> = objectMapper.treeToValue(klump["rettighetsperioder"])
    val opplysninger: JsonNode = klump["opplysninger"]
    val utbetalinger: JsonNode = klump["utbetalinger"]
    val førteTil: String = klump["førteTil"].asString()
    val behandletHendelse: JsonNode = klump["behandletHendelse"]

    val utfall get() = rettighetsperioder.last().harRett

    fun opplysninger(
        opplysningstype: Opplysningstype<*>,
        block: List<Opplysningsperiode>.() -> Unit = {},
    ): List<Opplysningsperiode> {
        val opplysninger =
            klump["opplysninger"].singleOrNull { it["opplysningTypeId"].asUUID() == opplysningstype.id.uuid } ?: return emptyList()

        val opplysningsperioder = objectMapper.treeToValue<List<Opplysningsperiode>>(opplysninger["perioder"])

        block(opplysningsperioder)
        return opplysningsperioder
    }
}

internal data class Opplysningsperiode(
    val id: UUID,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
    val verdi: Opplysningsverdi,
    val opprinnelse: Periodestatus,
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

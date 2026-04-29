package no.nav.dagpenger.brev

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Trigger.Alltid::class, name = "alltid"),
    JsonSubTypes.Type(value = Trigger.Avgjørelse::class, name = "avgjørelse"),
    JsonSubTypes.Type(value = Trigger.OpplysningFinnes::class, name = "opplysning_finnes"),
    JsonSubTypes.Type(value = Trigger.OpplysningVerdi::class, name = "opplysning_verdi"),
)
sealed interface Trigger {
    data object Alltid : Trigger

    data class Avgjørelse(
        val avgjørelse: String,
    ) : Trigger

    data class OpplysningFinnes(
        val opplysningsTypeId: UUID,
        val kunNyeOpplysninger: Boolean = false,
        val periodeType: PeriodeType? = null,
    ) : Trigger

    data class OpplysningVerdi(
        val opplysningsTypeId: UUID,
        val forventetVerdi: String,
        val kunNyeOpplysninger: Boolean = false,
    ) : Trigger
}

/**
 * Beskriver periodestrukturen til en opplysning.
 * Brukes for å trigge ulike maltekster basert på om perioden er åpen, lukket, eller om det er flere perioder.
 */
enum class PeriodeType {
    /** Kun fraOgMed — åpen, løpende rett */
    ÅPEN,

    /** Én periode med både fraOgMed og tilOgMed */
    LUKKET,

    /** Flere perioder — saksbehandler må skrive selv */
    FLERE,
}

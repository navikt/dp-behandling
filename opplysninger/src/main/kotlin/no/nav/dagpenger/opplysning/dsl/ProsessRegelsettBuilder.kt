package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType

/**
 * Builder for prosess-regelsett.
 * Prosess-regelsett produserer opplysninger som ikke arves til neste behandling i kjeden.
 */
class ProsessRegelsettBuilder internal constructor(
    hjemmel: Hjemmel,
) : RegelsettBuilderBase(hjemmel, RegelsettType.Prosess) {
    private var ønsketResultat: List<Opplysningstype<*>> = emptyList()

    fun ønsketResultat(vararg opplysningstype: Opplysningstype<*>) {
        ønsketResultat = opplysningstype.toList()
    }

    override fun build() =
        Regelsett(
            hjemmel = hjemmel,
            type = type,
            ønsketResultat = ønsketResultat,
            regler = regler,
            avklaringer = avklaringer,
            utfall = null,
            skalKjøres = skalKjøres,
            skalRevurderes = skalRevurderes,
            påvirkerResultat = relevant,
            betingelser = emptyList(),
        )
}

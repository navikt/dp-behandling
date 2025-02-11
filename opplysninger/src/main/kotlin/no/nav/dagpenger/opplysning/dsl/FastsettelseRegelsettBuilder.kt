package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType

class FastsettelseRegelsettBuilder internal constructor(
    hjemmel: Hjemmel,
) : RegelsettBuilderBase(hjemmel, RegelsettType.Fastsettelse) {
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
            utfall = emptyList(),
            skalKjøres = skalKjøres,
            erRelevant = relevant,
        )
}

package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Fastsatt
import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType

class FastsettelseRegelsettBuilder internal constructor(
    hjemmel: Hjemmel,
) : RegelsettBuilderBase(hjemmel, RegelsettType.Fastsettelse) {
    private var ønsketResultat: List<Opplysningstype<*>> = emptyList()
    private var fastsattBuilder: Fastsatt.FastsattBuilder? = null

    fun ønsketResultat(vararg opplysningstype: Opplysningstype<*>) {
        ønsketResultat = opplysningstype.toList()
    }

    fun fastsattBuilder(block: Fastsatt.FastsattBuilder.() -> Unit) {
        fastsattBuilder = Fastsatt.FastsattBuilder().apply(block)
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
            påvirkerResultat = relevant,
            fastsattBuilder = fastsattBuilder,
        )
}

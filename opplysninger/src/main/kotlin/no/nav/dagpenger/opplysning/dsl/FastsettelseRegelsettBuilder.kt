package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Regelverkstype

class FastsettelseRegelsettBuilder<T : Regelverkstype> internal constructor(
    hjemmel: Hjemmel,
) : RegelsettBuilderBase<T>(hjemmel, RegelsettType.Fastsettelse) {
    private var ønsketResultat: List<Opplysningstype<*>> = emptyList()
    private var resultatbygger: VedtakBygger<T>.(Opplysninger) -> Unit = {}

    fun ønsketResultat(vararg opplysningstype: Opplysningstype<*>) {
        ønsketResultat = opplysningstype.toList()
    }

    override fun build() =
        Regelsett<T>(
            hjemmel = hjemmel,
            type = type,
            ønsketResultat = ønsketResultat,
            regler = regler,
            avklaringer = avklaringer,
            utfall = emptyList(),
            skalKjøres = skalKjøres,
            påvirkerResultat = relevant,
            resultatbygger = resultatbygger,
        )
}

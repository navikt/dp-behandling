package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class VilkårRegelsettBuilder internal constructor(
    hjemmel: Hjemmel,
) : RegelsettBuilderBase(hjemmel, RegelsettType.Vilkår) {
    private var utfall: MutableList<Opplysningstype<Boolean>> = mutableListOf()
    private val ønsketResultat: MutableList<Opplysningstype<*>> = mutableListOf()

    // TODO: Dette trengs kun av samordning. Kan vi endre noe her?
    fun ønsketResultat(vararg opplysningstype: Opplysningstype<*>) {
        ønsketResultat += opplysningstype.toList()
    }

    fun utfall(
        produserer: Opplysningstype<Boolean>,
        gjelderFraOgMed: LocalDate = LocalDate.MIN,
        block: Opplysningstype<Boolean>.() -> Regel<*>,
    ) {
        regel(produserer, gjelderFraOgMed, block)
        utfall.add(produserer)
    }

    override fun build() =
        Regelsett(
            hjemmel = hjemmel,
            type = type,
            // TODO: ønsketResultat burde være emptyList() når det vilkår
            ønsketResultat = ønsketResultat.toList(),
            regler = regler,
            avklaringer = avklaringer,
            utfall = utfall,
            skalKjøres = skalKjøres,
            erRelevant = relevant,
        )
}

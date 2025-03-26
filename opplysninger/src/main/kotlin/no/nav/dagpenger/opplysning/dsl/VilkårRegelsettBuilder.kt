package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.NoeGøyFraBehandlingen
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Regelverkstype
import no.nav.dagpenger.opplysning.Vedtak
import no.nav.dagpenger.opplysning.Vilkår
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class VilkårRegelsettBuilder<T : Regelverkstype> internal constructor(
    hjemmel: Hjemmel,
) : RegelsettBuilderBase<T>(hjemmel, RegelsettType.Vilkår) {
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
            påvirkerResultat = relevant,
            resultatbygger = resultatbygger,
        )

    private var resultatbygger: VedtakBygger<T>.(Opplysninger) -> Unit = {}

    fun resultat(block: VedtakBygger<T>.(Opplysninger) -> Unit) {
        resultatbygger = block
    }
}

class VedtakBygger<F : Regelverkstype>(
    val metadata: NoeGøyFraBehandlingen,
) {
    private val vilkår = mutableListOf<() -> Vilkår>()
    private val fastsettelse = mutableListOf<F.() -> F>()

    fun vilkårsvurdering(block: () -> Vilkår) {
        vilkår.add(block)
    }

    fun fastsettelse(block: F.() -> F) {
        fastsettelse.add(block)
    }

    fun bygg(regelverk: Regelverk<F>): Vedtak<F> {
        val b: List<Vilkår> =
            vilkår.map { vilkårbygger ->
                vilkårbygger()
            }
        return TODO()
    }
}

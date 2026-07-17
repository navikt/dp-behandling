package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class VilkårRegelsettBuilder internal constructor(
    hjemmel: Hjemmel,
) : RegelsettBuilderBase(hjemmel, RegelsettType.Vilkår) {
    private var utfall: Opplysningstype<Boolean>? = null
    private var vurderinger: MutableList<Opplysningstype<Boolean>> = mutableListOf()
    private val ønsketResultat: MutableList<Opplysningstype<*>> = mutableListOf()
    private var selvstendigSøknad: ((LesbarOpplysninger) -> Boolean)? = null

    fun ønsketResultat(vararg opplysningstype: Opplysningstype<*>) {
        ønsketResultat += opplysningstype.toList()
    }

    fun utfall(
        produserer: Opplysningstype<Boolean>,
        gjelderFraOgMed: LocalDate = LocalDate.MIN,
        block: Opplysningstype<Boolean>.() -> Regel<*>,
    ) {
        require(utfall == null) { "Kan kun ha ett utfall per vilkår" }
        regel(produserer, gjelderFraOgMed, block)

        utfall = produserer

        // TODO: Gjør dette som en midlertidig løsning for å få vurderinger til å oppføre seg som før alle plasser vi før brukte "utfall"
        vurderinger.add(produserer)
    }

    fun betingelse(
        produserer: Opplysningstype<Boolean>,
        gjelderFraOgMed: LocalDate = LocalDate.MIN,
        block: Opplysningstype<Boolean>.() -> Regel<*>,
    ) {
        regel(produserer, gjelderFraOgMed, block)
        vurderinger.add(produserer)
    }

    // Marker at dette vilkåret besvarer en selvstendig søknad fra bruker (jf. forvaltningsloven § 11 a),
    // slik at et negativt utfall skal gi avslag på nettopp den søknaden – uavhengig av om det
    // ellers skjer endringer i det løpende rettighetsforholdet. Se Regelsett.girAvslagPåSelvstendigSøknad.
    fun selvstendigSøknad(trigger: (LesbarOpplysninger) -> Boolean) {
        selvstendigSøknad = trigger
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
            skalKjøres = skalVurderes,
            skalRevurderes = skalRevurderes,
            påvirkerResultat = relevant,
            betingelser = vurderinger,
            kvoter = kvoter.toList(),
            selvstendigSøknad = selvstendigSøknad,
        )
}

package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.harLøpendeRettId

object KravPåDagpenger {
    val harLøpendeRett = boolsk(harLøpendeRettId, "Har løpende rett på dagpenger")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(0, 0, "Krav på dagpenger", "Krav på dagpenger"),
        ) {
            regel(harLøpendeRett) { somUtgangspunkt(true) }

            ønsketResultat(harLøpendeRett)
        }
}

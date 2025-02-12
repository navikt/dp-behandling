package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.dsl.vilkår

object Meldeplikt {
    val regelsett =
        vilkår(folketrygden.hjemmel(4, 8, "Meldeplikt og møteplikt", "Meldeplikt")) {
            skalVurderes { false }
            påvirkerResultat { false }
        }
}

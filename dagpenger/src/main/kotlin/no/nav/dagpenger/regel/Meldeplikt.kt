package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Regelsett

object Meldeplikt {
    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 8, "Meldeplikt og møteplikt", "Meldeplikt"),
        ) {
            skalKjøres { false }
            relevantHvis { false }
        }
}

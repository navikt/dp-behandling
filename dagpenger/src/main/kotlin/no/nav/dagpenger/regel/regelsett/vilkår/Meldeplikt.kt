package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.tomRegel
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerMeldepliktId
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett

object Meldeplikt {
    val oppfyllerMeldeplikt = boolsk(OppfyllerMeldepliktId, "Oppfyller meldeplikt")

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 8, "Meldeplikt og møteplikt", "Meldeplikt")) {
            skalVurderes { it.erSann(harLøpendeRett) && it.kunEgne.har(oppfyllerMeldeplikt) }

            utfall(oppfyllerMeldeplikt) { tomRegel }

            påvirkerResultat { it.har(oppfyllerMeldeplikt) && !it.erSann(oppfyllerMeldeplikt) }
        }
}

package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerMeldepliktId
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalEksportVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalVernepliktVurderes

object Meldeplikt {
    val oppfyllerMeldeplikt = boolsk(OppfyllerMeldepliktId, "Oppfyller meldeplikt")

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 8, "Meldeplikt og møteplikt", "Meldeplikt")) {
            skalVurderes { it.erSann(harLøpendeRett) || it.erSann(skalVernepliktVurderes) }

            utfall(oppfyllerMeldeplikt) { somUtgangspunkt(true, Søknadstidspunkt.søknadsdato) }

            påvirkerResultat {
                it.har(oppfyllerMeldeplikt) &&
                    !it.erSann(oppfyllerMeldeplikt) &&
                    // Når bruker har eksport skal ikke meldeplikt være
                    !it.erSann(skalEksportVurderes)
            }
        }
}

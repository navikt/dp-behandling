package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravTilGjenopptakId
import no.nav.dagpenger.regel.OpplysningsTyper.SkalBehandlesSomGjenopptakId
import no.nav.dagpenger.regel.OpplysningsTyper.SøkerOmGjenopptakId
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype

object Gjenopptak {
    var søkerOmGjenopptak = boolsk(SøkerOmGjenopptakId, beskrivelse = "Søker om gjenopptak", behovId = "Gjenopptak")
    var oppfyllerKravTilGjenopptak = boolsk(OppfyllerKravTilGjenopptakId, beskrivelse = "Oppfyller kravet til gjenopptak")
    var skalBehandlesSomGjenopptak = boolsk(SkalBehandlesSomGjenopptakId, beskrivelse = "Skal behandles som gjenopptak")

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 16, "Gjenopptak av løpende stønadsperiode", "Gjenopptak")) {
            skalVurderes { true }

            regel(søkerOmGjenopptak) { innhentMed(søknadIdOpplysningstype) }

            // Skal det gis avslag om gjenopptak eller er kravene oppfylt
            regel(oppfyllerKravTilGjenopptak) {
                // TODO: Finnes det noe å gjenoppta?
                erSann(søkerOmGjenopptak)
            }

            påvirkerResultat { true }
        }
}

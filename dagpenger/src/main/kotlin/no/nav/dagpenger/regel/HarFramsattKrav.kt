package no.nav.dagpenger.regel

import no.nav.dagpenger.dato.september
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt

object HarFramsattKrav {
    val harFramsattKrav = Opplysningstype.boolsk(OpplysningsTyper.harFramsattKravId, "Har framsatt krav om dagpenger")

    private val gjelderFra = 26.september(2025)
    val regelsett =
        vilkår(
            folketrygden.hjemmel(22, 13, "Har framsatt krav om dagpenger", "Har framsatt krav om dagpenger"),
        ) {
            skalVurderes {
                it.har(Søknadstidspunkt.søknadstidspunkt) &&
                    it.finnOpplysning(Søknadstidspunkt.søknadstidspunkt).verdi.isAfter(
                        gjelderFra,
                    )
            }
            utfall(harFramsattKrav, gjelderFra) { oppslag(Søknadstidspunkt.søknadstidspunkt) { true } }
        }
}

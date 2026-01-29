package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.framsattSøknadId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravTilFramsattSøknadId
import no.nav.dagpenger.regel.OpplysningsTyper.ønskerDagpengerId

object Søknad {
    val framsattSøknad = Opplysningstype.boolsk(framsattSøknadId, "Har satt frem skriftlig søknad")
    val ønskerDagpenger = Opplysningstype.boolsk(ønskerDagpengerId, "Ønsker dagpenger ved framsatt søknad")

    val oppfyllerKravetTilSøknad = Opplysningstype.boolsk(oppfyllerKravTilFramsattSøknadId, "Oppfyller kravet til framsatt søknad")

    val regelsett =
        vilkår(folketrygden.hjemmel(22, 13, "Frister for framsetting av krav, virkningstidspunkt og etterbetaling", "Søknad")) {
            regel(framsattSøknad) { somUtgangspunkt(true) }
            regel(ønskerDagpenger) { somUtgangspunkt(true) }

            utfall(oppfyllerKravetTilSøknad) { alle(framsattSøknad, ønskerDagpenger) }
        }
}

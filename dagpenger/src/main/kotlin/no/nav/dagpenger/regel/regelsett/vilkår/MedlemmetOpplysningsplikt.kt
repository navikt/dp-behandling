package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerOpplysningspliktId

object MedlemmetOpplysningsplikt {
    val oppfyllerOpplysningsplikt =
        boolsk(
            oppfyllerOpplysningspliktId,
            "Har gitt alle nødvendige opplysninger?",
        )

    val regelsett =
        vilkår(
            folketrygden.hjemmel(
                21,
                3,
                "Medlemmets opplysningsplikt",
                "Medlemmets opplysningsplikt",
            ),
        ) {

            utfall(oppfyllerOpplysningsplikt) { somUtgangspunkt(true) }
        }
}

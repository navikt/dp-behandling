package no.nav.dagpenger.utestengning

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.tomRegel
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.utestengning.OpplysningsTyper.erUtestengtId
import no.nav.dagpenger.utestengning.OpplysningsTyper.utestengningFraOgMedId
import no.nav.dagpenger.utestengning.OpplysningsTyper.utestengningTilOgMedId

/**
 * Regelsett for utestengning.
 * Definer regler for når og i hvilken periode en person skal utestenges.
 */
object UtestengningVilkår {
    val erUtestengt = boolsk(erUtestengtId, "Er personen utestengt")
    val fraOgMed = dato(utestengningFraOgMedId, "Utestengning gjelder fra og med")
    val tilOgMed = dato(utestengningTilOgMedId, "Utestengning gjelder til og med")

    val regelsett =
        vilkår(
            tomHjemmel("Utestengning"),
        ) {
            // TODO: Legg til regler for å bestemme erUtestengt, fraOgMed og tilOgMed
            regel(erUtestengt) { tomRegel }
            regel(fraOgMed) { tomRegel }
            regel(tilOgMed) { tomRegel }

            utfall(erUtestengt) { tomRegel }
        }
}

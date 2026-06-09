package no.nav.dagpenger.utestengning

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.dato.leggTilUker
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.utestengning.OpplysningsTyper.erUtestengtId
import no.nav.dagpenger.utestengning.OpplysningsTyper.utestengningFraOgMedId
import no.nav.dagpenger.utestengning.OpplysningsTyper.utestengningTilOgMedId
import no.nav.dagpenger.utestengning.OpplysningsTyper.utestengtLengdeId
import java.time.LocalDate

/**
 * Regelsett for utestengning.
 * Definer regler for når og i hvilken periode en person skal utestenges.
 */
object UtestengningVilkår {
    val erUtestengt = boolsk(erUtestengtId, "Er personen utestengt")
    val utestengtLengde = heltall(utestengtLengdeId, "Hvor mange uker skal utesteningen vare", enhet = Enhet.Uker)
    val fraOgMed = dato(utestengningFraOgMedId, "Utestengning gjelder fra og med")
    val tilOgMed = dato(utestengningTilOgMedId, "Utestengning gjelder til og med")

    val regelsett =
        vilkår(
            tomHjemmel("Utestengning"),
        ) {
            regel(utestengtLengde) { somUtgangspunkt(12) }
            regel(fraOgMed) { somUtgangspunkt(LocalDate.now()) }
            regel(tilOgMed) { leggTilUker(fraOgMed, utestengtLengde) }

            utfall(erUtestengt) { somUtgangspunkt(true) }
        }
}

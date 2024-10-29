package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object Utestengning {
    val utestengt = Opplysningstype.somBoolsk("Utestengt")
    val ikkeUtestengt = Opplysningstype.somBoolsk("Oppfyller krav til ikke utestengt")
    val regelsett =
        Regelsett("Utestengning") {
            regel(utestengt) {
                oppslag(prøvingsdato) { false }
            }
            regel(ikkeUtestengt) {
                ikke(utestengt)
            }
        }
}

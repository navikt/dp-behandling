package no.nav.dagpenger.opplysning.regelsett

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.dato.sisteAv
import no.nav.dagpenger.opplysning.regelsett.Alderskrav.fødselsdato

internal object Virkningsdato {
    // val fødselsdato = Opplysningstype<LocalDate>("Fødselsdato")
    val søknadsdato = Opplysningstype.somDato("Søknadsdato")
    val sisteDagMedArbeidsplikt = Opplysningstype.somDato("Siste dag med arbeidsplikt")
    val sisteDagMedLønn = Opplysningstype.somDato("Siste dag med lønn")

    val virkningsdato = Opplysningstype.somDato("Virkningsdato")
    val regelsett =
        Regelsett("alder").apply {
            regel(virkningsdato) { sisteAv(fødselsdato, søknadsdato, sisteDagMedArbeidsplikt, sisteDagMedLønn) }
        }
}

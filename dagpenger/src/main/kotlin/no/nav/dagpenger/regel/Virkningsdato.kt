package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.dato.sisteAv
import no.nav.dagpenger.regel.Alderskrav.fødselsdato

object Virkningsdato {
    // val fødselsdato = Opplysningstype<LocalDate>("Fødselsdato")
    val søknadsdato = Opplysningstype.somDato("Søknadsdato".id("Søknadstidspunkt"))
    // val sisteDagMedArbeidsplikt = Opplysningstype<LocalDate>("Siste dag med arbeidsplikt")
    // val sisteDagMedLønn = Opplysningstype<LocalDate>("Siste dag med lønn")

    val virkningsdato = Opplysningstype.somDato("Virkningsdato")

    val regelsett =
        Regelsett("alder").apply {
            regel(virkningsdato) { sisteAv(fødselsdato, søknadsdato) }
        }
}

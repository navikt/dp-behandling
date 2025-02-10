package no.nav.dagpenger.opplysning.regelsett

import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Id
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.dato.sisteAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.uuid.UUIDv7

internal object Prøvingsdato {
    val søknadsdato = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Søknadsdato")
    val sisteDagMedArbeidsplikt = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Siste dag med arbeidsplikt")
    val sisteDagMedLønn = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Siste dag med lønn")

    val prøvingsdato = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Prøvingsdato")
    val regelsett =
        vilkår("alder") {
            regel(søknadsdato) { innhentes }
            regel(sisteDagMedArbeidsplikt) { innhentes }
            regel(sisteDagMedLønn) { innhentes }
            regel(prøvingsdato) { sisteAv(søknadsdato, sisteDagMedArbeidsplikt, sisteDagMedLønn) }
        }
}

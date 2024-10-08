package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.dato.sisteAv
import no.nav.dagpenger.opplysning.regel.innhentes

object Søknadstidspunkt {
    // § 3A-1.Søknadstidspunkt https://lovdata.no/forskrift/1998-09-16-890/§3a-1
    val søknadsdato =
        Opplysningstype.somDato(
            "Søknadsdato".id(
                id = "Søknadstidspunkt",
                tekstId = "opplysning.soknadsdato",
            ),
        )
    val ønsketdato = Opplysningstype.somDato("Ønsker dagpenger fra dato".id("ØnskerDagpengerFraDato"))

    // TODO: vi må rydde i begrepsbruk, behovsløsere forventer at dette kalles virkningsdato
    val søknadstidspunkt = Opplysningstype.somDato("Søknadstidspunkt".id("Virkningsdato"))

    val regelsett =
        Regelsett("Søknadstidspunkt").apply {
            regel(søknadsdato) { innhentes }
            regel(ønsketdato) { innhentes }
            regel(søknadstidspunkt) { sisteAv(søknadsdato, ønsketdato) }
        }

    val SøknadstidspunktForLangtFramITid =
        Kontrollpunkt(Avklaringspunkter.SøknadstidspunktForLangtFramITid) {
            it.har(søknadstidspunkt) &&
                it.finnOpplysning(søknadstidspunkt).verdi.isAfter(
                    it.finnOpplysning(søknadsdato).verdi.plusDays(14),
                )
        }
}

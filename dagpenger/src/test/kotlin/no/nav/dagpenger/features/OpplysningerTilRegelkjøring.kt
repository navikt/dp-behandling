package no.nav.dagpenger.features

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import java.time.LocalDate

val opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger = {
    if (har(Søknadstidspunkt.prøvingsdato)) {
        forDato(finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi)
    } else {
        forDato(it)
    }
}

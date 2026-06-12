package no.nav.dagpenger.ferietillegg.features

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import java.time.LocalDate

val opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger = {
    this
}

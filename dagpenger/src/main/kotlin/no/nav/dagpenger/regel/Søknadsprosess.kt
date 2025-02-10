package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.ReellArbeidssøker.registrertArbeidssøker

class Søknadsprosess : Forretningsprosess {
    override val regelverk = RegelverkDagpenger

    override fun regelsett() = regelverk.regelsett

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skal(opplysninger) }.flatMap {
            println("Vil kjøre regelsett: ${it.navn}")
            listOfNotNull(it.utfall) + it.ønsketResultat
        } + registrertArbeidssøker
}

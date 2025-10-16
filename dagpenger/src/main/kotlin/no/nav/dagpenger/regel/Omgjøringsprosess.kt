package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import java.time.LocalDate

class Omgjøringsprosess : Forretningsprosess(RegelverkDagpenger) {
    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring =
        Regelkjøring(
            virkningsdato(opplysninger),
            opplysninger,
            this,
            opplysningerGyldigPåPrøvingsdato,
        )

    override fun kontrollpunkter() = kontrollpunkter

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato(opplysninger)

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }

    private val opplysningerGyldigPåPrøvingsdato: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger =
        { forDato(prøvingsdato(this)) }

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.kunEgne
            .somListe()
            .last { !it.gyldighetsperiode.fraOgMed.isEqual(LocalDate.MIN) }
            .gyldighetsperiode.fraOgMed
}

package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.RegelverkDagpenger
import java.time.LocalDate

class Migreringsprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val dato = prøvingsdato(opplysninger)
        return Regelkjøring(
            regelverksdato = dato,
            prøvingsperiode = Regelkjøring.Periode(dato),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): Set<Opplysningstype<*>> = emptySet()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = false
    // opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato(opplysninger)

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate = PrøvingsdatoUtleder.utled(opplysninger)
}

package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.regel.Utgangspunkt
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.regel.OpplysningsTyper.AldersgrenseId
import no.nav.dagpenger.regel.RegelverkDagpenger
import java.time.LocalDate

class Migreringsprosess(
    private val etterspørSomUtgangspunkt: Set<Opplysningstype.Id<*>> = defaultEtterspurteUtgangspunkt,
) : Forretningsprosess(RegelverkDagpenger) {

    init {
        registrer(RettighetsperiodePlugin(regelverk))
    }

    private companion object {
        val defaultEtterspurteUtgangspunkt = setOf(AldersgrenseId)
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
    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = false

    // opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }
    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato(opplysninger)

    override fun produsenter(
        regelverksdato: LocalDate,
        opplysningerPåPrøvingsdato: LesbarOpplysninger,
    ) = super.produsenter(regelverksdato, opplysningerPåPrøvingsdato).mapValues { (type, regel) ->
        if (regel is Utgangspunkt<*> && type.id in etterspørSomUtgangspunkt) type.somEkstern() else regel
    }

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate = PrøvingsdatoUtleder.utled(opplysninger)

    @Suppress("UNCHECKED_CAST")
    private fun Opplysningstype<out Any>.somEkstern() = (this as Opplysningstype<Any>).innhentes

}

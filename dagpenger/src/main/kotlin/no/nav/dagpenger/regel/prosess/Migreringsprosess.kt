package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.regel.Utgangspunkt
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt
import java.time.LocalDate

class Migreringsprosess(
    private val etterspørSomUtgangspunkt: Set<Opplysningstype.Id<*>> = defaultEtterspurteUtgangspunkt.map { it.id }.toSet(),
) : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
    }

    companion object {
        //         Søknadstidspunkt.ønsketdato,

        val defaultEtterspurteUtgangspunkt =
            setOf(
                Søknadstidspunkt.ønsketdato,
                Verneplikt.avtjentVerneplikt,
                DagpengenesStørrelse.barn,
                Opphold.bostedsland,
                TapAvArbeidsinntektOgArbeidstid.kravPåLønn,
                ReellArbeidssøker.ønsketArbeidstid,
                ReellArbeidssøker.kanJobbeDeltid,
                ReellArbeidssøker.kanJobbeHvorSomHelst,
                ReellArbeidssøker.erArbeidsfør,
                ReellArbeidssøker.villigTilEthvertArbeid,
                Utdanning.tarUtdanning,
                Rettighetstype.skalPermitteringVurderes,
                Rettighetstype.skalPermitteringFiskeforedlingVurderes,
                Rettighetstype.skalReellArbeidssøkerVurderes,
                Rettighetstype.skalVernepliktVurderes,
                // Rettighetstype.skalGjenopptakVurderes,
                Rettighetstype.skalEksportVurderes,
                Rettighetstype.skalEtableringVurderes,
                Rettighetstype.ordinærArbeid,
                Rettighetstype.lønnsgaranti,
                Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet,
            )
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

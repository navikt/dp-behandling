package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.regel.Utgangspunkt
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
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
        private val betingedeKvoter: Map<Opplysningstype<Boolean>, KvoteDefinisjon> =
            mapOf(
                Permittering.oppfyllerKravetTilPermittering to PermitteringFastsetting.regelsett.kvoter.single(),
                Sanksjonsperiode.harSanksjon to Sanksjonsperiode.regelsett.kvoter.single(),
                TidsbegrensetBortfall.harTidsbegrensetBortfall to TidsbegrensetBortfall.regelsett.kvoter.single(),
            )

        val migreringssaldoer: Set<Opplysningstype<*>> =
            setOf(
                Beregning.gjenståendeDager,
                Beregning.forbrukt,
                Beregning.gjenståendeEgenandel,
                Beregning.sisteForbruksdag,
            ) + betingedeKvoter.values.flatMap { listOf(it.forbruksteller, it.gjenstående, it.sisteForbruk) }

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

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): Set<Opplysningstype<*>> =
        super.ønsketResultat(opplysninger) + manglendeSaldoer(opplysninger)

    // opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }
    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato(opplysninger)

    override fun produsenter(
        regelverksdato: LocalDate,
        opplysningerPåPrøvingsdato: LesbarOpplysninger,
    ) = super.produsenter(regelverksdato, opplysningerPåPrøvingsdato).mapValues { (type, regel) ->
        if (regel is Utgangspunkt<*> && type.id in etterspørSomUtgangspunkt) type.somEkstern() else regel
    } + manglendeSaldoer(opplysningerPåPrøvingsdato).associateWith { it.somEkstern() }

    private fun manglendeSaldoer(opplysninger: LesbarOpplysninger): Set<Opplysningstype<*>> {
        val saldodato = prøvingsdato(opplysninger).minusDays(1)
        val historiskeOpplysninger = opplysninger.forDato(saldodato)

        fun <T : Any> harSaldo(type: Opplysningstype<T>): Boolean =
            historiskeOpplysninger.finnNullableOpplysning(type)?.gyldighetsperiode?.tilOgMed == saldodato

        fun harForbruk(type: Opplysningstype<Int>): Boolean = harSaldo(type) && historiskeOpplysninger.finnOpplysning(type).verdi > 0

        val nødvendige =
            mutableSetOf<Opplysningstype<*>>(
                Beregning.gjenståendeDager,
                Beregning.forbrukt,
                Beregning.gjenståendeEgenandel,
            )
        if (harForbruk(Beregning.forbrukt)) nødvendige.add(Beregning.sisteForbruksdag)
        betingedeKvoter.forEach { (betingelse, kvote) ->
            if (opplysninger.erSann(betingelse)) {
                nødvendige.add(kvote.forbruksteller)
                nødvendige.add(kvote.gjenstående)
                if (harForbruk(kvote.forbruksteller)) nødvendige.add(kvote.sisteForbruk)
            }
        }
        return nødvendige.filterNot { harSaldo(it) }.toSet()
    }

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate = PrøvingsdatoUtleder.utled(opplysninger)

    @Suppress("UNCHECKED_CAST")
    private fun Opplysningstype<out Any>.somEkstern() = (this as Opplysningstype<Any>).innhentes
}

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
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt
import java.time.LocalDate

class Migreringsprosess(
    private val opplysningerViAlltidSkalBeOm: Set<Opplysningstype.Id<*>> = setMedopplysningerViAlltidSkalBeOm.map { it.id }.toSet(),
) : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
    }

    companion object {
        private val kvoterViSetterDersomVilkårErTrue: Map<Opplysningstype<Boolean>, KvoteDefinisjon> =
            mapOf(
                Permittering.oppfyllerKravetTilPermittering to PermitteringFastsetting.regelsett.kvoter.single(),
                Sanksjonsperiode.harSanksjon to Sanksjonsperiode.regelsett.kvoter.single(),
                TidsbegrensetBortfall.harTidsbegrensetBortfall to TidsbegrensetBortfall.regelsett.kvoter.single(),
            )

        private val opplysningerViSkalBeOmHvisEnOpplysningErTrue: Map<Opplysningstype<Boolean>, Set<Opplysningstype<*>>> =
            mapOf(
                Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet to setOf(Sanksjonsperiode.antallSanksjonsuker),
                Samordning.sykepenger to setOf(Samordning.sykepengerDagsats),
                Samordning.pleiepenger to setOf(Samordning.pleiepengerDagsats),
                Samordning.omsorgspenger to setOf(Samordning.omsorgspengerDagsats),
                Samordning.opplæringspenger to setOf(Samordning.opplæringspengerDagsats),
                Samordning.uføre to setOf(Samordning.uføreDagsats),
                Samordning.foreldrepenger to setOf(Samordning.foreldrepengerDagsats),
                Samordning.svangerskapspenger to setOf(Samordning.svangerskapspengerDagsats),
                SamordingUtenforFolketrygden.andreYtelser to
                    setOf(
                        SamordingUtenforFolketrygden.pensjonFraOffentligTjenestepensjonsordning,
                        SamordingUtenforFolketrygden.mottarRedusertUførepensjon,
                        SamordingUtenforFolketrygden.vartpenger,
                        SamordingUtenforFolketrygden.ventelønn,
                        SamordingUtenforFolketrygden.etterlønn,
                        SamordingUtenforFolketrygden.garantilottGFF,
                    ),
                SamordingUtenforFolketrygden.pensjonFraOffentligTjenestepensjonsordning to
                    setOf(SamordingUtenforFolketrygden.pensjonFraOffentligTjenestepensjonsordningBeløp),
                SamordingUtenforFolketrygden.mottarRedusertUførepensjon to
                    setOf(SamordingUtenforFolketrygden.redusertUførepensjonBeløp),
                SamordingUtenforFolketrygden.vartpenger to setOf(SamordingUtenforFolketrygden.vartpengerBeløp),
                SamordingUtenforFolketrygden.ventelønn to setOf(SamordingUtenforFolketrygden.ventelønnBeløp),
                SamordingUtenforFolketrygden.etterlønn to setOf(SamordingUtenforFolketrygden.etterlønnBeløp),
                SamordingUtenforFolketrygden.garantilottGFF to setOf(SamordingUtenforFolketrygden.garantilottGFFBeløp),
                Utdanning.tarUtdanning to
                    setOf(
                        Utdanning.deltakelseIArbeidsmarkedstiltak,
                        Utdanning.opplæringForInnvandrere,
                        Utdanning.grunnskoleopplæring,
                        Utdanning.høyereYrkesfagligUtdanning,
                        Utdanning.høyereUtdanning,
                        Utdanning.deltakelsePåKurs,
                    ),
                Rettighetstype.skalEtableringVurderes to
                    setOf(
                        Etablering.nyVirksomhet,
                        Etablering.selvforsørget,
                        Etablering.godkjentNæringsfaglig,
                        Etablering.ikkeSelvforskyldtArbeidsledig,
                        Etablering.overFemtiProsentEierandel,
                        Etablering.sluttDato,
                    ),
                Rettighetstype.skalPermitteringVurderes to
                    setOf(Permittering.godkjentPermitteringsårsak, Permittering.erPermitteringenMidlertidig),
            )

        private val opplysningerViSkalBeOmHvisEnOpplysningErFalse: Map<Opplysningstype<Boolean>, Set<Opplysningstype<*>>> =
            mapOf(
                ReellArbeidssøker.kanJobbeDeltid to setOf(ReellArbeidssøker.godkjentDeltidssøker),
                ReellArbeidssøker.kanJobbeHvorSomHelst to setOf(ReellArbeidssøker.godkjentLokalArbeidssøker),
                ReellArbeidssøker.erArbeidsfør to setOf(ReellArbeidssøker.godkjentArbeidsufør),
            )

        val kvoterViSkalMigrere: Set<Opplysningstype<*>> =
            setOf(
                Beregning.gjenståendeDager,
                Beregning.forbrukt,
                Beregning.gjenståendeEgenandel,
                Beregning.sisteForbruksdag,
            ) + kvoterViSetterDersomVilkårErTrue.values.flatMap { listOf(it.forbruksteller, it.gjenstående, it.sisteForbruk) }

        val setMedopplysningerViAlltidSkalBeOm =
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
        super.ønsketResultat(opplysninger) + manglendeKvoter(opplysninger) + betingedeOpplysninger(opplysninger)

    // opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }
    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato(opplysninger)

    override fun produsenter(
        regelverksdato: LocalDate,
        opplysningerPåPrøvingsdato: LesbarOpplysninger,
    ) = super.produsenter(regelverksdato, opplysningerPåPrøvingsdato).mapValues { (type, regel) ->
        if (regel is Utgangspunkt<*> && type.id in opplysningerViAlltidSkalBeOm) type.somEkstern() else regel
    } +
        (manglendeKvoter(opplysningerPåPrøvingsdato) + betingedeOpplysninger(opplysningerPåPrøvingsdato))
            .associateWith { it.somEkstern() }

    private fun betingedeOpplysninger(opplysninger: LesbarOpplysninger): Set<Opplysningstype<*>> =
        (
            opplysningerViSkalBeOmHvisEnOpplysningErTrue.filterKeys { opplysninger.erSann(it) }.values +
                opplysningerViSkalBeOmHvisEnOpplysningErFalse.filterKeys { opplysninger.finnNullableOpplysning(it)?.verdi == false }.values
        ).flatten().toSet()

    private fun manglendeKvoter(opplysninger: LesbarOpplysninger): Set<Opplysningstype<*>> {
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
        kvoterViSetterDersomVilkårErTrue.forEach { (betingelse, kvote) ->
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

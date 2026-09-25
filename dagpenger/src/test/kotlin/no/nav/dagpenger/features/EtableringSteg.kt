package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.regel.prosess.RettighetsperiodePlugin
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype

class EtableringSteg : No {
    private val fraDato = 1.januar(2025)
    private val regelsett = listOf(Etablering.regelsett)
    private val opplysninger =
        Opplysninger.basertPå(
            Opplysninger.med(Faktum(harLøpendeRett, true, Gyldighetsperiode(fraDato))),
        )
    private val etableringRegelverk =
        Regelverk(
            navn = RegelverkType("Etablering"),
            regelsett = arrayOf(Etablering.regelsett),
        )
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at etablering skal vurderes") {
            opplysninger
                .leggTil(Faktum(Rettighetstype.skalEtableringVurderes, true) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Gitt("at etablering ikke skal vurderes") {
            opplysninger.leggTil(Faktum(Rettighetstype.skalEtableringVurderes, false) as Opplysning<*>)
        }

        Gitt("de øvrige vilkårene for etablering er oppfylt") {
            opplysninger
                .leggTil {
                    it.add(Faktum(Etablering.godkjentNæringsfaglig, true))
                    it.add(Faktum(Etablering.ikkeSelvforskyldtArbeidsledig, true))
                }.also { regelkjøring.evaluer() }
        }

        Og("saksbehandler vurderer at etablering påvirker resultatet {boolsk}") { påvirker: Boolean ->
            opplysninger
                .leggTil(Faktum(Etablering.påvirkerUtfallet, påvirker) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Og("saksbehandler vurderer at det er en ny virksomhet {boolsk}") { nyVirksomhet: Boolean ->
            opplysninger
                .leggTil(Faktum(Etablering.nyVirksomhet, nyVirksomhet) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Og("saksbehandler vurderer selvforsørgelse som {boolsk}") { selvforsørget: Boolean ->
            opplysninger
                .leggTil(Faktum(Etablering.selvforsørget, selvforsørget) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Så("skal vilkåret om etablering være {boolsk}") { utfall: Boolean ->
            opplysninger.finnOpplysning(Etablering.etableringGodkjent).verdi shouldBe utfall
        }

        Så("skal opplysninger om etablering ikke være satt") {
            listOf(
                Etablering.påvirkerUtfallet,
                Etablering.nyVirksomhet,
                Etablering.selvforsørget,
                Etablering.etableringGodkjent,
            ).forEach { opplysningstype ->
                opplysninger.har(opplysningstype) shouldBe false
            }
        }

        Og("skal retten til dagpenger være {boolsk}") { forventetRett: Boolean ->
            RettighetsperiodePlugin(etableringRegelverk).regelkjøringFerdig(Prosesskontekst(opplysninger))
            opplysninger.finnOpplysning(harLøpendeRett).verdi shouldBe forventetRett
        }
    }
}

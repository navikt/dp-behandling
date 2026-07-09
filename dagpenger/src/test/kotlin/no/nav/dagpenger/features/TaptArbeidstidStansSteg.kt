package no.nav.dagpenger.features

import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.regel.prosess.TaptArbeidstidStans
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.regelsett.vilkår.TreMeldePerioderUtentilstrekkeligTapAvArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TreMeldePerioderUtentilstrekkeligTapAvArbeidstid.trePåfølgendePerioderUtenTilstrekkeligTap
import no.nav.dagpenger.testsupport.somLocalDate

class TaptArbeidstidStansSteg : No {
    private val opplysninger = mutableListOf<Opplysning<*>>()
    private lateinit var resultatOpplysninger: Opplysninger

    init {
        Gitt("at bruker har løpende rett fra og med {string}") { fraOgMed: String ->
            opplysninger.add(Faktum(harLøpendeRett, true, Gyldighetsperiode(fraOgMed.somLocalDate())))
            opplysninger.add(Faktum(trePåfølgendePerioderUtenTilstrekkeligTap, true, Gyldighetsperiode(fraOgMed.somLocalDate())))
        }

        Gitt("at grenseverdien er {int} påfølgende perioder uten tapt arbeidstid") { antall: Int ->
            opplysninger.add(Faktum(Beregning.maksAntallPerioderMedIkkeTaptArbeidstid, antall))
        }

        Og("at periode {string} til {string} oppfylte kravet til tapt arbeidstid") { fra: String, til: String ->
            opplysninger.add(
                Faktum(Beregning.oppfyllerKravTilTaptArbeidstidIPerioden, true, Gyldighetsperiode(fra.somLocalDate(), til.somLocalDate())),
            )
        }

        Og("at periode {string} til {string} ikke oppfylte kravet til tapt arbeidstid") { fra: String, til: String ->
            opplysninger.add(
                Faktum(Beregning.oppfyllerKravTilTaptArbeidstidIPerioden, false, Gyldighetsperiode(fra.somLocalDate(), til.somLocalDate())),
            )
        }

        Når("stansregelen kjøres") {
            resultatOpplysninger = opplysninger.somOpplysninger()
            TaptArbeidstidStans().regelkjøringFerdig(Prosesskontekst(resultatOpplysninger))
        }

        Så("skal dagpengene stanses fra og med {string}") { stansDato: String ->
            resultatOpplysninger
                .finnAlle(TreMeldePerioderUtentilstrekkeligTapAvArbeidstid.trePåfølgendePerioderUtenTilstrekkeligTap)
                .any { !it.verdi && it.gyldighetsperiode.fraOgMed == stansDato.somLocalDate() } shouldBe true
        }

        Så("skal dagpengene ikke stanses") {
            resultatOpplysninger
                .finnAlle(TreMeldePerioderUtentilstrekkeligTapAvArbeidstid.trePåfølgendePerioderUtenTilstrekkeligTap)
                .none { !it.verdi } shouldBe true
        }
    }
}

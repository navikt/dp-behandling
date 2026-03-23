package no.nav.dagpenger.features

import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Gjenopptak
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Rettighetstype
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import java.time.LocalDate

class GjenopptakSteg : No {
    private val regelsett = RegelverkDagpenger.regelsettFor(Gjenopptak.skalGjenopptas)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    init {
        Gitt("at søkeren har hatt en løpende stønadsperiode og har hatt minst en forbruksdag på {dato}") { sisteForbruksdag: LocalDate ->
            regelkjøring = Regelkjøring(sisteForbruksdag, opplysninger, *regelsett.toTypedArray())
            opplysninger.leggTil(Faktum(Beregning.sisteForbruksdag, sisteForbruksdag, Gyldighetsperiode(sisteForbruksdag)))
            opplysninger.leggTil(Faktum(Dagpengeperiode.antallStønadsdager, 520, Gyldighetsperiode()))
            opplysninger.leggTil(Faktum(Beregning.sisteGjenståendeDager, 520, Gyldighetsperiode()))
        }

        Gitt("søker etter gjenopptak på {dato}") { gjenopptaksDato: LocalDate ->
            regelkjøring = Regelkjøring(gjenopptaksDato, opplysninger, *regelsett.toTypedArray())

            opplysninger.leggTil(Faktum(Søknadstidspunkt.prøvingsdato, gjenopptaksDato))
            opplysninger.leggTil(Faktum(Rettighetstype.skalGjenopptakVurderes, true, Gyldighetsperiode()))
            opplysninger.leggTil(Faktum(Dagpengeperiode.antallStønadsdager, 520, Gyldighetsperiode()))
            opplysninger.leggTil(Faktum(Beregning.sisteGjenståendeDager, 520, Gyldighetsperiode()))
            regelkjøring.evaluer()
        }

        Gitt("har vært i {boolsk} etter siste forbruksdag") { arbeid12UkerEllerMer: Boolean ->
            opplysninger.leggTil(Faktum(Gjenopptak.oppholdMedArbeidI12ukerEllerMer, arbeid12UkerEllerMer))
            regelkjøring.evaluer()
        }

        Så("skal gjenopptak være {boolsk}") { gjennoptak: Boolean ->
            val skalGjenoppta = opplysninger.finnOpplysning(Gjenopptak.skalGjenopptas)
            skalGjenoppta.verdi shouldBe gjennoptak
        }

        Så("skal reberegnes være {boolsk}") { reberegnes: Boolean ->
            val skalReberegnes = opplysninger.finnOpplysning(Gjenopptak.oppholdMedArbeidI12ukerEllerMer)
            skalReberegnes.verdi shouldBe reberegnes
        }
    }
}

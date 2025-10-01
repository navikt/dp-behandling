package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.oppfyllerKravetTilPermitteringFiskeindustri
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Rettighetstype.permitteringFiskeforedling
import no.nav.dagpenger.regel.Virkningstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Egenandel

class EgenandelSteg : No {
    private val fraDato = 10.mai(2024)
    private val regelsett =
        RegelverkDagpenger.regelsettFor(Egenandel.egenandel) + PermitteringFraFiskeindustrien.regelsett
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at sats er {string}") { sats: String ->
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato)).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg, Beløp(sats.toBigDecimal())),
                ).also { regelkjøring.evaluer() }
        }
        Og("søker har ikke permittering fra fiskeindustrien") {
            opplysninger.apply {
                leggTil(Faktum(oppfyllerKravetTilPermitteringFiskeindustri, false))
                leggTil(Faktum(permitteringFiskeforedling, false))
                regelkjøring.evaluer()
            }
        }

        Og("søker har permittering fra fiskeindustrien") {
            opplysninger
                .apply {
                    leggTil(Faktum(oppfyllerKravetTilPermitteringFiskeindustri, true))
                    leggTil(Faktum(permitteringFiskeforedling, true))
                    regelkjøring.evaluer()
                }
        }

        Så("skal egenandel være {string}") { string: String ->
            opplysninger.finnOpplysning(Egenandel.egenandel).verdi shouldBe Beløp(string.toBigDecimal())
        }
    }
}

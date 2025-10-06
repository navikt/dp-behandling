package no.nav.dagpenger.regel

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.april
import no.nav.dagpenger.dato.juli
import no.nav.dagpenger.dato.juni
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.beregning.Beregning.meldeperiode
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.aldersgrenseBarnetillegg
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.antallBarn
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.barn
import org.junit.jupiter.api.Test

class BarnOver18Test {
    private val barneliste =
        BarnListe(
            listOf(
                Barn(
                    fødselsdato = 5.april(2016),
                    fornavnOgMellomnavn = "Per",
                    etternavn = "Pedersen",
                    statsborgerskap = "NOR",
                    kvalifiserer = true,
                ),
                Barn(
                    fødselsdato = 1.april(2012),
                    fornavnOgMellomnavn = "Arne",
                    etternavn = "Arnesen",
                    statsborgerskap = "NOR",
                    kvalifiserer = true,
                ),
            ),
        )
    private val innvilgelse =
        Opplysninger().apply {
            leggTil(Faktum(aldersgrenseBarnetillegg, 18))
            leggTil(Faktum(barn, barneliste))
            leggTil(Faktum(antallBarn, barneliste.count { it.girBarnetillegg(1.juni(2018), 18) }, Gyldighetsperiode(1.juni(2018))))
        }

    @Test
    fun `barn blir 18 på onsdag i meldeperioden`() {
        val beregning =
            Opplysninger.basertPå(innvilgelse).apply {
                leggTil(Faktum(meldeperiode, Periode(3.april(2034), 16.april(2034))))
            }
        val plugin = BarnOver18()
        plugin.start(beregning)

        val tillegg = beregning.finnAlle(antallBarn)
        tillegg shouldHaveSize 2

        tillegg.first().gyldighetsperiode.tilOgMed shouldBe 4.april(2034)
        tillegg.first().verdi shouldBe 2

        // Femte april 2034 er en onsdag
        tillegg.last().gyldighetsperiode.fraOgMed shouldBe 5.april(2034)
        tillegg.last().verdi shouldBe 0
    }

    @Test
    fun `regner ut nye barn som gir barnetillegg siden forrige meldeperiode`() {
        val beregning =
            Opplysninger.basertPå(innvilgelse).apply {
                leggTil(Faktum(meldeperiode, Periode(1.juli(2031), 14.juli(2031))))
            }
        val plugin = BarnOver18()
        plugin.start(beregning)

        val tillegg = beregning.finnAlle(antallBarn)
        tillegg shouldHaveSize 2
    }

    @Test
    fun `barn blir 18 på søndag før meldeperioden`() {
        val beregning =
            Opplysninger.basertPå(innvilgelse).apply {
                leggTil(Faktum(meldeperiode, Periode(1.april(2030), 14.april(2030))))
            }
        val plugin = BarnOver18()
        plugin.start(beregning)

        val tillegg = beregning.finnAlle(antallBarn)
        tillegg shouldHaveSize 2

        tillegg.first().verdi shouldBe 2
        tillegg.last().verdi shouldBe 1
    }
}

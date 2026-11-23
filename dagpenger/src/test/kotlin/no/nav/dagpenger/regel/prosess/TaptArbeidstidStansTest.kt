package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.TreMeldePerioderUtentilstrekkeligTapAvArbeidstid.trePåfølgendePerioderUtenTilstrekkeligTap
import org.junit.jupiter.api.Test

class TaptArbeidstidStansTest {
    private val stansdato = 20.januar(2025)
    private val stansperiode = Gyldighetsperiode(stansdato)
    private val plugin = TaptArbeidstidStans()

    @Test
    fun `ber bare om rekjøring første gang samme stans fastsettes`() {
        val opplysninger = grunnlag()
        val førsteRunde = Prosesskontekst(opplysninger)

        plugin.regelkjøringFerdig(førsteRunde)

        førsteRunde.kreverRekjøring shouldBe true
        opplysninger.finnOpplysning(trePåfølgendePerioderUtenTilstrekkeligTap, stansdato).verdi shouldBe false
        val etterFørsteRunde = opplysninger.kunEgne.somListe().map { it.id }
        val nesteRunde = Prosesskontekst(opplysninger)

        plugin.regelkjøringFerdig(nesteRunde)

        nesteRunde.kreverRekjøring shouldBe false
        opplysninger.kunEgne.somListe().map { it.id } shouldBe etterFørsteRunde
    }

    @Test
    fun `skriver ikke ny stans når arvede perioder samlet dekker hele stansperioden`() {
        val arvede =
            grunnlag().apply {
                leggTil(
                    Faktum(
                        trePåfølgendePerioderUtenTilstrekkeligTap,
                        false,
                        Gyldighetsperiode(stansdato.minusDays(1), stansdato.plusDays(13)),
                    ),
                )
                leggTil(Faktum(trePåfølgendePerioderUtenTilstrekkeligTap, false, Gyldighetsperiode(stansdato.plusDays(14))))
            }
        val opplysninger = Opplysninger().baserPå(arvede)
        val kontekst = Prosesskontekst(opplysninger)

        plugin.regelkjøringFerdig(kontekst)

        kontekst.kreverRekjøring shouldBe false
        opplysninger.kunEgne.somListe() shouldBe emptyList()
    }

    @Test
    fun `utvider stansen når eksisterende stans slutter for tidlig`() {
        val opplysninger =
            grunnlag().apply {
                leggTil(
                    Faktum(
                        trePåfølgendePerioderUtenTilstrekkeligTap,
                        false,
                        Gyldighetsperiode(stansdato, stansdato.plusDays(13)),
                    ),
                )
            }
        val kontekst = Prosesskontekst(opplysninger)

        plugin.regelkjøringFerdig(kontekst)

        kontekst.kreverRekjøring shouldBe true
        opplysninger.finnOpplysning(trePåfølgendePerioderUtenTilstrekkeligTap).gyldighetsperiode shouldBe stansperiode
    }

    @Test
    fun `flytter stansen bakover når eksisterende stans starter for sent`() {
        val opplysninger =
            grunnlag().apply {
                leggTil(Faktum(trePåfølgendePerioderUtenTilstrekkeligTap, false, Gyldighetsperiode(stansdato.plusDays(14))))
            }
        val kontekst = Prosesskontekst(opplysninger)

        plugin.regelkjøringFerdig(kontekst)

        kontekst.kreverRekjøring shouldBe true
        opplysninger.finnOpplysning(trePåfølgendePerioderUtenTilstrekkeligTap, stansdato).verdi shouldBe false
    }

    @Test
    fun `skriver stans på nytt når en dag i stansperioden er satt tilbake til sann`() {
        val opplysninger =
            grunnlag().apply {
                leggTil(Faktum(trePåfølgendePerioderUtenTilstrekkeligTap, false, stansperiode))
                leggTil(Faktum(trePåfølgendePerioderUtenTilstrekkeligTap, true, Gyldighetsperiode.kun(stansdato.plusDays(14))))
            }
        val kontekst = Prosesskontekst(opplysninger)

        plugin.regelkjøringFerdig(kontekst)

        kontekst.kreverRekjøring shouldBe true
        opplysninger.finnOpplysning(trePåfølgendePerioderUtenTilstrekkeligTap, stansdato.plusDays(14)).verdi shouldBe false
    }

    private fun grunnlag() =
        Opplysninger().apply {
            leggTil(Faktum(Beregning.maksAntallPerioderMedIkkeTaptArbeidstid, 3))
            leggTil(Faktum(trePåfølgendePerioderUtenTilstrekkeligTap, true, Gyldighetsperiode(6.januar(2025))))
            repeat(3) { indeks ->
                val fraOgMed = stansdato.plusDays(indeks * 14L)
                leggTil(
                    Faktum(
                        Beregning.oppfyllerKravTilTaptArbeidstidIPerioden,
                        false,
                        Gyldighetsperiode(fraOgMed, fraOgMed.plusDays(13)),
                    ),
                )
            }
        }
}

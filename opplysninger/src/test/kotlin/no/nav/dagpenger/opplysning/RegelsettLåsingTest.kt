package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.addisjon
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RegelsettLåsingTest {
    private val inntektA = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "Inntekt A")
    private val inntektB = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "Inntekt B")
    private val totalInntekt = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "Total inntekt")
    private val resultat = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "Resultat")

    @Test
    fun `kan låse opplysninger fra spesifikt regelsett`() {
        // Regelsett 1: Minsteinntekt - beregner totalInntekt
        val minsteinntektRegelsett =
            vilkår(tomHjemmel("Minsteinntekt")) {
                regel(inntektA) { innhentes }
                regel(inntektB) { innhentes }
                regel(totalInntekt) { addisjon(inntektA, inntektB) }
            }

        // Regelsett 2: Beregning - adderer totalInntekt + inntektB
        val beregningRegelsett =
            vilkår(tomHjemmel("Beregning")) {
                regel(resultat) { addisjon(totalInntekt, inntektB) }
            }

        // Første behandling
        val opplysninger1 = Opplysninger()
        opplysninger1.leggTil(Faktum(inntektA, Beløp(100.0)))
        opplysninger1.leggTil(Faktum(inntektB, Beløp(200.0)))

        val regelkjøring1 =
            Regelkjøring(
                LocalDate.now(),
                opplysninger1,
                minsteinntektRegelsett,
                beregningRegelsett,
            )
        regelkjøring1.evaluer()

        // Verifiser første beregning
        opplysninger1.finnOpplysning(totalInntekt).verdi shouldBe Beløp(300.0)
        opplysninger1.finnOpplysning(resultat).verdi shouldBe Beløp(500.0)

        // Ny behandling basert på den første
        val opplysninger2 = Opplysninger.basertPå(opplysninger1)

        // Lås alle opplysninger fra Minsteinntekt-regelsettet
        opplysninger2.låsOpplysningerFraRegelsett(minsteinntektRegelsett)

        // Verifiser at totalInntekt er låst
        val totalInntektOppl = opplysninger2.finnOpplysning(totalInntekt)
        totalInntektOppl.erLåst shouldBe true
        totalInntektOppl.utledetAv?.regelsettnavn shouldBe "Minsteinntekt"

        // Endre inntektA i ny behandling
        opplysninger2.leggTil(Faktum(inntektA, Beløp(500.0)))

        val regelkjøring2 =
            Regelkjøring(
                LocalDate.now(),
                opplysninger2,
                minsteinntektRegelsett,
                beregningRegelsett,
            )
        val rapport = regelkjøring2.evaluer()

        // totalInntekt skal fortsatt være 300 (ikke 700) fordi den er låst
        opplysninger2.finnOpplysning(totalInntekt).verdi shouldBe Beløp(300.0)

        // resultat skal fortsatt være 500 fordi totalInntekt ikke endret seg
        opplysninger2.finnOpplysning(resultat).verdi shouldBe Beløp(500.0)

        // Ingen regler fra Minsteinntekt skulle kjørt
        rapport.kjørteRegler.none { it.regelsettnavn == "Minsteinntekt" } shouldBe true
    }

    @Test
    fun `opplysninger fra ulåst regelsett kan fortsatt utledes på nytt`() {
        val minsteinntektRegelsett =
            vilkår(tomHjemmel("Minsteinntekt")) {
                regel(inntektA) { innhentes }
                regel(inntektB) { innhentes }
                regel(totalInntekt) { addisjon(inntektA, inntektB) }
            }

        val beregningRegelsett =
            vilkår(tomHjemmel("Beregning")) {
                regel(resultat) { addisjon(totalInntekt, inntektB) }
            }

        val opplysninger1 = Opplysninger()
        opplysninger1.leggTil(Faktum(inntektA, Beløp(100.0)))
        opplysninger1.leggTil(Faktum(inntektB, Beløp(200.0)))

        Regelkjøring(LocalDate.now(), opplysninger1, minsteinntektRegelsett, beregningRegelsett).evaluer()

        val opplysninger2 = Opplysninger.basertPå(opplysninger1)

        // Lås KUN Minsteinntekt, ikke Beregning
        opplysninger2.låsOpplysningerFraRegelsett(minsteinntektRegelsett)

        // Endre inntektB - dette skal påvirke resultat men ikke totalInntekt
        opplysninger2.leggTil(Faktum(inntektB, Beløp(400.0)))

        Regelkjøring(LocalDate.now(), opplysninger2, minsteinntektRegelsett, beregningRegelsett).evaluer()

        // totalInntekt er låst og skal være uendret (300)
        opplysninger2.finnOpplysning(totalInntekt).verdi shouldBe Beløp(300.0)

        // resultat er IKKE låst og skal oppdateres: 300 + 400 = 700
        opplysninger2.finnOpplysning(resultat).verdi shouldBe Beløp(700.0)
    }

    @Test
    fun `kan sjekke hvilke opplysninger som tilhører et regelsett`() {
        val minsteinntektRegelsett =
            vilkår(tomHjemmel("Minsteinntekt")) {
                regel(inntektA) { innhentes }
                regel(inntektB) { innhentes }
                regel(totalInntekt) { addisjon(inntektA, inntektB) }
            }

        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(inntektA, Beløp(100.0)))
        opplysninger.leggTil(Faktum(inntektB, Beløp(200.0)))

        Regelkjøring(LocalDate.now(), opplysninger, minsteinntektRegelsett).evaluer()

        // totalInntekt skal ha regelsettnavn = "Minsteinntekt"
        val totalInntektOppl = opplysninger.finnOpplysning(totalInntekt)
        totalInntektOppl.utledetAv?.regelsettnavn shouldBe "Minsteinntekt"

        // inntektA er innhentet (ikke utledet), så den har ikke regelsettnavn
        val inntektAOppl = opplysninger.finnOpplysning(inntektA)
        inntektAOppl.utledetAv shouldBe null
    }
}

package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tester at fjernHvis-logikken i Regelkjøring ikke fjerner opplysninger
 * som midlertidig ser "unødvendige" ut på grunn av ufullstendig evaluering.
 *
 * Scenariet: Når det finnes uløste informasjonsbehov (trenger ikke tom),
 * kan ønsketResultat være ufullstendig fordi noen regelsett har skalKjøres=false
 * (manglende gate-opplysning). Uten en guard vil fjernHvis fjerne opplysninger
 * som faktisk trengs når alle behov er løst.
 */
class FjernHvisGuardTest {
    // Opplysningstyper for testen
    private val gate = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Gate")
    private val input = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Input")
    private val result = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Result")
    private val unresolved = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Unresolved")

    // GateRegelsett: produserer gate (eksternt), kjører alltid
    private val gateRegelsett =
        vilkår("Gate") {
            utfall(gate) { innhentes }
        }

    // ProducerRegelsett: kjører bare når gate finnes, produserer result fra input
    private val producerRegelsett =
        vilkår("Producer") {
            skalVurderes { it.har(gate) }
            regel(input) { innhentes }
            utfall(result) { enAv(input) }
        }

    // OtherRegelsett: kjører alltid, har uløst eksternt behov
    private val otherRegelsett =
        vilkår("Other") {
            utfall(unresolved) { innhentes }
        }

    private val prøvingsdato = 10.januar

    @Test
    fun `fjernHvis skal ikke fjerne opplysninger når trenger ikke er tom`() {
        val opplysninger = Opplysninger()

        // Steg 1: Legg til alle opplysninger og kjør komplett evaluering
        opplysninger.leggTil(Faktum(gate, true))
        opplysninger.leggTil(Faktum(input, true))
        opplysninger.leggTil(Faktum(unresolved, true))

        val førsteRegelkjøring = lagRegelkjøring(opplysninger)
        val førsteRapport = førsteRegelkjøring.evaluer()

        // Verifiser at result ble produsert og markert behandlet
        førsteRapport.erFerdig() shouldBe true
        opplysninger.har(result) shouldBe true
        opplysninger.finnOpplysning(result).behandletVed shouldBe prøvingsdato

        // Steg 2: Fjern gate og unresolved (simulerer at de trenger re-fetch)
        // result avhenger av input (IKKE gate), så cascade-fjerning treffer ikke result
        opplysninger.fjernHvis { it.opplysningstype == gate || it.opplysningstype == unresolved }

        // Verifiser at gate er fjernet men result og input fortsatt finnes
        opplysninger.har(gate) shouldBe false
        opplysninger.har(unresolved) shouldBe false
        opplysninger.har(input) shouldBe true
        opplysninger.har(result) shouldBe true

        // Steg 3: Ny regelkjøring - gate mangler → Producer inaktiv (skalKjøres=false)
        // Gate og Unresolved er eksterne som mangler → trenger IKKE tom
        val andreRegelkjøring = lagRegelkjøring(opplysninger)
        val andreRapport = andreRegelkjøring.evaluer()

        // Rapporten sier vi mangler opplysninger (gate og unresolved)
        andreRapport.manglerOpplysninger() shouldBe true

        // MED guard: result bevares (fjernHvis hoppes over fordi trenger ikke er tom)
        // UTEN guard: result fjernes (behandletVed==prøvingsdato, type ikke i brukteOpplysninger
        //             fordi Producer-regelsettet har skalKjøres=false)
        opplysninger.har(result) shouldBe true
    }

    private fun lagRegelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val regelverk =
            Regelverk(
                navn = RegelverkType("TestRegelverk"),
                regelsett = arrayOf(gateRegelsett, producerRegelsett, otherRegelsett),
            )
        val prosess =
            object : Forretningsprosess(regelverk) {
                override fun regelkjøring(opplysninger: Opplysninger) = lagRegelkjøring(opplysninger)

                override fun kontrollpunkter(): List<IKontrollpunkt> = emptyList()

                override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = false

                override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato
            }

        return Regelkjøring(
            regelverksdato = prøvingsdato,
            prøvingsperiode = Regelkjøring.Periode(prøvingsdato),
            opplysninger = opplysninger,
            forretningsprosess = prosess,
        )
    }
}

package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.dag.DatatreBygger
import no.nav.dagpenger.behandling.dag.RegeltreBygger
import no.nav.dagpenger.behandling.dag.printer.MermaidPrinter
import no.nav.dagpenger.behandling.dsl.DSL.Companion.regelsett
import no.nav.dagpenger.behandling.regel.enAvRegel
import no.nav.dagpenger.behandling.regel.multiplikasjon
import no.nav.dagpenger.behandling.regel.oppslag
import no.nav.dagpenger.behandling.regel.størreEnn
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertEquals

class OpplysningerTest {
    @Test
    fun `vilkår er avhengig av andre vilkår`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")
        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        val alder = Opplysningstype("Alder", vilkår)

        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(1.mai, opplysninger)

        opplysninger.leggTil(Faktum(minsteinntekt, true))
        opplysninger.leggTil(Faktum(alder, true))
        opplysninger.leggTil(Faktum(vilkår, true))

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.har(alder))
        assertTrue(opplysninger.har(vilkår))
    }

    @Test
    fun `tillater ikke overlappende opplysninger av samme type`() {
        val opplysningstype = Opplysningstype<Double>("Type")
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(1.mai, opplysninger)

        opplysninger.leggTil(Faktum(opplysningstype, 0.5, Gyldighetsperiode(1.mai, 10.mai)))
        assertThrows<IllegalArgumentException> {
            opplysninger.leggTil(Faktum(opplysningstype, 0.5))
        }
        opplysninger.leggTil(Faktum(opplysningstype, 1.5, Gyldighetsperiode(11.mai)))

        assertEquals(0.5, opplysninger.finnOpplysning(opplysningstype).verdi)
        assertEquals(0.5, opplysninger.finnOpplysning(opplysningstype).verdi)
        Regelkjøring(15.mai, opplysninger) // Bytt til 15. mai for regelkjøringen
        assertEquals(1.5, opplysninger.finnOpplysning(opplysningstype).verdi)
    }

    private object Grunnbeløp {
        const val TEST_GRUNNBELØP = 118620.0

        fun finnFor(dato: LocalDate) = TEST_GRUNNBELØP
    }

    @Test
    fun `finn alle løvnoder som mangler`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")
        val regelsett = Regelsett()

        val nedreTerskelFaktor = Opplysningstype<Double>("Antall G for krav til 12 mnd inntekt")
        val øvreTerskelFaktor = Opplysningstype<Double>("Antall G for krav 36 mnd inntekt")
        val inntekt12 = Opplysningstype<Double>("Inntekt siste 12 mnd")
        val inntekt36 = Opplysningstype<Double>("Inntekt siste 36 mnd")
        val grunnbeløp = Opplysningstype<Double>("Grunnbeløp")
        val virkningsdato = Opplysningstype<LocalDate>("Virkningsdato")

        regelsett.oppslag(grunnbeløp, virkningsdato) { Grunnbeløp.finnFor(it) }

        val nedreTerskel = Opplysningstype<Double>("Inntektskrav for siste 12 mnd")
        regelsett.multiplikasjon(nedreTerskel, nedreTerskelFaktor, grunnbeløp)

        val øvreTerskel = Opplysningstype<Double>("Inntektskrav for siste 36 mnd")
        regelsett.multiplikasjon(øvreTerskel, øvreTerskelFaktor, grunnbeløp)

        val overNedreTerskel = Opplysningstype<Boolean>("Inntekt er over kravet for siste 12 mnd")
        regelsett.størreEnn(overNedreTerskel, inntekt12, nedreTerskel)

        val overØvreTerskel = Opplysningstype<Boolean>("Inntekt er over kravet for siste 36 mnd")
        regelsett.størreEnn(overØvreTerskel, inntekt36, øvreTerskel)

        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        regelsett.enAvRegel(minsteinntekt, overNedreTerskel, overØvreTerskel)

        val fraDato = 10.mai.atStartOfDay()
        val opplysninger =
            Opplysninger(
                listOf(
                    // Setter opp opplysninger med ting som er kjent fra før
                    // Har er ikke lengre gyldig og må hentes på nytt
                    Faktum(inntekt12, 221221.0, Gyldighetsperiode(1.januar, 1.mai)),
                ),
            )
        val regelkjøring = Regelkjøring(fraDato, opplysninger, regelsett)

        // Sett virkningsdato som en opplysning
        opplysninger.leggTil(Faktum(virkningsdato, fraDato.toLocalDate()))

        // Flyt for å innhente manglende opplysninger
        val actual = regelkjøring.trenger(minsteinntekt)

        assertEquals(4, actual.size)
        assertEquals(setOf(inntekt12, inntekt36, nedreTerskelFaktor, øvreTerskelFaktor), actual)

        assertEquals(Grunnbeløp.TEST_GRUNNBELØP, opplysninger.finnOpplysning(grunnbeløp).verdi)

        opplysninger.leggTil(Faktum(nedreTerskelFaktor, 1.5))
        assertEquals(3, regelkjøring.trenger(minsteinntekt).size)

        opplysninger.leggTil(Faktum(øvreTerskelFaktor, 3.0))
        assertEquals(2, regelkjøring.trenger(minsteinntekt).size)

        // Har er ikke lengre gyldig inntekt og må hentes på nytt
        opplysninger.leggTil(Hypotese(inntekt12, 321321.0, Gyldighetsperiode(9.mai)))
        opplysninger.leggTil(Hypotese(inntekt36, 321321.0, Gyldighetsperiode(9.mai)))
        assertEquals(0, regelkjøring.trenger(minsteinntekt).size)

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.finnOpplysning(minsteinntekt).verdi)

        val regelDAG = RegeltreBygger(regelsett).dag()
        val mermaidDiagram = MermaidPrinter(regelDAG).toPrint()
        println(mermaidDiagram)
        println(opplysninger.toString())

        val dataDAG = DatatreBygger(opplysninger).dag()
        println(MermaidPrinter(dataDAG, retning = "LR").toPrint())
    }
}
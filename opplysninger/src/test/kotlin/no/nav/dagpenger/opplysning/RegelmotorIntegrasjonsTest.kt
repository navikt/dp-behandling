package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainAll
import no.nav.dagpenger.opplysning.dag.DatatreBygger
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import no.nav.dagpenger.opplysning.dag.printer.MermaidPrinter
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regelsett.Alderskrav
import no.nav.dagpenger.opplysning.regelsett.Grunnbeløp
import no.nav.dagpenger.opplysning.regelsett.Minsteinntekt
import no.nav.dagpenger.opplysning.regelsett.Virkningsdato
import no.nav.dagpenger.opplysning.regelsett.Virkningsdato.virkningsdato
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class RegelmotorIntegrasjonsTest {
    @Test
    fun `som sjekker minsteinntekt og kravet til alder`() {
        val regelverksdato = 10.mai.atStartOfDay()
        val opplysninger =
            Opplysninger(
                listOf(
                    // Setter opp opplysninger med ting som er kjent fra før
                    // Har er ikke lengre gyldig og må hentes på nytt
                    Faktum(Minsteinntekt.inntekt12, 221221.0, Gyldighetsperiode(1.januar, 1.mai)),
                ),
            )
        val alleVilkår = Opplysningstype.somBoolsk("Vilkår")
        val regelsett =
            Regelsett("Krav til Dagpenger").apply {
                regel(alleVilkår) { alle(Minsteinntekt.minsteinntekt, Alderskrav.vilkår) }
            }
        val regelkjøring =
            Regelkjøring(
                regelverksdato,
                opplysninger,
                regelsett,
                Virkningsdato.regelsett,
                Alderskrav.regelsett,
                Minsteinntekt.regelsett,
            )

        // Sett virkningsdato som en opplysning
        opplysninger.leggTil(
            Faktum(
                Virkningsdato.søknadsdato,
                regelverksdato.toLocalDate(),
                Gyldighetsperiode(regelverksdato.toLocalDate()),
            ),
        )
        opplysninger.leggTil(Faktum(Virkningsdato.sisteDagMedArbeidsplikt, regelverksdato.toLocalDate()))
        opplysninger.leggTil(Faktum(Virkningsdato.sisteDagMedLønn, regelverksdato.toLocalDate()))

        // Flyt for å innhente manglende opplysninger
        val avhengigheterTilalleVilkår = regelkjøring.trenger(alleVilkår)
        val avhengigheterTilMinsteinntekt = regelkjøring.trenger(Minsteinntekt.minsteinntekt)
        val avhengigheterTilAlder = regelkjøring.trenger(Alderskrav.vilkår)

        val forventetAlderskravOpplysninger = setOf(Alderskrav.fødselsdato)
        val forventetMinsteinntektOpplysninger =
            setOf(
                Minsteinntekt.inntekt12,
                Minsteinntekt.inntekt36,
                Minsteinntekt.nedreTerskelFaktor,
                Minsteinntekt.øvreTerskelFaktor,
                Alderskrav.fødselsdato,
            )
        avhengigheterTilAlder shouldContainExactly forventetAlderskravOpplysninger
        avhengigheterTilMinsteinntekt shouldContainExactly forventetMinsteinntektOpplysninger
        avhengigheterTilalleVilkår shouldContainExactly (forventetAlderskravOpplysninger + forventetMinsteinntektOpplysninger)

        regelkjøring.informasjonsbehov(alleVilkår) shouldContainAll
            mapOf(
                Alderskrav.fødselsdato to listOf(),
            )

        opplysninger.leggTil(Faktum(Alderskrav.fødselsdato, LocalDate.of(1953, 2, 10)))
        val faktiskVirkningsdato = opplysninger.finnOpplysning(virkningsdato)
        regelkjøring.informasjonsbehov(alleVilkår) shouldContainAll
            mapOf(
                Minsteinntekt.inntekt12 to listOf(faktiskVirkningsdato),
                Minsteinntekt.inntekt36 to listOf(faktiskVirkningsdato),
                Minsteinntekt.nedreTerskelFaktor to listOf(faktiskVirkningsdato),
                Minsteinntekt.øvreTerskelFaktor to listOf(faktiskVirkningsdato),
            )
        assertEquals(Grunnbeløp.TEST_GRUNNBELØP, opplysninger.finnOpplysning(Minsteinntekt.grunnbeløp).verdi)

        opplysninger.leggTil(Faktum(Minsteinntekt.nedreTerskelFaktor, 1.5))
        assertEquals(3, regelkjøring.trenger(Minsteinntekt.minsteinntekt).size)

        opplysninger.leggTil(Faktum(Minsteinntekt.øvreTerskelFaktor, 3.0))
        assertEquals(2, regelkjøring.trenger(Minsteinntekt.minsteinntekt).size)

        // Har er ikke lengre gyldig inntekt og må hentes på nytt
        opplysninger.leggTil(Hypotese(Minsteinntekt.inntekt12, 321321.0, Gyldighetsperiode(9.mai)))
        opplysninger.leggTil(Hypotese(Minsteinntekt.inntekt36, 321321.0, Gyldighetsperiode(9.mai, 12.mai)))
        assertEquals(0, regelkjøring.trenger(Minsteinntekt.minsteinntekt).size)

        Assertions.assertTrue(opplysninger.har(Minsteinntekt.minsteinntekt))
        Assertions.assertTrue(opplysninger.finnOpplysning(Minsteinntekt.minsteinntekt).verdi)

        Assertions.assertTrue(opplysninger.har(alleVilkår))

        val regelDAG = RegeltreBygger(regelsett, Minsteinntekt.regelsett, Virkningsdato.regelsett, Alderskrav.regelsett).dag()
        val mermaidDiagram = MermaidPrinter(regelDAG).toPrint()
        println(mermaidDiagram)

        val dataDAG = DatatreBygger(opplysninger).dag()
        println(MermaidPrinter(dataDAG, retning = "LR").toPrint())
    }

    @Test
    fun `test av datoer ved å sjekke kravet til alder`() {
        val fraDato = 10.mai.atStartOfDay()
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(fraDato, opplysninger, Alderskrav.regelsett)

        // Flyt for å innhente manglende opplysninger
        val trenger = regelkjøring.trenger(Alderskrav.vilkår)
        assertEquals(setOf(Alderskrav.fødselsdato, virkningsdato), trenger)

        opplysninger.leggTil(Faktum(virkningsdato, LocalDate.of(2020, 2, 29)))
        assertEquals(setOf(Alderskrav.fødselsdato), regelkjøring.trenger(Alderskrav.vilkår))

        opplysninger.leggTil(Faktum(Alderskrav.fødselsdato, LocalDate.of(1953, 2, 10)))

        Assertions.assertTrue(opplysninger.har(Alderskrav.vilkår))
        Assertions.assertTrue(opplysninger.finnOpplysning(Alderskrav.vilkår).verdi)

        val regelDAG = RegeltreBygger(Alderskrav.regelsett).dag()
        val mermaidDiagram = MermaidPrinter(regelDAG).toPrint()
        println(mermaidDiagram)
        println(opplysninger.toString())

        val dataDAG = DatatreBygger(opplysninger).dag()
        println(MermaidPrinter(dataDAG, retning = "LR").toPrint())
    }
}

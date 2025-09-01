package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dag.printer.MermaidPrinter
import no.nav.dagpenger.opplysning.dag.DatatreBygger
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regelsett.Alderskrav
import no.nav.dagpenger.opplysning.regelsett.Alderskrav.fødselsdato
import no.nav.dagpenger.opplysning.regelsett.Grunnbeløp
import no.nav.dagpenger.opplysning.regelsett.Prøvingsdato
import no.nav.dagpenger.opplysning.regelsett.Prøvingsdato.prøvingsdato
import no.nav.dagpenger.opplysning.regelsett.Prøvingsdato.sisteDagMedArbeidsplikt
import no.nav.dagpenger.opplysning.regelsett.Prøvingsdato.sisteDagMedLønn
import no.nav.dagpenger.opplysning.regelsett.Prøvingsdato.søknadsdato
import no.nav.dagpenger.opplysning.regelsett.ReglerForInntektTest
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class RegelmotorIntegrasjonsTest {
    @Test
    fun `som sjekker minsteinntekt og kravet til alder`() {
        val regelverksdato = 10.mai
        val tidligereBehandling =
            Opplysninger.med(
                // Setter opp opplysninger med ting som er kjent fra før
                // Har er ikke lengre gyldig og må hentes på nytt
                Faktum(ReglerForInntektTest.inntekt12, Beløp(221221.0), Gyldighetsperiode(1.januar, 1.mai)),
            )
        val opplysninger = Opplysninger.basertPå(tidligereBehandling)
        val alleVilkår = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Vilkår")
        val regelsett =
            vilkår("Krav til Dagpenger") {
                regel(alleVilkår) { alle(ReglerForInntektTest.minsteinntekt, Alderskrav.vilkår) }
            }
        val regelkjøring =
            Regelkjøring(
                regelverksdato,
                opplysninger,
                regelsett,
                Prøvingsdato.regelsett,
                Alderskrav.regelsett,
                ReglerForInntektTest.regelsett,
            )

        // Sett virkningsdato som en opplysning
        opplysninger
            .leggTil(
                Faktum(
                    søknadsdato,
                    regelverksdato,
                    Gyldighetsperiode(regelverksdato),
                ),
            ).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(sisteDagMedArbeidsplikt, regelverksdato)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(sisteDagMedLønn, regelverksdato)).also { regelkjøring.evaluer() }

        regelkjøring.evaluer().informasjonsbehov shouldContainAll mapOf(fødselsdato to setOf())
        opplysninger.leggTil(Faktum(fødselsdato, LocalDate.of(1953, 2, 10))).also { regelkjøring.evaluer() }

        val faktiskVirkningsdato = opplysninger.finnOpplysning(prøvingsdato)
        with(regelkjøring.evaluer().informasjonsbehov) {
            shouldContainAll(
                mapOf(
                    ReglerForInntektTest.inntekt12 to setOf(faktiskVirkningsdato),
                    ReglerForInntektTest.inntekt36 to setOf(faktiskVirkningsdato),
                ),
            )
        }
        assertEquals(Grunnbeløp.TEST_GRUNNBELØP, opplysninger.finnOpplysning(ReglerForInntektTest.grunnbeløp).verdi)

        // Har er ikke lengre gyldig inntekt og må hentes på nytt
        opplysninger
            .leggTil(
                Hypotese(
                    ReglerForInntektTest.inntekt12,
                    Beløp(321321.0),
                    Gyldighetsperiode(9.mai),
                    utledetAv = Utledning(ReglerForInntektTest.inntekt12.innhentMed(prøvingsdato), listOf(faktiskVirkningsdato)),
                ),
            ).also { regelkjøring.evaluer() }
        opplysninger
            .leggTil(Hypotese(ReglerForInntektTest.inntekt36, Beløp(321321.0), Gyldighetsperiode(9.mai, 12.mai)))
            .also { regelkjøring.evaluer() }

        regelkjøring.evaluer().mangler.shouldBeEmpty()

        assertTrue(opplysninger.har(ReglerForInntektTest.minsteinntekt))
        assertTrue(opplysninger.finnOpplysning(ReglerForInntektTest.minsteinntekt).verdi)

        assertTrue(opplysninger.har(alleVilkår))

        val regelDAG = RegeltreBygger(regelsett, ReglerForInntektTest.regelsett, Prøvingsdato.regelsett, Alderskrav.regelsett).dag()
        val mermaidDiagram = MermaidPrinter(regelDAG).toPrint()
        println(mermaidDiagram)

        val dataDAG = DatatreBygger(opplysninger).dag()
        println(MermaidPrinter(dataDAG, retning = "LR").toPrint())
    }

    @Test
    fun `test av datoer ved å sjekke kravet til alder`() {
        val fraDato = 28.februar(2020)
        val opplysninger = Opplysninger()
        val regelkjøring =
            Regelkjøring(fraDato, opplysninger, TestProsess()) { prøvingsdato ->
                forDato(prøvingsdato)
            }

        // Flyt for å innhente manglende opplysninger
        val mangler = regelkjøring.evaluer().mangler
        assertEquals(setOf(fødselsdato, søknadsdato, sisteDagMedArbeidsplikt, sisteDagMedLønn), mangler)

        // Skal kortslutte behovet for de tre underliggende opplysningene
        opplysninger.leggTil(Faktum(prøvingsdato, LocalDate.of(2020, 2, 29)))
        val evaluer = regelkjøring.evaluer()
        assertEquals(setOf(fødselsdato), evaluer.mangler)

        opplysninger.leggTil(Faktum(fødselsdato, LocalDate.of(1953, 2, 10))).also { regelkjøring.evaluer() }

        assertTrue(opplysninger.har(Alderskrav.vilkår))
        assertTrue(opplysninger.finnOpplysning(Alderskrav.vilkår).verdi)

        val regelDAG = RegeltreBygger(Alderskrav.regelsett).dag()
        val mermaidDiagram = MermaidPrinter(regelDAG).toPrint()
        println(mermaidDiagram)
        println(opplysninger.toString())

        val dataDAG = DatatreBygger(opplysninger).dag()
        println(MermaidPrinter(dataDAG, retning = "LR").toPrint())
    }

    @Test
    fun `Rekjører regler når opplysninger en avhenger av er nyere`() {
        val fraDato = 10.mai
        val opplysninger = Opplysninger()
        val a0 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "A0")
        val a = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "A")
        val b = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "B")
        val c = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "C")
        val d = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "D")
        val regelsett =
            vilkår("test") {
                regel(a0) { innhentes }
                regel(a) { alle(a0) }
                regel(d) { innhentes }
                regel(b) { alle(a, d) }
                regel(c) { alle(b) }
            }
        val regelkjøring =
            Regelkjøring(
                fraDato,
                opplysninger,
                object : Forretningsprosess {
                    override val regelverk: Regelverk
                        get() = TODO("Not yet implemented")

                    override fun regelkjøring(
                        opplysninger: Opplysninger,
                        regeldato: LocalDate?,
                    ): Regelkjøring {
                        TODO("Not yet implemented")
                    }

                    override fun kontrollpunkter(): List<IKontrollpunkt> {
                        TODO("Not yet implemented")
                    }

                    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
                        TODO("Not yet implemented")
                    }

                    override fun regelsett(): List<Regelsett> = listOf(regelsett)

                    override fun ønsketResultat(opplysninger: LesbarOpplysninger) = listOf(c)
                },
            ) { prøvingsdato ->
                forDato(prøvingsdato)
            }

        opplysninger.leggTil(Faktum(a0, true)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(d, true)).also { regelkjøring.evaluer() }

        opplysninger.har(c) shouldBe true
        opplysninger.finnOpplysning(c).verdi shouldBe true

        opplysninger.leggTil(Faktum(a0, false)).also { regelkjøring.evaluer() }

        opplysninger.finnOpplysning(c).verdi shouldBe false
    }

    @Test
    fun `Kortslutte del av regeltreet når saksbehandler overstyrer opplysning`() {
        val fraDato = 10.mai
        val opplysninger = Opplysninger()
        val a0 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "A0")
        val a = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "A")
        val b = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "B")
        val c = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "C")
        val d = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "D")
        val regelsett =
            vilkår("test") {
                regel(a0) { innhentes }
                regel(a) { alle(a0) }
                regel(d) { alle(a0) }
                regel(b) { alle(a, d) }
                regel(c) { alle(b) }
            }
        val regelkjøring =
            Regelkjøring(
                fraDato,
                opplysninger,
                object : Forretningsprosess {
                    override val regelverk: Regelverk
                        get() = TODO("Not yet implemented")

                    override fun regelkjøring(
                        opplysninger: Opplysninger,
                        regeldato: LocalDate?,
                    ): Regelkjøring {
                        TODO("Not yet implemented")
                    }

                    override fun kontrollpunkter(): List<IKontrollpunkt> {
                        TODO("Not yet implemented")
                    }

                    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
                        TODO("Not yet implemented")
                    }

                    override fun regelsett(): List<Regelsett> = listOf(regelsett)

                    override fun ønsketResultat(opplysninger: LesbarOpplysninger) = listOf(c)
                },
            ) { prøvingsdato ->
                forDato(prøvingsdato)
            }

        opplysninger.leggTil(Faktum(b, true)).also { regelkjøring.evaluer() }

        regelkjøring.evaluer().mangler.shouldBeEmpty()

        opplysninger.har(c) shouldBe true
        opplysninger.finnOpplysning(c).verdi shouldBe true

        opplysninger.leggTil(Faktum(a0, false)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(d, false)).also { regelkjøring.evaluer() }

        opplysninger.finnOpplysning(c).verdi shouldBe true
    }

    @Test
    fun `kjør 2 ganger`() {
        val fraDato = 10.mai
        val a = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "a")
        val b = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "b")
        val c = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "c")
        val d = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "d")
        val e = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "e")
        val f = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "f")
        val g = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "g")
        val h = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "h")
        val i = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "i")

        val regelsett =
            vilkår("test av regelsett") {
                regel(e) { innhentes }
                regel(f) { innhentes }
                regel(g) { innhentes }
                regel(h) { innhentes }
                regel(d) { alle(g, h) }
                regel(c) { enAv(f) }
                regel(b) { enAv(e) }
                regel(a) { alle(b, c, d) }
            }

        val regelDAG = RegeltreBygger(regelsett).dag()
        val mermaidDiagram = MermaidPrinter(regelDAG).toPrint()
        println(mermaidDiagram)
        val opplysninger = Opplysninger()
        val regelkjøring =
            Regelkjøring(
                fraDato,
                opplysninger,
                object : Forretningsprosess {
                    override val regelverk: Regelverk
                        get() = TODO("Not yet implemented")

                    override fun regelkjøring(
                        opplysninger: Opplysninger,
                        regeldato: LocalDate?,
                    ): Regelkjøring {
                        TODO("Not yet implemented")
                    }

                    override fun kontrollpunkter(): List<IKontrollpunkt> {
                        TODO("Not yet implemented")
                    }

                    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
                        TODO("Not yet implemented")
                    }

                    override fun regelsett(): List<Regelsett> = listOf(regelsett)

                    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> {
                        val ønsker = mutableListOf<Opplysningstype<*>>()
                        ønsker.add(d)
                        if (opplysninger.mangler(d)) return ønsker
                        if (opplysninger.finnOpplysning(d).verdi) {
                            ønsker.add(a)
                        }
                        return ønsker
                    }
                },
            ) { prøvingsdato ->
                forDato(prøvingsdato)
            }

        opplysninger.leggTil(Faktum(e, true))
        opplysninger.leggTil(Faktum(f, true))
        opplysninger.leggTil(Faktum(g, true))
        opplysninger.leggTil(Faktum(h, true))
        opplysninger.leggTil(Faktum(i, true)) // "fast" opplysning

        regelkjøring.evaluer().mangler.shouldBeEmpty()

        // Sjekk at A blir produsert
        opplysninger.har(a) shouldBe true
        opplysninger.finnOpplysning(a).verdi shouldBe true

        // Endre verdi av G
        opplysninger.leggTil(Faktum(g, false))

        regelkjøring.evaluer().mangler.shouldBeEmpty()

        // D har blitt oppdatert på grunn av endring i G
        opplysninger.har(d) shouldBe true
        opplysninger.finnOpplysning(d).verdi shouldBe false

        // Fordi at D er falsk, så A skal ikke ha blitt utledet på grunn av ønsketResultat
        opplysninger.har(a) shouldBe false

        // Denne skal aldri forsvinne (emulerer opplysninger uten regel)
        opplysninger.har(i) shouldBe true

        opplysninger.har(b) shouldBe false
        opplysninger.har(c) shouldBe false
        opplysninger.har(e) shouldBe false
        opplysninger.har(f) shouldBe false
    }
}

private class TestProsess : Forretningsprosess {
    override val regelverk: Regelverk
        get() = TODO("Not yet implemented")

    override fun regelkjøring(
        opplysninger: Opplysninger,
        regeldato: LocalDate?,
    ): Regelkjøring {
        TODO("Not yet implemented")
    }

    override fun kontrollpunkter(): List<IKontrollpunkt> {
        TODO("Not yet implemented")
    }

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
        TODO("Not yet implemented")
    }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
        TODO("Not yet implemented")
    }

    override fun regelsett(): List<Regelsett> = listOf(Alderskrav.regelsett, Prøvingsdato.regelsett)

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> = listOf(Alderskrav.vilkår, prøvingsdato)
}

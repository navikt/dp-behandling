package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dag.printer.MermaidPrinter
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.uuid.UUIDv7
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RegelverkTest {
    private val typeA = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "A")
    private val typeB = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "B")
    private val typeC = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "C")
    private val typeE = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "E")
    private val typeF = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "F")

    private val r1 =
        vilkår("Søknadstidspunkt") {
            regel(typeA) { innhentes }
            regel(typeA, 1.juli) { innhentes }
            regel(typeA, 1.mai) { innhentes }
            regel(typeE) { erSann(typeF) }
            regel(typeE, 1.juni) { erSann(typeF) }
        }
    private val r2 = vilkår("Inntekt") { regel(typeB) { innhentMed(typeA) } }
    private val r3 = vilkår("Dagpenger") { regel(typeC) { innhentMed(typeA, typeB) } }

    private val regelverk = Regelverk(null, r1, r2, r3)

    @Test
    fun `finner alle nødvendige regler for en opplysning helt til venstre`() {
        regelverk.reglerFor(typeA).shouldContainExactlyInAnyOrder(r1.regler())
        regelverk.reglerFor(typeA, 5.mai).shouldContainExactlyInAnyOrder(r1.regler(5.mai))

        regelverk.regelsettFor(typeA).shouldContainExactlyInAnyOrder(r1)
    }

    @Test
    fun `finner alle nødvendige regler for en opplysning i midten`() {
        regelverk.reglerFor(typeB).shouldContainExactlyInAnyOrder(r1.regler() + r2.regler())
        regelverk.reglerFor(typeB, 5.mai).shouldContainExactlyInAnyOrder(r1.regler(5.mai) + r2.regler(5.mai))

        regelverk.regelsettFor(typeB).shouldContainExactlyInAnyOrder(r1, r2)
    }

    @Test
    fun `finner alle nødvendige regler for en opplysning helt til høyre`() {
        regelverk.reglerFor(typeC).shouldContainExactlyInAnyOrder(r1.regler() + r2.regler() + r3.regler())
        regelverk.reglerFor(typeC, 5.mai).shouldContainExactlyInAnyOrder(r1.regler(5.mai) + r2.regler(5.mai) + r3.regler(5.mai))

        regelverk.regelsettFor(typeC).shouldContainExactlyInAnyOrder(r1, r2, r3)
    }

    @Test
    fun `kan også vise regelverket som ett tre`() {
        val dag = regelverk.regeltreFor(typeC)

        MermaidPrinter(dag).also {
            it.toPrint() shouldBe forventedDiagram
        }
    }

    @Language("Mermaid")
    private val forventedDiagram =
        """
        |graph RL
        |  A["Dagpenger"] -->|"avhenger av"| B["Søknadstidspunkt"]
        |  A["Dagpenger"] -->|"avhenger av"| C["Inntekt"]
        |  C["Inntekt"] -->|"avhenger av"| B["Søknadstidspunkt"]
        """.trimMargin()
}

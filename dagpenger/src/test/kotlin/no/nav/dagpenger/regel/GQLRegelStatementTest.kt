package no.nav.dagpenger.regel

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import org.junit.jupiter.api.Test

class GQLRegelStatementTest {
    private val regeltre =
        RegeltreBygger(
            *RegelverkDagpenger.regelsett.toTypedArray(),
        ).dag()

    @Test
    fun `Lager Cypher statements for Dagpengeregelverket`() {
        val printer = GrafPrinter(regeltre, CypherFormatter)
        val statements = printer.toPrint()
        statements.shouldNotBeEmpty()
        statements.shouldContain("CREATE")
        statements.shouldContain(":Opplysning")
        statements.shouldContain(":Regel:")
        statements.shouldContain("-[:PRODUSERER]->")
        statements.shouldContain("-[:AVHENGER_AV]->")
        // println(statements)
    }

    @Test
    fun `Lager GQL statements for Dagpengeregelverket`() {
        val printer = GrafPrinter(regeltre, GQLFormatter)
        val statements = printer.toPrint()
        statements.shouldNotBeEmpty()
        statements.shouldContain("INSERT")
        statements.shouldContain(":Opplysning")
        statements.shouldContain(":Regel&")
        statements.shouldContain("-[:PRODUSERER]->")
        statements.shouldContain("-[:AVHENGER_AV]->")
        // println(statements)
    }
}

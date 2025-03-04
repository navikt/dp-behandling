package no.nav.dagpenger.regel

import io.kotest.matchers.string.shouldNotBeEmpty
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import org.junit.jupiter.api.Test

class GQLRegelStatementTest {
    @Test
    fun `Lager GQL statements for Dagpengeregelverket`() {
        val bygger =
            RegeltreBygger(
                *RegelverkDagpenger.regelsett.toTypedArray(),
            )

        val regeltre = bygger.dag()
        val gQLStatementPrinter = GQLStatementPrinter(regeltre)
        val statements = gQLStatementPrinter.toPrint()
        statements.shouldNotBeEmpty()
        // println(statements)
    }
}

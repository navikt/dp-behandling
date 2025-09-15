package no.nav.dagpenger.regel

import com.spun.util.persistence.Loader
import no.nav.dagpenger.dag.printer.MermaidPrinter
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class DiagramTest {
    private companion object {
        val path = "${
            Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")
        }/docs/"
        val options = Options().forFile().withExtension(".md")
    }

    @Test
    fun `printer hele dagpengeregeltreet`() {
        val bygger =
            RegeltreBygger(
                *RegelverkDagpenger.regelsett.toTypedArray(),
            )

        val regeltre = bygger.dag()
        val mermaidPrinter = MermaidPrinter(regeltre)
        val output = mermaidPrinter.toPrint()
        assertTrue(output.contains("graph RL"))

        println(output)

        @Language("Markdown")
        val markdown =
            """
                    ># Regeltre - Dagpenger (inngangsvilkår)
                    >
                    >## Regeltre
                    >
                    >```mermaid
                    >${output.trim()}
                    >```
                    """.trimMargin(">")
        skriv(
            markdown,
        )
    }

    @Test
    fun `lager tre av regelsettene`() {
        val regelverk =
            Regelverk(
                KravPåDagpenger.harLøpendeRett,
                *RegelverkDagpenger.regelsett.toTypedArray(),
            )

        regelverk.regeltreFor(Minsteinntekt.minsteinntekt).also {
            val b = MermaidPrinter(it)
            println(b.toPrint())
        }
    }

    fun skriv(dokumentasjon: String) {
        Approvals.namerCreater = Loader { NamerWrapper({ "regeltre-dagpenger" }, { path }) }
        Approvals
            .verify(
                dokumentasjon,
                options,
            )
    }
}

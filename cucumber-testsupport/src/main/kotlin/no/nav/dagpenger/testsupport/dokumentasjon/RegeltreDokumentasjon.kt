package no.nav.dagpenger.testsupport.dokumentasjon

import com.spun.util.persistence.Loader
import io.cucumber.java.Scenario
import io.cucumber.plugin.ConcurrentEventListener
import io.cucumber.plugin.event.EmbedEvent
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestSourceParsed
import io.cucumber.plugin.event.TestSourceRead
import no.nav.dagpenger.dag.printer.MermaidPrinter
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import java.nio.file.Paths

data class RegeltreDokumentasjonOppsett(
    val dokumentasjonskatalog: String = "regler",
    val regelsettPerTag: Map<String, Regelsett>,
)

abstract class RegeltreDokumentasjonPlugin(
    private val oppsett: RegeltreDokumentasjonOppsett,
) : ConcurrentEventListener {
    private val regeltrær = mutableMapOf<String, String>()
    private val tester = mutableMapOf<String, String>()
    private val dokumenter = mutableMapOf<String, String>()

    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ ->
            regeltrær
                .map { (uri, regeltre) ->
                    val navn = tester[uri]
                    val gherkinSource = dokumenter[uri]
                    Regeldokumentasjon(navn!!, regeltre, gherkinSource!!)
                }.forEach { (navn, regeltreDiagram, gherkinSource) ->
                    val markdown =
                        """
                    ># $navn
                    >
                    >## Regeltre
                    >
                    >```mermaid
                    >${regeltreDiagram.trim()}
                    >```
                    >
                    >## Akseptansetester
                    >
                    >```gherkin
                    >${gherkinSource.trim()}
                    >``` 
                    """.trimMargin(">")
                    skriv(
                        navn,
                        markdown,
                    )
                }
        }

        publisher.registerHandlerFor(TestSourceRead::class.java) { event ->
            dokumenter[event.uri.toString()] = event.source
        }
        publisher.registerHandlerFor(TestSourceParsed::class.java) { event ->
            tester[event.uri.toString()] =
                event.nodes
                    .first()
                    .name
                    .get()
        }
        publisher.registerHandlerFor(EmbedEvent::class.java) { event ->
            regeltrær[event.testCase.uri.toString()] = String(event.data)
        }
    }

    private companion object {
        val path =
            "${Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")}/docs/"
        val options = Options().forFile().withExtension(".md")
    }

    protected fun skriv(
        tittel: String,
        dokumentasjon: String,
    ) {
        Approvals.namerCreater = Loader { NamerWrapper({ "${oppsett.dokumentasjonskatalog}/$tittel" }, { path }) }
        Approvals.verify(
            dokumentasjon,
            options,
        )
    }

    private data class Regeldokumentasjon(
        var navn: String,
        var regeltreDiagram: String,
        var gherkinSource: String,
    )
}

fun dokumenterRegeltre(
    scenario: Scenario,
    oppsett: RegeltreDokumentasjonOppsett,
) {
    val test = scenario.sourceTagNames.first { it.startsWith("@regel") }
    val regelsett =
        requireNotNull(oppsett.regelsettPerTag[test]) {
            "Fant ikke regelsett for $test, det må mappes i RegeltreDokumentasjonOppsett"
        }
    val regeltre = RegeltreBygger(regelsett)
    val tre = MermaidPrinter(regeltre.dag()).toPrint()
    scenario.attach(tre, "text/markdown", "regeltre.md")
}

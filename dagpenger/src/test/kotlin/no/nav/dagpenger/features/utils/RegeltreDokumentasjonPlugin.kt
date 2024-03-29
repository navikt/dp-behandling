package no.nav.dagpenger.features.utils

import com.spun.util.persistence.Loader
import io.cucumber.java.After
import io.cucumber.java.Scenario
import io.cucumber.plugin.ConcurrentEventListener
import io.cucumber.plugin.event.EmbedEvent
import io.cucumber.plugin.event.EventPublisher
import io.cucumber.plugin.event.TestRunFinished
import io.cucumber.plugin.event.TestSourceParsed
import io.cucumber.plugin.event.TestSourceRead
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import no.nav.dagpenger.opplysning.dag.printer.MermaidPrinter
import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Opptjeningstid
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.intellij.lang.annotations.Language
import java.nio.file.Paths

@After("@dokumentasjon")
fun dokumentasjon(scenario: Scenario) {
    val test = scenario.sourceTagNames.first { it.startsWith("@regel") }
    val regler =
        mapOf(
            "@regel-alder" to Alderskrav.regelsett,
            "@regel-minsteinntekt" to Minsteinntekt.regelsett,
            "@regel-opptjeningstid" to Opptjeningstid.regelsett,
        )
    println("Lager dokumentasjon for $test")
    val regeltre = RegeltreBygger(regler[test]!!)
    val tre = MermaidPrinter(regeltre.dag()).toPrint()
    scenario.attach(tre, "text/markdown", "regeltre.md")
}

class RegeltreDokumentasjonPlugin : ConcurrentEventListener {
    private val regeltrær = mutableMapOf<String, String>()
    private val tester = mutableMapOf<String, String>()
    private val dokumenter = mutableMapOf<String, String>()

    override fun setEventPublisher(publisher: EventPublisher) {
        publisher.registerHandlerFor(TestRunFinished::class.java) { _ ->
            regeltrær.map { (uri, regeltre) ->
                val navn = tester[uri]
                val gherkinSource = dokumenter[uri]
                Regeldokumentasjon(navn!!, regeltre, gherkinSource!!)
            }.forEach { (navn, regeltreDiagram, gherkinSource) ->
                @Language("Markdown")
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
            tester[event.uri.toString()] = event.nodes.first().name.get()
        }
        publisher.registerHandlerFor(EmbedEvent::class.java) { event ->
            regeltrær[event.testCase.uri.toString()] = String(event.data)
        }
    }

    private companion object {
        val path = "${
            Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")
        }/docs/"
        val options = Options().forFile().withExtension(".md")
    }

    fun skriv(
        tittel: String,
        dokumentasjon: String,
    ) {
        Approvals.namerCreater = Loader { NamerWrapper({ "regler/$tittel" }, { path }) }
        Approvals
            .verify(
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

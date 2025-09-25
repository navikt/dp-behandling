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
import no.nav.dagpenger.dag.printer.MermaidPrinter
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.FulleYtelser
import no.nav.dagpenger.regel.HarFramsattKrav
import no.nav.dagpenger.regel.Meldeplikt
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.Opptjeningstid
import no.nav.dagpenger.regel.Permittering
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien
import no.nav.dagpenger.regel.ReellArbeidssøker
import no.nav.dagpenger.regel.RegistrertArbeidssøker
import no.nav.dagpenger.regel.Samordning
import no.nav.dagpenger.regel.StreikOgLockout
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.Utdanning
import no.nav.dagpenger.regel.Utestengning
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.fastsetting.PermitteringFraFiskeindustrienFastsetting
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting
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
            "@regel-reell-arbeidssøker" to ReellArbeidssøker.regelsett,
            "@regel-meldeplikt" to Meldeplikt.regelsett,
            "@regel-opphold" to Opphold.regelsett,
            "@regel-tap-arbeidsinntekt-og-arbeidstid" to TapAvArbeidsinntektOgArbeidstid.regelsett,
            "@regel-utdanning" to Utdanning.regelsett,
            "@regel-verneplikt" to Verneplikt.regelsett,
            "@regel-utestengning" to Utestengning.regelsett,
            "@regel-fulle-ytelser-eller-afp" to FulleYtelser.regelsett,
            "@regel-streik-og-lockout" to StreikOgLockout.regelsett,
            "@regel-dapengeperiode" to Dagpengeperiode.regelsett,
            "@regel-dagpengegrunnlag" to Dagpengegrunnlag.regelsett,
            "@regel-dagpengensStørrelse" to DagpengenesStørrelse.regelsett,
            "@regel-egenandel" to Egenandel.regelsett,
            "@regel-verneplikt-fastsetting" to VernepliktFastsetting.regelsett,
            "@regel-samordning" to Samordning.regelsett,
            "@regel-samordning-utenfor-folketrygden" to SamordingUtenforFolketrygden.regelsett,
            "@regel-permittering" to Permittering.regelsett,
            "@regel-permittering-fastsetting" to PermitteringFastsetting.regelsett,
            "@regel-registrert-arbeidssøker" to RegistrertArbeidssøker.regelsett,
            "@regel-permittering-fiskeindustrien" to PermitteringFraFiskeindustrien.regelsett,
            "@regel-permitteringFiskeindustrien-fastsetting" to PermitteringFraFiskeindustrienFastsetting.regelsett,
            "@regel-framsattkrav" to HarFramsattKrav.regelsett,
        )
    val regelsett = regler[test]
    requireNotNull(regelsett) { "Fant ikke regelsett for $test, det må mappes manuelt i RegeltreDokumentasjonPlugin" }
    val regeltre = RegeltreBygger(regelsett)
    val tre = MermaidPrinter(regeltre.dag()).toPrint()
    scenario.attach(tre, "text/markdown", "regeltre.md")
}

class RegeltreDokumentasjonPlugin : ConcurrentEventListener {
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

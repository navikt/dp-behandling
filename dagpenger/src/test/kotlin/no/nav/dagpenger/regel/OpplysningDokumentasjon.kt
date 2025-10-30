package no.nav.dagpenger.regel

import com.spun.util.persistence.Loader
import no.nav.dagpenger.opplysning.Avklaringkode.Companion.avklaringer
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class OpplysningDokumentasjon {
    val opplysninger = RegelverkDagpenger.produserer + fagsakIdOpplysningstype

    @Test
    fun `opplysninger gruppert etter regelsett`() {
        val regelverk =
            RegelverkDagpenger.regelsett.sortedBy { it.hjemmel.paragraf }.associateWith {
                it.produserer
            }

        val behov = RegelverkDagpenger.regelsett.flatMap { it.behov }
        val markdown =
            StringBuilder(
                """
            ># Dokumentasjon av opplysninger
            >
            >Dette er opplysninger som blir brukt av regelverket. 
            >
            > UUID og datatype er en unik identifikator for opplysningstypen. Den skal _ALDRI_ endres. Beskrivelse og behovId kan endres. 
            > 
            > For nye opplysningtyper, generer en ny UUID og legg til.
            > 
            > Generering av UUID kan gjøres med UUIDv7.ny() i Kotlin
            >""".trimMargin(">"),
            )

        markdown.appendLine("## Regelsett")
        regelverk.forEach { (regelsett, opplysninger) ->
            markdown.appendLine("### ${regelsett.hjemmel}")
            markdown.appendLine("*Type:* ${regelsett.type}")

            if (regelsett.avklaringer.isNotEmpty()) {
                markdown.appendLine("#### Avklaringer")

                regelsett.avklaringer.forEach {
                    markdown.appendLine("- ${it.kode} - [${it.tittel}](./avklaringer.approved.md#${tilMarkdownURL(it.tittel)})")
                }
            }

            if (regelsett.avhengerAv.isNotEmpty()) {
                val andreRegeverk = regelverk.filter { (_, produserer) -> produserer.any { it in regelsett.avhengerAv } }

                markdown.appendLine("#### Avhenger på data fra")
                andreRegeverk.forEach { (avhengighet, _) ->
                    markdown.appendLine("- [${avhengighet.hjemmel}](#${tilMarkdownURL(avhengighet.hjemmel.toString())})")
                }
            }

            markdown.appendLine(
                """
                >#### Opplysninger
                >|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
                >|---|---|---|---|---|
                """.trimMargin(">"),
            )

            opplysninger.sortedBy { it.id.uuid }.forEach {
                val behovId = if (it in behov) it.behovId else ""

                markdown.appendLine(
                    "|${it.id.uuid}|${it.navn}|${it.datatype}|${it.datatype.klasse.simpleName}|$behovId|",
                )
            }
        }

        skriv("opplysninger", markdown.toString())
    }

    @Test
    fun `dokumenterer hvilke opplysninger som innhentes via behov`() {
        val regler = RegelverkDagpenger.regelsett

        val behov = regler.flatMap { it.behov }

        val markdown =
            """
            ># Dokumentasjon på behov for opplysninger
            >
            >Dette er opplysninger som blir innhentet som en del av dagpengebehandlingen. De publiseres som behov på rapiden.
            >
            >|Behov|Beskrivelse|Logisk datatype|Datatype|
            >|---|---|---|---|
            ${
                behov.sortedBy { it.behovId }.joinToString("\n") {
                    ">|${it.behovId} | ${it.navn} | ${it.datatype}|${it.datatype.klasse.simpleName}|"
                }
            }
            """.trimMargin(">")

        skriv("behov", markdown)
    }

    @Test
    fun `dokumenterer avklaringer`() {
        val markdown = StringBuilder()
        markdown.appendLine("# Avklaringer")

        // language="Markdown"
        markdown.appendLine(
            """Avklaringer opprettes hvor regelmotoren er usikker på enten fakta eller riktig vei videre.
                |
            """.trimMargin(),
        )
        // language="Markdown"
        markdown.appendLine(
            """Avklaringer opprettes av "kontrollpunkt" som gjør en vurdering av opplysninger og ser om det avklaringen er nødvendig.
                |
                |Endringer i opplysninger vil automatisk lukke avklaringen om kontrollpunktet sier den ikke lengre er nødvendig.
                |Tilsvarende vil avklaringen åpnes opp igjen om opplysningene endres.
                |
            """.trimMargin(),
        )

        val regelsett =
            RegelverkDagpenger.regelsett
                .flatMap { regel -> regel.avklaringer.map { it to regel } }
                .groupBy({ it.first }, { it.second })

        avklaringer.sortedBy { it.kode }.forEach {
            markdown.appendLine("## ${it.tittel}")
            markdown.appendLine("**Kode:** `${it.kode}`\n")

            if (it.beskrivelse.isNotEmpty()) {
                markdown.appendLine("### Beskrivelse")
                markdown.appendLine("${newlineToBr(it.beskrivelse)}\n")
            }

            if (regelsett[it] != null) {
                markdown.appendLine("### Tilknyttet regelsett")
                regelsett[it]?.forEach { bruktAv ->
                    markdown.appendLine("- [${bruktAv.hjemmel}](./opplysninger.approved.md#${tilMarkdownURL(bruktAv.hjemmel.toString())}")
                }
            }

            if (!it.kanKvitteres) markdown.appendLine("❌ Kan ikke kvitteres\n")
            if (!it.kanAvbrytes) markdown.appendLine("❌ Kan ikke avbrytes\n")

            markdown.appendLine("---")
        }

        skriv("avklaringer", markdown.toString())
    }

    private fun tilMarkdownURL(tekst: String) =
        tekst
            .lowercase()
            .replace("§", "")
            .replace(".", "")
            .replace(" ", "-")

    private fun newlineToBr(tekst: String) = tekst.replace("\n", "<br>")

    private companion object {
        val path = "${Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")}/docs/"
        val options = Options().forFile().withExtension(".md")
    }

    private fun skriv(
        filnavn: String,
        innhold: String,
    ) {
        Approvals.namerCreater = Loader { NamerWrapper({ filnavn }, { path }) }
        Approvals.verify(innhold, options)
    }
}

package no.nav.dagpenger.regel.mottak

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

internal class Migreringsdialog {
    private val jsonMapper = JsonMapper.builder().build()
    private val meldinger = mutableListOf<Melding>()
    private val sendteTyper = mutableSetOf<String>()

    fun sendt(
        runde: Int,
        json: String,
    ) {
        val melding = jsonMapper.readTree(json)
        val nyeFakta =
            melding["opplysninger"].values().filter {
                sendteTyper.add(it["uuid"].asString())
            }
        meldinger.add(Melding(runde, "Avsender → mottak", melding, nyeFakta))
    }

    fun mottatt(
        runde: Int,
        melding: JsonNode,
    ) {
        meldinger.add(Melding(runde, "Mottak → avsender", jsonMapper.readTree(melding.toString())))
    }

    fun markdown(avgjørelse: String): String =
        buildString {
            appendLine("# Meldingsdialog ved migrering")
            appendLine()
            appendLine("Tekniske ID-er, ident og meldingstidsstempler er erstattet med plassholdere.")
            appendLine("Opplysningstype-UUID-er, faktaverdier og gyldighetsdatoer er beholdt.")
            appendLine()
            appendLine("| Runde | Retning | Melding | Opplysninger |")
            appendLine("| --- | --- | --- | --- |")
            meldinger.forEach {
                val navn = it.innhold["@event_name"].asString()
                val antall =
                    if (navn == "migrering_startet") {
                        "${it.innhold["opplysninger"].size()} totalt (nye: ${it.nyeFakta.size})"
                    } else {
                        "${it.innhold["manglendeOpplysninger"].size()} mangler"
                    }
                appendLine("| ${it.runde} | ${it.retning} | `$navn` | $antall |")
            }
            meldinger.groupBy { it.runde }.forEach { (runde, meldingerIRunden) ->
                appendLine()
                appendLine("## Runde $runde")
                meldingerIRunden.forEach { melding ->
                    val sendt = melding.innhold["@event_name"].asString() == "migrering_startet"
                    appendLine()
                    appendLine("### ${melding.retning}")
                    appendLine()
                    if (sendt) {
                        appendLine("Nye fakta i denne runden (JSON-meldingen inneholder også tidligere sendte fakta).")
                        appendLine()
                        appendLine("| UUID | Navn | Verdi | Fra og med | Til og med |")
                        appendLine("| --- | --- | --- | --- | --- |")
                        melding.nyeFakta.sortedBy { it["uuid"].asString() }.forEach { faktum ->
                            val verdi = faktum["verdi"]
                            val vistVerdi = if (verdi.isObject || verdi.isArray) "Strukturert verdi (se JSON)" else verdi.toString()
                            appendLine(
                                "| ${faktum["uuid"].asString()} | ${celle(faktum["navn"].asString())} | " +
                                    "${celle(vistVerdi)} | ${faktum["gyldigFraOgMed"].asString()} | " +
                                    "${faktum.get("gyldigTilOgMed")?.asString() ?: "Ingen sluttdato"} |",
                            )
                        }
                        if (melding.nyeFakta.isEmpty()) appendLine("| | Ingen nye fakta | | | |")
                    } else {
                        appendLine("| Manglende UUID | Navn |")
                        appendLine("| --- | --- |")
                        melding.innhold["manglendeOpplysninger"].values().forEach {
                            appendLine("| ${it["uuid"].asString()} | ${celle(it["navn"].asString())} |")
                        }
                    }
                    appendLine()
                    appendLine("<details>")
                    appendLine("<summary>Hele JSON-meldingen</summary>")
                    appendLine()
                    appendLine("```json")
                    appendLine(
                        normaliser(
                            jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(melding.innhold),
                        ),
                    )
                    appendLine("```")
                    appendLine()
                    appendLine("</details>")
                }
            }
            appendLine()
            appendLine("## Resultat i testen")
            appendLine()
            appendLine("Mottaket videresendte hendelsen én gang, og testen opprettet én domenebehandling.")
            appendLine("Etter regelkjøring og rettighetsplugin er beregnet avgjørelse `$avgjørelse`.")
            appendLine("Dette er testobservasjoner, ikke en publisert svarmelding eller et fattet vedtak.")
        }

    private fun celle(verdi: String): String =
        normaliser(verdi)
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("|", "&#124;")
            .replace("\n", "<br>")

    private fun normaliser(json: String): String =
        json.replace(Regex("\"(@id|@opprettet|migreringId|inntektsId|ident|id|time|opprettet)\"(\\s*:\\s*)\"[^\"]*\"")) {
            "\"${it.groupValues[1]}\"${it.groupValues[2]}\"<${it.groupValues[1]}>\""
        }

    private data class Melding(
        val runde: Int,
        val retning: String,
        val innhold: JsonNode,
        val nyeFakta: List<JsonNode> = emptyList(),
    )
}

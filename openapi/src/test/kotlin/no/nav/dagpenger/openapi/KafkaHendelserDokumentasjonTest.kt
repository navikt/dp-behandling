package no.nav.dagpenger.openapi

import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.ScalarNode
import java.io.File

/**
 * Genererer `docs/kafka-events-catalog.md` automatisk fra `webhooks`-seksjonen i
 * `behandling-api.yaml`. Kafka-hendelser er ikke en del av HTTP-API-et, men dokumenteres i
 * OpenAPI-speccen som webhooks (OAS 3.1) for å gjøre dem discoverable (Swagger UI/Redoc viser dem
 * i en egen "Webhooks"-seksjon).
 *
 * Katalogen holder seg bevisst tynn — selve schemaet (felter, typer, påkrevd/valgfritt) er allerede
 * godt dekket av OpenAPI-speccen selv (Swagger UI/Redoc, eller lenken under hver hendelse). Det
 * denne fila skal svare på er *hvilke* hendelser som finnes, en kort forklaring av *når* de
 * publiseres, og *hvordan* de henger sammen (se flytskjemaet).
 *
 * Legg til en ny Kafka-hendelse ved å legge den til under `webhooks:` i behandling-api.yaml —
 * denne testen plukker den automatisk opp neste gang den kjøres.
 */
class KafkaHendelserDokumentasjonTest {
    /** Relativ sti fra `docs/` til spec-fila, brukt i lenken til schema-definisjonen. */
    private val specSti = "../openapi/src/main/resources/behandling-api.yaml"

    @Test
    fun `dokumenterer alle Kafka-hendelser`() {
        val specFile = File(System.getProperty("user.dir")).resolve("src/main/resources/behandling-api.yaml")
        val spec = Yaml().load<Map<String, Any?>>(specFile.reader())
        val schemaLinjer = specFile.reader().use { Yaml().compose(it) }.schemaLinjenumre()

        @Suppress("UNCHECKED_CAST")
        val webhooks = (spec["webhooks"] as? Map<String, Any?>).orEmpty()

        val doc =
            buildString {
                appendLine("# Kafka-hendelser i dp-behandling")
                appendLine()
                appendLine(
                    "Katalog over hendelser publisert på rapid-topicet, generert fra `webhooks`-seksjonen i " +
                        "`openapi/src/main/resources/behandling-api.yaml`. Ikke rediger denne fila manuelt — " +
                        "den overskrives av `KafkaHendelserDokumentasjonTest`. Selve schemaet (felter/typer) " +
                        "dekkes av OpenAPI-speccen selv; her lenker vi bare dit.",
                )
                appendLine()
                appendLine(flytskjema)
                appendLine()
                appendLine("## Hendelser")
                appendLine()

                webhooks.toSortedMap().forEach { (navn, webhook) ->
                    @Suppress("UNCHECKED_CAST")
                    val post = (webhook as Map<String, Any?>)["post"] as Map<String, Any?>
                    val summary = (post["summary"] as? String).orEmpty()
                    val description = (post["description"] as? String)?.trim()?.replace("\n", " ").orEmpty()

                    @Suppress("UNCHECKED_CAST")
                    val requestBody = post["requestBody"] as Map<String, Any?>

                    @Suppress("UNCHECKED_CAST")
                    val schemaRef =
                        ((requestBody["content"] as Map<String, Any?>)["application/json"] as Map<String, Any?>)["schema"]
                            as Map<String, Any?>
                    val schemaNavn = schemaRef.ref()

                    appendLine("### `$navn`")
                    appendLine()
                    appendLine(summary)
                    appendLine()
                    if (description.isNotBlank() && description != summary) {
                        appendLine(description)
                        appendLine()
                    }
                    appendLine("Schema: ${schemaLinjer.lenke(schemaNavn)}")
                    appendLine()
                }
            }

        val docsDir = File(System.getProperty("user.dir")).resolve("../docs")
        docsDir.mkdirs()
        docsDir.resolve("kafka-events-catalog.md").writeText(doc)
    }

    /**
     * Statisk (ikke generert fra spec-en) forklaring av når/hvordan hendelser publiseres. Domenet
     * (`Behandling`/`Person`) varsler `PersonObservatør`/`BehandlingObservatør`-implementasjoner om
     * hendelser i behandlingens livssyklus — hver observatør avgjør selv hvilke(t) av disse den vil
     * publisere videre som en (eller flere) Kafka-hendelser, og bygger opp meldingene i minnet fram
     * til `HendelseMediator` ber den ferdigstille (skrive til utboks/rapid) etter at hendelsen er
     * ferdig håndtert.
     */
    private val flytskjema =
        """
        ## Hvordan hendelser publiseres

        ```mermaid
        flowchart TD
            B["Behandling / Person\n(domenemodell)"] -->|kaller| O["PersonObservatør /\nBehandlingObservatør"]
            O -.implementeres av.-> PM[PersonMediator]
            O -.implementeres av.-> SB[SøknadBehandletObserver]
            O -.implementeres av.-> OO["OppdateringObserver"]
            O -.implementeres av.-> FS["FlyttSøskenObserver"]

            PM -->|behandling_opprettet, behandling_endret_tilstand,\nforslag_til_behandlingsresultat, behandlingsresultat,\nbehandling_avbrutt, avklaring_lukket| K[(Kafka / rapid)]
            SB -->|kun for søknad-behandlinger:\nsøknad_behandlet, søknadsbehandling_avbrutt| K
            FS -->|behandling flyttet mellom søsken| K
            OO -->|lagres for SSE-strøm til\nsaksbehandlere, ikke Kafka| DB[(Database)]

            HM["HendelseMediator"] -->|"ferdigstill(utboks) etter hendelsen\ner ferdig håndtert"| PM
            HM -->|ferdigstill| SB
            HM -->|ferdigstill| OO
        ```

        Alle observatørene registreres på `Person` i `HendelseMediator` og samler opp meldinger i
        minnet mens hendelsen behandles. Når hendelsen er ferdig håndtert, kaller `HendelseMediator`
        `ferdigstill(...)` på hver observatør, som da skriver meldingene til utboksen (samme
        transaksjon som resten av behandlingen — se `FlyttSøskenObserver` for et unntak som bruker
        rapid-context direkte).

        En ny Kafka-hendelse legges typisk til ved å implementere ett eller flere metoder på
        `PersonObservatør`/`BehandlingObservatør` i en (ny eller eksisterende) observatør, publisere
        med `toJsonMessage(eventName, dto)` og en typet DTO (se `SøknadBehandletObserver.kt`), og
        dokumentere den under `webhooks:` i `behandling-api.yaml`.
        """.trimIndent()

    /** Lenke til der `schemaNavn` er definert i spec-fila, med linjenummer om vi fant en. */
    private fun Map<String, Int>.lenke(schemaNavn: String): String {
        val linje = this[schemaNavn]
        return if (linje != null) "[`$schemaNavn`]($specSti#L$linje)" else "`$schemaNavn`"
    }

    /**
     * Finner linjenummeret (1-indeksert) til hver schema-definisjon under `components/schemas`,
     * ved å gå gjennom YAML-nodetreet (som har posisjonsinformasjon) i stedet for den vanlige
     * `Map`-representasjonen (som ikke har det).
     */
    private fun Node?.schemaLinjenumre(): Map<String, Int> {
        val rot = this as? MappingNode ?: return emptyMap()
        val components = rot.value.finnMappingVerdi("components") ?: return emptyMap()
        val schemas = components.value.finnMappingVerdi("schemas") ?: return emptyMap()
        return schemas.value.associate { tuple ->
            (tuple.keyNode as ScalarNode).value to (tuple.keyNode.startMark.line + 1)
        }
    }

    private fun List<NodeTuple>.finnMappingVerdi(nøkkel: String): MappingNode? =
        firstOrNull { tuple -> (tuple.keyNode as? ScalarNode)?.value == nøkkel }?.valueNode as? MappingNode

    private fun Map<String, Any?>.ref(): String = (this["\$ref"] as String).substringAfterLast("/")
}

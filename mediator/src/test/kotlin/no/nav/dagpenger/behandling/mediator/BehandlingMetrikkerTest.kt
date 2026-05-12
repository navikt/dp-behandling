package no.nav.dagpenger.behandling.mediator

import io.prometheus.metrics.core.metrics.MetricWithFixedMetadata
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.GaugeSnapshot
import io.prometheus.metrics.model.snapshots.HistogramSnapshot
import io.prometheus.metrics.model.snapshots.SummarySnapshot
import no.nav.dagpenger.behandling.mediator.repository.DbMetrics
import org.junit.jupiter.api.Test
import java.io.File

class BehandlingMetrikkerTest {
    @Test
    fun `dokumenterer alle metrikker`() {
        // Sørg for at alle metrikker er registrert ved å referere til companion objects
        BehandlingMetrikker
        Metrikk
        DbMetrics

        // Kjente prefiks for våre egne metrikker
        val egnePrefikser =
            listOf(
                "dp_behandling_",
                "dp_antall_",
                "hendelse_behandling_",
                "behandling_",
                "transactions_",
                "transaction_duration_",
                "commit_duration_",
                "active_transactions",
            )

        val snapshots = PrometheusRegistry.defaultRegistry.scrape()
        val metrikker =
            snapshots
                .filter { snapshot -> egnePrefikser.any { snapshot.metadata.prometheusName.startsWith(it) } }
                .map { snapshot ->
                    val metadata = snapshot.metadata
                    val type =
                        when (snapshot) {
                            is CounterSnapshot -> "counter"
                            is GaugeSnapshot -> "gauge"
                            is HistogramSnapshot -> "histogram"
                            is SummarySnapshot -> "summary"
                            else -> "unknown"
                        }
                    MetrikkInfo(
                        navn = metadata.prometheusName,
                        type = type,
                        beskrivelse = metadata.help ?: "",
                        labels = snapshot.hentLabelNames(),
                    )
                }.sortedBy { it.navn }

        val doc =
            buildString {
                appendLine("# dp-behandling Metrikker")
                appendLine()
                appendLine("Automatisk generert dokumentasjon over alle Prometheus-metrikker i dp-behandling.")
                appendLine("Generert fra `PrometheusRegistry` — legger du til en ny metrikk, oppdateres denne filen automatisk.")
                appendLine()
                appendLine("## Alle metrikker")
                appendLine()
                appendLine("| Metrikk | Type | Beskrivelse | Labels |")
                appendLine("|---------|------|-------------|--------|")

                metrikker.forEach { m ->
                    val labels = m.labels.ifEmpty { "—" }
                    appendLine("| `${m.navn}` | ${m.type} | ${m.beskrivelse} | $labels |")
                }
            }

        val docsDir = File(System.getProperty("user.dir")).resolve("../docs")
        docsDir.mkdirs()
        docsDir.resolve("metrikker.md").writeText(doc)
    }

    private data class MetrikkInfo(
        val navn: String,
        val type: String,
        val beskrivelse: String,
        val labels: String,
    )
}

/**
 * Henter label-navn fra registrerte metrikker via reflection.
 * Prometheus-klienten eksponerer ikke label-navn direkte i snapshots,
 * men `MetricWithFixedMetadata` har et `labelNames`-felt vi kan lese.
 */
private fun io.prometheus.metrics.model.snapshots.MetricSnapshot.hentLabelNames(): String {
    // Prøv å hente fra registeret via collector
    val registry = PrometheusRegistry.defaultRegistry
    val collectorsField = registry.javaClass.getDeclaredField("collectors")
    collectorsField.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    val collectors = collectorsField.get(registry) as List<Any>
    val collector =
        collectors.filterIsInstance<MetricWithFixedMetadata>().firstOrNull {
            it.prometheusName == this.metadata.prometheusName
        }
    if (collector != null) {
        val labelNamesField = MetricWithFixedMetadata::class.java.getDeclaredField("labelNames")
        labelNamesField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val labelNames = labelNamesField.get(collector) as Array<String>
        if (labelNames.isNotEmpty()) return labelNames.joinToString(", ")
    }

    // Fallback: hent fra multi-collectors (GaugeWithCallback etc.)
    val multiCollectorsField = registry.javaClass.getDeclaredField("multiCollectors")
    multiCollectorsField.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    val multiCollectors = multiCollectorsField.get(registry) as List<Any>
    for (mc in multiCollectors) {
        try {
            val namesMethod = mc.javaClass.getMethod("getPrometheusNames")

            @Suppress("UNCHECKED_CAST")
            val names = namesMethod.invoke(mc) as List<String>
            if (this.metadata.prometheusName in names) {
                // GaugeWithCallback har labelNames-felt
                val lnField = mc.javaClass.getDeclaredField("labelNames")
                lnField.isAccessible = true

                @Suppress("UNCHECKED_CAST")
                val labelNames = lnField.get(mc) as Array<String>
                if (labelNames.isNotEmpty()) return labelNames.joinToString(", ")
            }
        } catch (_: Exception) {
            // Ikke alle multi-collectors har labelNames
        }
    }

    return ""
}

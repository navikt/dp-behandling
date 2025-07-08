package no.nav.dagpenger.behandling.mediator

import io.prometheus.metrics.core.metrics.Histogram
import io.prometheus.metrics.core.metrics.Summary
import io.prometheus.metrics.model.registry.PrometheusRegistry

internal object Metrikk {
    private val antallBehandlinger =
        Histogram
            .builder()
            .name("dp_antall_behandlinger")
            .help("Antall behandlinger per person")
            .register(PrometheusRegistry.defaultRegistry)

    fun registrerAntallBehandlinger(antall: Int) {
        antallBehandlinger.observe(antall.toDouble())
    }

    val hentPersonTimer =
        Summary
            .builder()
            .name("dp_hent_person_tid")
            .help("Tid brukt på å hente person med behandlinger")
            .quantile(0.5, 0.01)
            .quantile(0.95, 0.001)
            .quantile(0.99, 0.001)
            .register(PrometheusRegistry.defaultRegistry)

    val tidBruktPerHendelse: Histogram =
        Histogram
            .builder()
            .name("hendelse_behandling_tid_sekunder")
            .help("Tid det tar å behandle en hendelse, i sekunder")
            .labelNames("hendelse")
            .register()

    val lagrePersonMetrikk: Histogram =
        Histogram
            .builder()
            .name("behandling_lagre_person_tid_sekunder")
            .help("Tid det tar å lagre en person, i sekunder")
            .register()

    val totalTidBruktPerHendelse: Histogram =
        Histogram
            .builder()
            .name("hendelse_behandling_total_tid_sekunder")
            .help("Total tid det tar å behandle en hendelse, i sekunder")
            .labelNames("hendelse")
            .register()
}

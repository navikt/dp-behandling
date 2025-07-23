package no.nav.dagpenger.behandling.mediator

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.core.metrics.Histogram
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

    val hentPersonTimer: Histogram =
        Histogram
            .builder()
            .name("behandling_hent_person_tid")
            .help("Tid brukt på å hente person med behandlinger")
            .register(PrometheusRegistry.defaultRegistry)

    val hentBehandlingTimer: Histogram =
        Histogram
            .builder()
            .name("dp_behandling_hent_behandling_tid")
            .help("Tid brukt på å hente en behandling")
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

    val avklaringOpprettetTeller: Counter =
        Counter
            .builder()
            .name("dp_behandling_avklaring_opprettet")
            .help("Avklaringer opprettet i behandling")
            .labelNames("kode")
            .register()

    val tidBruktPerEndring: Histogram =
        Histogram
            .builder()
            .name("dp_behandling_tid_brukt_per_endring")
            .help("Tid det tar å utføre en endring i behandlingen, i sekunder")
            .labelNames("opplysningstype")
            .register()

    val scraped1: Counter =
        Counter
            .builder()
            .name("dp_behandling_scraped1")
            .help("Antall ganger behandlingsdata er hentet fra Arena")
            .register(PrometheusRegistry.defaultRegistry)
    val scraped2: Gauge =
        Gauge
            .builder()
            .name("dp_behandling_scraped2_gauge")
            .help("Antall ganger behandlingsdata er hentet fra Arena")
            .register(PrometheusRegistry.defaultRegistry)
    val scraped3: Histogram =
        Histogram
            .builder()
            .name("dp_behandling_scraped2_histogram")
            .help("Antall ganger behandlingsdata er hentet fra Arena")
            .register(PrometheusRegistry.defaultRegistry)
}

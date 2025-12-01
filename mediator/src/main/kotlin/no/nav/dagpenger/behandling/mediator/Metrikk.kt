package no.nav.dagpenger.behandling.mediator

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Histogram

internal object Metrikk {
    private val antallBehandlinger =
        Histogram
            .builder()
            .name("dp_antall_behandlinger")
            .help("Antall behandlinger per person")
            .register()

    fun registrerAntallBehandlinger(antall: Int) {
        antallBehandlinger.observe(antall.toDouble())
    }

    val hentPersonTimer: Histogram =
        Histogram
            .builder()
            .name("behandling_hent_person_tid")
            .help("Tid brukt på å hente person med behandlinger")
            .register()

    val hentBehandlingTimer: Histogram =
        Histogram
            .builder()
            .name("dp_behandling_hent_behandling_tid")
            .help("Tid brukt på å hente en behandling")
            .register()

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

    val tidBruktPerSletting: Histogram =
        Histogram
            .builder()
            .name("dp_behandling_tid_brukt_per_sletting")
            .help("Tid det tar å utføre en sletting i behandlingen, i sekunder")
            .labelNames("opplysningstype")
            .register()

    val aktivitetsloggTimer: Histogram =
        Histogram
            .builder()
            .name("behandling_publiser_aktivitetslogg_sekunder")
            .help("Tid brukt på å hente person med behandlinger")
            .register()
}

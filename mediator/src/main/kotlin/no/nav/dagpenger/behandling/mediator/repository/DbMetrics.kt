package no.nav.dagpenger.behandling.mediator.repository

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge
import io.prometheus.metrics.core.metrics.Histogram

internal object DbMetrics {
    val commitCounter: Counter =
        Counter
            .builder()
            .name("transactions_committed_total")
            .help("Total number of committed transactions")
            .register()

    val rollbackCounter: Counter =
        Counter
            .builder()
            .name("transactions_rolledback_total")
            .help("Total number of rolled-back transactions")
            .register()

    // Histogram for full transaction duration (query build + DB ops + commit)
    val transactionDuration: Histogram =
        Histogram
            .builder()
            .name("transaction_duration_seconds")
            .help("Full transaction duration including queries and commit")
            .register()

    // Histogram for pure DB commit time
    val commitDuration: Histogram =
        Histogram
            .builder()
            .name("commit_duration_seconds")
            .help("Time spent in actual DB commit")
            .register()

    // Gauge for currently open transactions
    val activeTransactions: Gauge =
        Gauge
            .builder()
            .name("active_transactions")
            .help("Number of active transactions currently open")
            .register()
}

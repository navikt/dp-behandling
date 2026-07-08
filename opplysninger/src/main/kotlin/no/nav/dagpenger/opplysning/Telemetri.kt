package no.nav.dagpenger.opplysning

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer

fun <T> Tracer.medSpan(
    navn: String,
    attributter: Map<String, String> = emptyMap(),
    block: (Span) -> T,
): T {
    val span =
        spanBuilder(navn)
            .apply { attributter.forEach { (k, v) -> setAttribute(k, v) } }
            .startSpan()
    return try {
        span.makeCurrent().use { block(span) }
    } finally {
        span.end()
    }
}

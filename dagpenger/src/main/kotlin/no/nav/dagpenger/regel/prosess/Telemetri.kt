package no.nav.dagpenger.regel.prosess

import io.opentelemetry.api.GlobalOpenTelemetry

internal val telemetri = GlobalOpenTelemetry.getTracer("no.nav.dagpenger.regel.prosess")

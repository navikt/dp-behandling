package no.nav.dagpenger.mediator

import no.nav.dagpenger.konfigurasjon.Configuration

fun main() {
    ApplicationBuilder(Configuration.config).start()
}

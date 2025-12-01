package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggEventMapper
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse
import no.nav.dagpenger.behandling.mediator.Metrikk.aktivitetsloggTimer

internal class AktivitetsloggMediator {
    private val aktivitetsloggEventMapper = AktivitetsloggEventMapper()

    @WithSpan
    fun håndter(
        context: MessageContext,
        hendelse: AktivitetsloggHendelse,
    ) {
        Span.current().apply {
            addEvent("Publiserer aktivitetslogg")
            setAttribute("hendelseType", hendelse::class.simpleName ?: "Ukjent")
            setAttribute("antallAktiviteter", hendelse.aktivitetsteller().toLong())
        }
        aktivitetsloggTimer.time {
            aktivitetsloggEventMapper.håndter(hendelse) { aktivitetLoggMelding ->
                context.publish(
                    JsonMessage
                        .newMessage(
                            aktivitetLoggMelding.eventNavn,
                            aktivitetLoggMelding.innhold,
                        ).toJson(),
                )
            }
        }
    }
}

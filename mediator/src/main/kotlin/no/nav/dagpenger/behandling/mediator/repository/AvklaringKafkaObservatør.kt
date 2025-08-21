package no.nav.dagpenger.behandling.mediator.repository

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.behandling.mediator.Metrikk.avklaringOpprettetTeller

class AvklaringKafkaObservatør(
    private val rapid: MessageContext,
) : AvklaringRepositoryObserver {
    override fun nyAvklaring(nyAvklaringHendelse: AvklaringRepositoryObserver.NyAvklaringHendelse) {
        withLoggingContext(
            "avklaringId" to nyAvklaringHendelse.avklaring.id.toString(),
            "kode" to nyAvklaringHendelse.avklaring.kode.kode,
        ) {
            avklaringOpprettetTeller
                .labelValues(nyAvklaringHendelse.avklaring.kode.kode)
                .inc()

            rapid.publish(
                nyAvklaringHendelse.ident,
                JsonMessage
                    .newMessage(
                        "NyAvklaring",
                        mapOf<String, Any>(
                            "ident" to nyAvklaringHendelse.ident,
                            "avklaringId" to nyAvklaringHendelse.avklaring.id,
                            "kode" to nyAvklaringHendelse.avklaring.kode.kode,
                        ) + nyAvklaringHendelse.kontekst.kontekstMap,
                    ).toJson(),
            )

            logger.info {
                "Publisert NyAvklaring med kode ${nyAvklaringHendelse.avklaring.kode.kode}"
            }
        }
    }

    override fun endretAvklaring(endretAvklaringHendelse: AvklaringRepositoryObserver.EndretAvklaringHendelse) {}

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

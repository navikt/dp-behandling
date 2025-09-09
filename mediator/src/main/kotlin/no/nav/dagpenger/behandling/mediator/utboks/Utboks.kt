package no.nav.dagpenger.behandling.mediator.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage

class Utboks(
    private val rapid: MessageContext,
    private val utboksLager: UtboksLager,
) : MessageContext {
    override fun publish(message: String) = rapid.publish(message).also { utboksLager.lagre(message) }

    override fun publish(
        key: String,
        message: String,
    ) = rapid.publish(key, message).also { utboksLager.lagre(message) }

    override fun publish(messages: List<OutgoingMessage>) = throw UnsupportedOperationException("Batch publish er ikke støttet")

    override fun rapidName() = rapid.rapidName()
}

interface UtboksLager {
    fun lagre(melding: String)
}

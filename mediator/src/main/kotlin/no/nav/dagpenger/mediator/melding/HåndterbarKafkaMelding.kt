package no.nav.dagpenger.mediator.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.regelverk.melding.KafkaMelding

internal abstract class HåndterbarKafkaMelding(
    packet: JsonMessage,
) : KafkaMelding(packet) {
    internal abstract fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    )
}

package no.nav.dagpenger.regelverk

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.regelverk.melding.KafkaMelding

interface HendelseMottaker {
    fun behandle(
        hendelse: StartHendelse,
        message: KafkaMelding,
        context: MessageContext,
    )
}

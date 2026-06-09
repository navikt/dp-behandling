package no.nav.dagpenger.utestengning

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.RegelverkRegistrering

class UtestengningRegistrering : RegelverkRegistrering(RegelverkUtestengning) {
    override fun registrerMottak(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
    ) {
        // Ingen Kafka-mottak ennå; behandlinger opprettes manuelt via API
    }

    override fun registrerProsesser(prosessregister: Prosessregister) {
        prosessregister.registrer(Utestengningsprosess())
    }
}

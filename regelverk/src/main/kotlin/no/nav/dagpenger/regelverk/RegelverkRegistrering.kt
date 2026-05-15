package no.nav.dagpenger.regelverk

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection

interface RegelverkRegistrering {
    fun mottak(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
    )
}

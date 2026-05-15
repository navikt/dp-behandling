package no.nav.dagpenger.ferietillegg

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.ferietillegg.mottak.BeregnFerietilleggMottak
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.RegelverkRegistrering

class FerietilleggRegistrering : RegelverkRegistrering {
    override fun mottak(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
    ) {
        BeregnFerietilleggMottak(rapidsConnection, hendelseMottaker)
    }
}

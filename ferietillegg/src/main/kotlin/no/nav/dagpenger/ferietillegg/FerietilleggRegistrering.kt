package no.nav.dagpenger.ferietillegg

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.ferietillegg.mottak.BeregnFerietilleggMottak
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.RegelverkRegistrering

class FerietilleggRegistrering : RegelverkRegistrering(RegelverkFerietillegg) {
    override fun registrerMottak(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
    ) {
        BeregnFerietilleggMottak(rapidsConnection, hendelseMottaker)
    }

    override fun registrerProsesser(prosessregister: Prosessregister) {
        prosessregister.registrer(Ferietilleggprosess())
    }
}

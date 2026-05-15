package no.nav.dagpenger.ferietillegg

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.ferietillegg.mottak.BeregnFerietilleggMottak
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.RegelverkRegistrering

class FerietilleggRegistrering : RegelverkRegistrering {
    override val opplysningstyper: Set<Opplysningstype<*>> =
        RegelverkFerietillegg.produserer

    override fun registrer(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
        prosessregister: Prosessregister,
    ) {
        BeregnFerietilleggMottak(rapidsConnection, hendelseMottaker)
        registrerProsesser(prosessregister)
    }

    override fun registrerProsesser(prosessregister: Prosessregister) {
        prosessregister.registrer(Ferietilleggprosess())
    }
}

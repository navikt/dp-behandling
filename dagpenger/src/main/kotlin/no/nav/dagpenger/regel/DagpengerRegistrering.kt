package no.nav.dagpenger.regel

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.regel.mottak.AvsluttetArbeidssøkerperiodeMottak
import no.nav.dagpenger.regel.mottak.OpprettBehandlingMottak
import no.nav.dagpenger.regel.mottak.SamordningHendelseMottak
import no.nav.dagpenger.regel.mottak.SøknadInnsendtMottak
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.RegelverkRegistrering

class DagpengerRegistrering : RegelverkRegistrering {
    override fun mottak(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
    ) {
        AvsluttetArbeidssøkerperiodeMottak(rapidsConnection, hendelseMottaker)
        OpprettBehandlingMottak(rapidsConnection, hendelseMottaker)
        SamordningHendelseMottak(rapidsConnection, hendelseMottaker)
        SøknadInnsendtMottak(rapidsConnection, hendelseMottaker)
    }
}

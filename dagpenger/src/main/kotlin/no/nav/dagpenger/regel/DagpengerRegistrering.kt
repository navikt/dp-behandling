package no.nav.dagpenger.regel

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import no.nav.dagpenger.regel.mottak.AvsluttetArbeidssøkerperiodeMottak
import no.nav.dagpenger.regel.mottak.OpprettBehandlingMottak
import no.nav.dagpenger.regel.mottak.SamordningHendelseMottak
import no.nav.dagpenger.regel.mottak.SøknadInnsendtMottak
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.RegelverkRegistrering

class DagpengerRegistrering : RegelverkRegistrering {
    override val opplysningstyper: Set<Opplysningstype<*>> =
        RegelverkDagpenger.produserer + fagsakIdOpplysningstype + hendelseTypeOpplysningstype

    override fun registrer(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
        prosessregister: Prosessregister,
    ) {
        AvsluttetArbeidssøkerperiodeMottak(rapidsConnection, hendelseMottaker)
        OpprettBehandlingMottak(rapidsConnection, hendelseMottaker)
        SamordningHendelseMottak(rapidsConnection, hendelseMottaker)
        SøknadInnsendtMottak(rapidsConnection, hendelseMottaker)
        registrerProsesser(prosessregister)
    }

    override fun registrerProsesser(prosessregister: Prosessregister) {
        prosessregister.registrer(Søknadsprosess())
        prosessregister.registrer(Meldekortprosess())
        prosessregister.registrer(Manuellprosess())
        prosessregister.registrer(Omgjøringsprosess())
        prosessregister.registrer(Stansprosess())
    }
}

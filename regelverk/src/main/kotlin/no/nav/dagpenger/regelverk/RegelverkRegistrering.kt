package no.nav.dagpenger.regelverk

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister

interface RegelverkRegistrering {
    val opplysningstyper: Set<Opplysningstype<*>>

    fun registrer(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
        prosessregister: Prosessregister,
    )

    fun registrerProsesser(prosessregister: Prosessregister)
}

package no.nav.dagpenger.regelverk

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Tekst
import java.util.UUID

val hendelseTypeOpplysningstype =
    Opplysningstype.tekst(
        Opplysningstype.Id(UUID.fromString("01958ef2-e237-77c4-89e1-de91256e2e4a"), Tekst),
        "hendelseType",
    )

abstract class RegelverkRegistrering(
    val regelverk: Regelverk,
) {
    open val opplysningstyper: Set<Opplysningstype<*>> = regelverk.produserer + hendelseTypeOpplysningstype

    fun registrer(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
        prosessregister: Prosessregister,
    ) {
        registrerMottak(rapidsConnection, hendelseMottaker)
        registrerProsesser(prosessregister)
    }

    protected abstract fun registrerMottak(
        rapidsConnection: RapidsConnection,
        hendelseMottaker: HendelseMottaker,
    )

    abstract fun registrerProsesser(prosessregister: Prosessregister)
}

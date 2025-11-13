package no.nav.dagpenger.behandling.mediator.api

import no.nav.dagpenger.behandling.api.models.EnhetDTO
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet

fun Opplysningstype<*>.tilEnhetDTO() =
    when (this.enhet) {
        Enhet.G -> EnhetDTO.G
        Enhet.Prosent -> EnhetDTO.PROSENT
        Enhet.Timer -> EnhetDTO.TIMER
        Enhet.Dager -> EnhetDTO.DAGER
        Enhet.Uker -> EnhetDTO.UKER
        Enhet.År -> EnhetDTO.ÅR
        Enhet.Måneder -> EnhetDTO.MÅNEDER
        null -> null
    }

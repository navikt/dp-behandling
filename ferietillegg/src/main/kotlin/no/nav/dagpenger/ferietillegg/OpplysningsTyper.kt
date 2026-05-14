package no.nav.dagpenger.ferietillegg

import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import java.util.UUID

object OpplysningsTyper {
    val årSomSkalBeregnesId = Opplysningstype.Id(UUID.fromString("019d7695-7c15-74b7-a3af-7c82e47c2a20"), Heltall)
    val harKravPåFerietilleggId = Opplysningstype.Id(UUID.fromString("019d7231-21b8-7d1f-9e3a-314195d9f347"), Boolsk)
    val ferietilleggBeløpId = Opplysningstype.Id(UUID.fromString("019d7231-50cb-7940-873b-969d1b9dab7e"), Penger)
    val sumUtbetaltForÅrId = Opplysningstype.Id(UUID.fromString("019d7231-85a1-7ef5-a58e-1909ecdb4127"), Penger)
    val ferietilleggProsentId = Opplysningstype.Id(UUID.fromString("019d7231-aecf-7b19-a027-5c25fb5f45be"), Desimaltall)
    val antallDagerForbrukId = Opplysningstype.Id(UUID.fromString("019d7231-ddc0-7059-a631-7d4d06b974de"), Heltall)
    val ferietilleggTerskelId = Opplysningstype.Id(UUID.fromString("019d7232-0fe1-79fa-8a87-b6937fe67079"), Heltall)
}

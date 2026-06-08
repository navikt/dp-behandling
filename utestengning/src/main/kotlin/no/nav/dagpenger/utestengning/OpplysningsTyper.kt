package no.nav.dagpenger.utestengning

import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Opplysningstype
import java.util.UUID

object OpplysningsTyper {
    val erUtestengtId = Opplysningstype.Id(UUID.fromString("6cc5b2dc-8f69-4eb3-955b-a549c8eb3daa"), Boolsk)
    val utestengningFraOgMedId = Opplysningstype.Id(UUID.fromString("25f23583-0be4-4c04-a7fa-ae8158b71a7d"), Dato)
    val utestengningTilOgMedId = Opplysningstype.Id(UUID.fromString("5111da1a-4661-4783-949c-6b3a59eb1333"), Dato)
}

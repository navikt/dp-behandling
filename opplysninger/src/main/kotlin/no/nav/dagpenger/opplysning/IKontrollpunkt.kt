package no.nav.dagpenger.opplysning

import java.util.UUID

interface IKontrollpunkt {
    fun evaluer(opplysninger: LesbarOpplysninger): Kontrollresultat

    sealed class Kontrollresultat {
        data object OK : Kontrollresultat()

        data class KreverAvklaring(
            val avklaringkode: Avklaringkode,
            val grunnlag: Set<UUID>,
        ) : Kontrollresultat()
    }
}

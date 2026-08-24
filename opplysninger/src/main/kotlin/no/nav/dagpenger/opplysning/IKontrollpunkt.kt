package no.nav.dagpenger.opplysning

import java.time.LocalDateTime

interface IKontrollpunkt {
    fun evaluer(opplysninger: LesbarOpplysninger): Kontrollresultat

    /**
     * Evaluer med tilgang til både de aktive opplysningene for en gitt prøvingsdato og alle
     * opplysninger (inkludert arvede fra tidligere behandlinger). Standard er å se bort fra
     * alleOpplysninger — overstyres av kontrollpunkter som trenger å se hele bildet.
     */
    fun evaluer(
        aktiveOpplysninger: LesbarOpplysninger,
        alleOpplysninger: LesbarOpplysninger,
    ): Kontrollresultat = evaluer(aktiveOpplysninger)

    sealed class Kontrollresultat {
        data object OK : Kontrollresultat()

        data class KreverAvklaring(
            val avklaringkode: Avklaringkode,
            val sisteOpplysning: LocalDateTime,
        ) : Kontrollresultat()
    }
}

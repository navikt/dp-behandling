package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.Opplysning
import java.time.LocalDateTime
import java.util.UUID

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
            val opplysninger: List<Opplysning<*>>,
        ) : Kontrollresultat() {
            init {
                require(opplysninger.isNotEmpty()) { "Et kontrollpunkt som krever avklaring må ha slått opp minst én opplysning" }
            }

            val sisteOpplysning: LocalDateTime get() = opplysninger.maxOf { it.opprettet }
            val opplysningIder: List<UUID> get() = opplysninger.map { it.id }.distinct()
        }
    }
}

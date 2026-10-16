package no.nav.dagpenger.avklaring

import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysningerMedLogg

fun interface Kontroll {
    fun kjør(opplysninger: LesbarOpplysninger): Boolean
}

class Kontrollpunkt(
    val avklaringkode: Avklaringkode,
    private val kontroll: Kontroll,
) : IKontrollpunkt {
    override fun evaluer(opplysninger: LesbarOpplysninger): IKontrollpunkt.Kontrollresultat {
        val opplysningerMedLogg = LesbarOpplysningerMedLogg(opplysninger)
        return when {
            kontroll.kjør(opplysningerMedLogg) ->
                IKontrollpunkt.Kontrollresultat.KreverAvklaring(
                    avklaringkode,
                    opplysningerMedLogg.brukteOpplysninger,
                )

            else -> IKontrollpunkt.Kontrollresultat.OK
        }
    }
}

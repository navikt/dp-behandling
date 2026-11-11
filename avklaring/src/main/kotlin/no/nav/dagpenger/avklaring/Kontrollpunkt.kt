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

/**
 * Kontrollpunkt som vurderer alle opplysninger på tvers av datoer, ikke bare dagen som evalueres.
 * Brukes når avklaringen skal bestå så lenge minst én dag trenger den.
 */
class KontrollpunktForAlleDatoer(
    private val avklaringkode: Avklaringkode,
    private val kontroll: Kontroll,
) : IKontrollpunkt {
    override fun evaluer(opplysninger: LesbarOpplysninger) = evaluer(opplysninger, opplysninger)

    override fun evaluer(
        alleOpplysninger: LesbarOpplysninger,
        aktiveOpplysninger: LesbarOpplysninger,
    ): IKontrollpunkt.Kontrollresultat = Kontrollpunkt(avklaringkode, kontroll).evaluer(alleOpplysninger)
}

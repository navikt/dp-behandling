package no.nav.dagpenger.behandling.konklusjon

import no.nav.dagpenger.opplysning.LesbarOpplysninger

interface Konklusjon {
    val årsak: String
}

class KonklusjonsStrategi(
    private val årsak: Konklusjon,
    private val konklusjonsSjekk: KonklusjonsSjekk,
) {
    fun evaluer(opplysninger: LesbarOpplysninger) =
        when (konklusjonsSjekk.kanKonkludere(opplysninger)) {
            KonklusjonsSjekk.Resultat.Konkludert -> årsak
            KonklusjonsSjekk.Resultat.IkkeKonkludert -> null
        }
}

fun interface KonklusjonsSjekk {
    fun kanKonkludere(opplysninger: LesbarOpplysninger): Resultat

    enum class Resultat {
        Konkludert,
        IkkeKonkludert,
    }
}

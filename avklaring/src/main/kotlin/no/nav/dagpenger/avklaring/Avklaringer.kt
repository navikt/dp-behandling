package no.nav.dagpenger.avklaring

import no.nav.dagpenger.avklaring.Kontrollpunkt.Kontrollresultat.KreverAvklaring
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import java.util.UUID

class Avklaringer(
    private val kontrollpunkter: List<Kontrollpunkt>,
    avklaringer: List<Avklaring> = emptyList(),
) {
    internal val avklaringer = avklaringer.toMutableSet()

    fun avklaringer(opplysninger: LesbarOpplysninger) = vurderAvklaringer(opplysninger)

    fun måAvklares(opplysninger: LesbarOpplysninger) = vurderAvklaringer(opplysninger).filter { it.måAvklares() }

    private fun vurderAvklaringer(opplysninger: LesbarOpplysninger): List<Avklaring> {
        val aktiveAvklaringer: List<KreverAvklaring> =
            kontrollpunkter
                .map { it.evaluer(opplysninger) }
                .filterIsInstance<KreverAvklaring>()

        // Avbryt alle avklaringer som ikke lenger er aktive
        avklaringer.filter { it.måAvklares() && !aktiveAvklaringer.any { aktiv -> aktiv.avklaringkode == it.kode } }.forEach { it.avbryt() }

        // Gjenåpne avklaringer som er aktive igjen, men har blitt avbrutt tidligere
        // Avklaringer som er kvittert skal ikke gjenåpnes
        aktiveAvklaringer
            .mapNotNull { aktiv ->
                avklaringer.find { eksisterendeAvklaring ->
                    eksisterendeAvklaring.kode == aktiv.avklaringkode &&
                        eksisterendeAvklaring.erAvbrutt() &&
                        eksisterendeAvklaring.sistEndret.isBefore(aktiv.sisteOpplysning)
                }
            }.forEach { it.gjenåpne() }

        // Legg til nye avklaringer
        // TODO: Vi bør nok kun lage nye avklaringer for de som ikke allerede er i listen (her løser Set det for oss)
        avklaringer.addAll(aktiveAvklaringer.map { Avklaring(it.avklaringkode) })

        return avklaringer.toList()
    }

    fun avbryt(avklaringId: UUID): Boolean = avklaringer.find { it.id == avklaringId }?.avbryt() ?: false
}

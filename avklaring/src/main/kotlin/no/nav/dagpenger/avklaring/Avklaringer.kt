package no.nav.dagpenger.avklaring

import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.IKontrollpunkt.Kontrollresultat.KreverAvklaring
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.RegelkjøringObserver
import no.nav.dagpenger.opplysning.Regelkjøringsrapport
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import java.util.UUID

class Avklaringer(
    private val kontrollpunkter: List<IKontrollpunkt>,
    avklaringer: List<Avklaring> = emptyList(),
) : RegelkjøringObserver {
    internal val avklaringer = avklaringer.toMutableSet()

    fun avklaringer() = avklaringer

    fun måAvklares() = avklaringer.filter { it.måAvklares() }

    override fun evaluert(
        rapport: Regelkjøringsrapport,
        opplysninger: Opplysninger,
    ) {
        vurderAvklaringer(opplysninger)
    }

    fun gjenåpne(avklaringId: UUID): Boolean = avklaringer.find { it.id == avklaringId }?.gjenåpne() ?: false

    fun avklar(
        avklaringId: UUID,
        kilde: Kilde,
    ): Boolean = avklaringer.find { it.id == avklaringId }?.avklar(kilde) ?: false

    fun kvitter(
        avklaringId: UUID,
        kilde: Saksbehandlerkilde,
        begrunnelse: String,
    ): Boolean = avklaringer.find { it.id == avklaringId }?.kvitter(kilde, begrunnelse) ?: false

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
}

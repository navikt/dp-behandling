package no.nav.dagpenger.avklaring

import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.IKontrollpunkt.Kontrollresultat.KreverAvklaring
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.LesbarOpplysninger
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

    fun leggTil(avklaring: Avklaring) = avklaringer.add(avklaring)

    override fun evaluert(
        rapport: Regelkjøringsrapport,
        alleOpplysninger: LesbarOpplysninger,
        aktiveOpplysninger: LesbarOpplysninger,
    ) {
        vurderAvklaringer(alleOpplysninger, aktiveOpplysninger)
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

    private fun vurderAvklaringer(
        alleOpplysninger: LesbarOpplysninger,
        aktiveOpplysninger: LesbarOpplysninger,
    ): List<Avklaring> {
        val aktiveAvklaringer: List<KreverAvklaring> =
            kontrollpunkter
                .map { it.evaluer(aktiveOpplysninger) }
                .filterIsInstance<KreverAvklaring>()

        // Avbryt alle avklaringer som ikke lenger er aktive
        avklaringer
            .filter { it.måAvklares() && !aktiveAvklaringer.any { aktiv -> aktiv.avklaringkode == it.kode } }
            .filter { it.kode.kanAvbrytes }
            .forEach { it.avbryt() }

        // Gjenåpne avklaringer som er aktive igjen, men har blitt avbrutt tidligere,
        // og der minst én av de opplysningene som trigget siste åpning er erstattet.
        // Avklaringer som er kvittert skal ikke gjenåpnes.
        aktiveAvklaringer.forEach { aktiv ->
            avklaringer
                .find { eksisterende ->
                    eksisterende.kode == aktiv.avklaringkode &&
                        eksisterende.erAvbrutt() &&
                        eksisterende.erGrunnlagetEndret(alleOpplysninger)
                }?.gjenåpne(aktiv.grunnlag)
        }

        // Legg til nye avklaringer
        avklaringer.addAll(aktiveAvklaringer.map { Avklaring(it.avklaringkode, it.grunnlag) })

        return avklaringer.toList()
    }
}

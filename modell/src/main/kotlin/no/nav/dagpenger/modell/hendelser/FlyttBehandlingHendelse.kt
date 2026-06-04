package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.avklaring.Avklaringer
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDateTime
import java.util.UUID

class FlyttBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    val nyBasertPåId: UUID? = null,
    val opplysninger: List<Opplysning<*>> = emptyList(),
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse {
    fun leggTilOpplysninger(opplysninger: Opplysninger) {
        this.opplysninger.forEach { opplysninger.leggTil(it) }
    }

    fun leggTilAvklaring(avklaringer: Avklaringer) {
        avklaringer.leggTil(Avklaring(UUIDv7.ny(), behandlingFlyttetAvklaring))
    }

    override fun kontekstMap(): Map<String, String> =
        mapOf(
            "behandlingId" to behandlingId.toString(),
        )

    companion object {
        val behandlingFlyttetAvklaring =
            Avklaringkode(
                kode = "BehandlingFlyttet",
                tittel = "Det har blitt utført nye behandlinger siden behandlingen ble opprettet",
                beskrivelse =
                    "Det har blitt utført endringer siden behandlingen ble påbegynt. Vurder om nye opplysninger påvirker denne behandlingen",
                kanKvitteres = true,
                kanAvbrytes = false,
            )
    }
}

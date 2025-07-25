package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
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

    override fun kontekstMap(): Map<String, String> =
        mapOf(
            "behandlingId" to behandlingId.toString(),
        )
}

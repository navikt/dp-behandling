package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Kilde
import java.time.LocalDateTime
import java.util.UUID

class EndreOpplysningHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    val opplysningId: UUID,
    val verdi: Any?,
    val gyldighetsperiode: Gyldighetsperiode?,
    val kilde: Kilde,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse {
    init {
        require(verdi != null || gyldighetsperiode != null) { "Må endre verdi eller gyldighetsperiode" }
    }

    override fun kontekstMap(): Map<String, String> =
        mapOf(
            "behandlingId" to behandlingId.toString(),
            "opplysningId" to opplysningId.toString(),
        )
}

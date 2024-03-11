package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import java.time.LocalDate
import java.util.UUID

// Baseklasse for alle hendelser som er knyttet til en person som søker om dagpenger
abstract class SøkerHendelse(
    meldingsreferanseId: UUID,
    val ident: String,
    internal val søknadId: UUID,
    override val gjelderDato: LocalDate,
) : PersonHendelse(meldingsreferanseId, ident), BehandlingHendelse {
    override fun kontekstMap() =
        mapOf(
            "søknadId" to søknadId.toString(),
            "gjelderDato" to gjelderDato.toString(),
        )

}

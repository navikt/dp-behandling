package no.nav.dagpenger.modell

import no.nav.dagpenger.opplysning.Opplysninger
import java.util.UUID

data class BehandlingskjedeOpplysninger(
    val behandlingskjedeId: UUID,
    val opplysninger: Opplysninger,
)

package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Tekst
import java.util.UUID

val hendelseTypeOpplysningstype =
    Opplysningstype.tekst(
        Opplysningstype.Id(UUID.fromString("01958ef2-e237-77c4-89e1-de91256e2e4a"), Tekst),
        "hendelseType",
    )

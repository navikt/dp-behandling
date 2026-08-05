package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Tekst
import java.util.UUID

// Flyttet hit fra no.nav.dagpenger.regelverk.RegelverkRegistrering fordi StartHendelse.opprettBehandling()
// trenger å kunne referere til denne uten å skape en sirkulær modulavhengighet (regelverk avhenger av modell).
val HendelseTypeId = Opplysningstype.Id(UUID.fromString("01958ef2-e237-77c4-89e1-de91256e2e4a"), Tekst)
val hendelseTypeOpplysningstype = Opplysningstype.tekst(HendelseTypeId, "hendelseType")

package no.nav.dagpenger.vedtak.modell.hendelser

import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import java.time.LocalDate
import java.util.UUID

class SøknadInnsendtHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val gjelderDato: LocalDate,
    private val søknadId: UUID,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Hendelse(meldingsreferanseId, ident, aktivitetslogg) {
    override fun kontekst() = mapOf("søknadId" to søknadId.toString())
}

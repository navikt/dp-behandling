package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Opptjeningstid
import no.nav.dagpenger.regel.RettTilDagpenger
import no.nav.dagpenger.regel.Søknadstidspunkt
import java.time.LocalDate
import java.util.UUID

class SøknadInnsendtHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    søknadId: UUID,
    gjelderDato: LocalDate,
) : SøkerHendelse(meldingsreferanseId, ident, søknadId, gjelderDato) {
    override fun regelsett() =
        listOf(
            RettTilDagpenger.regelsett,
            Alderskrav.regelsett,
            Minsteinntekt.regelsett,
            Søknadstidspunkt.regelsett,
            Opptjeningstid.regelsett,
        )

    override fun avklarer() = RettTilDagpenger.kravPåDagpenger
}

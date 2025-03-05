package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BeregnMeldekortHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    meldekortId: UUID,
    opprettet: LocalDateTime,
    meldekort: Meldekort,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = MeldekortId(meldekort.eksternMeldekortId),
        skjedde = meldekort.innsendtTidspunkt.toLocalDate(),
        fagsakId = 0,
        opprettet = opprettet,
    ) {
    override val forretningsprosess = Søknadsprosess()

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        TODO("Not yet implemented")
    }

    override fun behandling(): Behandling {
        TODO("Not yet implemented")
    }

    override fun kontrollpunkter(): List<Kontrollpunkt> {
        TODO("Not yet implemented")
    }

    override fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate {
        TODO("Not yet implemented")
    }

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
        TODO("Not yet implemented")
    }
}

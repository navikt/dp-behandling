package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortBeregningHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortMottattHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse

interface PersonHåndter : BehandlingHåndter {
    fun håndter(hendelse: MeldekortMottattHendelse)
}

interface BehandlingHåndter {
    fun håndter(hendelse: SøknadInnsendtHendelse)

    fun håndter(hendelse: OpplysningSvarHendelse)

    fun håndter(hendelse: AvbrytBehandlingHendelse)

    fun håndter(hendelse: ForslagGodkjentHendelse)

    fun håndter(hendelse: AvklaringIkkeRelevantHendelse)

    fun håndter(hendelse: PåminnelseHendelse)

    fun håndter(hendelse: MeldekortBeregningHendelse)
}

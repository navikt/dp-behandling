package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.behandling.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.LåsHendelse
import no.nav.dagpenger.behandling.modell.hendelser.LåsOppHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.behandling.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse

interface PersonHåndter : BehandlingHåndter

interface BehandlingHåndter {
    fun håndter(hendelse: StartHendelse)

    fun håndter(hendelse: OpplysningSvarHendelse)

    fun håndter(hendelse: AvbrytBehandlingHendelse)

    fun håndter(hendelse: ForslagGodkjentHendelse)

    fun håndter(hendelse: LåsHendelse)

    fun håndter(hendelse: LåsOppHendelse)

    fun håndter(hendelse: AvklaringIkkeRelevantHendelse)

    fun håndter(hendelse: PåminnelseHendelse)

    fun håndter(hendelse: RekjørBehandlingHendelse)

    fun håndter(hendelse: AvklaringKvittertHendelse)

    fun håndter(hendelse: GodkjennBehandlingHendelse)

    fun håndter(hendelse: BesluttBehandlingHendelse)

    fun håndter(hendelse: SendTilbakeHendelse)

    fun håndter(hendelse: MeldekortInnsendtHendelse)

    fun håndter(hendelse: FjernOpplysningHendelse)
}

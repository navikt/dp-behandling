package no.nav.dagpenger.modell

import no.nav.dagpenger.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.LåsHendelse
import no.nav.dagpenger.modell.hendelser.LåsOppHendelse
import no.nav.dagpenger.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelse

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

    fun håndter(hendelse: FlyttBehandlingHendelse)
}

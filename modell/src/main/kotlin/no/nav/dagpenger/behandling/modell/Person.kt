package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.behandling.modell.Behandling.Companion.finn
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse

class Person(
    val ident: Ident,
    behandlinger: List<Behandling>,
) : Aktivitetskontekst,
    PersonHåndter {
    private val observatører = mutableSetOf<PersonObservatør>()
    private val behandlinger = behandlinger.toMutableList()

    constructor(ident: Ident) : this(ident, mutableListOf())

    override fun håndter(hendelse: SøknadInnsendtHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling =
            hendelse.behandling().also { behandling ->
                behandlinger.add(behandling)
                observatører.forEach {
                    behandling.registrer(
                        PersonObservatørAdapter(ident.identifikator(), it),
                    )
                }
            }
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: AvklaringIkkeRelevantHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: OpplysningSvarHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: AvbrytBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: ForslagGodkjentHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: PåminnelseHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    fun behandlinger() = behandlinger.toList()

    fun registrer(observatør: PersonObservatør) {
        observatører.add(observatør)
        behandlinger.forEach { it.registrer(PersonObservatørAdapter(ident.identifikator(), observatør)) }
    }

    private fun PersonHendelse.leggTilKontekst(kontekst: Aktivitetskontekst) {
        kontekst(this)
        kontekst(kontekst)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst = PersonKontekst(ident.identifikator())

    data class PersonKontekst(
        val ident: String,
    ) : SpesifikkKontekst("Person") {
        override val kontekstMap = mapOf("ident" to ident)
    }

    private class PersonObservatørAdapter(
        private val ident: String,
        private val delegate: PersonObservatør,
    ) : PersonObservatør {
        override fun endretTilstand(event: BehandlingEndretTilstand) {
            delegate.endretTilstand(event)
            delegate.endretTilstand(PersonObservatør.PersonEvent(ident, event))
        }
    }
}

interface PersonObservatør : BehandlingObservatør {
    fun endretTilstand(event: PersonEvent<BehandlingEndretTilstand>) {}

    data class PersonEvent<T>(
        val ident: String,
        val wrappedEvent: T,
    )
}

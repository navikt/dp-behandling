package no.nav.dagpenger.behandling.modell

import mu.KotlinLogging
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.behandling.modell.Behandling.Companion.finn
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.behandling.modell.PersonObservatør.PersonEvent
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.behandling.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.behandling.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.LåsHendelse
import no.nav.dagpenger.behandling.modell.hendelser.LåsOppHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.behandling.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.TemporalCollection
import java.time.LocalDate
import java.util.UUID

data class Rettighetstatus(
    val virkningsdato: LocalDate,
    val utfall: Boolean,
    val behandlingId: UUID,
)

class Person(
    val ident: Ident,
    behandlinger: List<Behandling>,
    private val rettighetstatus: TemporalCollection<Rettighetstatus> = TemporalCollection(),
) : Aktivitetskontekst,
    PersonHåndter,
    PersonObservatør {
    private val observatører =
        mutableSetOf<PersonObservatør>()

    fun rettighethistorikk() = rettighetstatus.contents()

    fun harRettighet(dato: LocalDate) = runCatching { rettighetstatus.get(dato).utfall }.getOrElse { false }

    private val behandlinger = behandlinger.toMutableList()

    constructor(ident: Ident) : this(ident, mutableListOf())

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun ferdig(event: BehandlingFerdig) {
        rettighetstatus.put(event.virkningsdato, Rettighetstatus(event.virkningsdato, event.utfall, event.behandlingId))
    }

    override fun håndter(hendelse: StartHendelse) {
        if (behandlinger.any { it.behandler.eksternId == hendelse.eksternId }) {
            hendelse.varsel("Søknad med eksternId ${hendelse.eksternId} er allerede mottatt")
            return
        }

        // Oppskrift for å opprette en behandling
        hendelse.leggTilKontekst(this)
        val behandling =
            hendelse.behandling(enVeldigSmartMåteÅfinneRiktigForrigeBehandling(), rettighetstatus).also { behandling ->
                logger.info {
                    """
                    Oppretter behandling med behandlingId=${behandling.behandlingId} for 
                    hendelse ${hendelse.type} av ${hendelse.eksternId.id}
                    """.trimIndent()
                }
                behandlinger.add(behandling)
                observatører.forEach {
                    behandling.registrer(
                        PersonObservatørAdapter(ident.identifikator(), it),
                    )
                }
            }
        behandling.håndter(hendelse)
    }

    // TODO: Dette er en veldig dum måte å finne forrige behandling på
    // 1. Det finnes ingen tidligere behandling = ingen kjede
    // 2. Det finnes tidligere behandling, men ikke ferdig = ingen kjede
    // 3. Det finnes tidligere behandling, men ikke rett på dagpenger = ingen kjede
    // 4. Det finnes tidligere behandling, med rett på dagpenger = kjede
    private fun enVeldigSmartMåteÅfinneRiktigForrigeBehandling() =
        behandlinger.lastOrNull {
            it.vedtakopplysninger.utfall
            it.harTilstand(Behandling.TilstandType.Ferdig)
        }

    override fun håndter(hendelse: AvklaringIkkeRelevantHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: AvklaringKvittertHendelse) {
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

    override fun håndter(hendelse: LåsHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: LåsOppHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: PåminnelseHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: RekjørBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: MeldekortInnsendtHendelse) {
        hendelse.leggTilKontekst(this)
        logger.info { "Vet ikke hvordan vi skal behandle meldekort ${hendelse.meldekort.eksternMeldekortId}" }
    }

    override fun håndter(hendelse: GodkjennBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: BesluttBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: SendTilbakeHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: FjernOpplysningHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: FlyttBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    fun behandlinger() = behandlinger.toList()

    fun registrer(observatør: PersonObservatør) {
        observatører.add(observatør)
        behandlinger.forEach {
            it.registrer(PersonObservatørAdapter(ident.identifikator(), observatør))
            it.registrer(this)
        }
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
        override fun opprettet(event: BehandlingObservatør.BehandlingOpprettet) {
            event.medIdent { delegate.opprettet(it) }
        }

        override fun forslagTilVedtak(event: BehandlingObservatør.BehandlingForslagTilVedtak) {
            event.medIdent { delegate.forslagTilVedtak(it) }
        }

        override fun ferdig(event: BehandlingFerdig) {
            event.medIdent { delegate.ferdig(it) }
        }

        override fun endretTilstand(event: BehandlingEndretTilstand) {
            event.medIdent { delegate.endretTilstand(it) }
        }

        override fun avbrutt(event: BehandlingObservatør.BehandlingAvbrutt) {
            event.medIdent { delegate.avbrutt(it) }
        }

        override fun avklaringLukket(event: BehandlingObservatør.AvklaringLukket) {
            event.medIdent { delegate.avklaringLukket(it) }
        }

        private fun <T : PersonEvent> T.medIdent(block: (T) -> Unit) = block(this.also { it.ident = this@PersonObservatørAdapter.ident })
    }
}

interface PersonObservatør : BehandlingObservatør {
    sealed class PersonEvent(
        var ident: String? = null,
    )
}

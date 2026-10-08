package no.nav.dagpenger.modell

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.modell.Behandling.Companion.finn
import no.nav.dagpenger.modell.Behandling.TilstandType.Avbrutt
import no.nav.dagpenger.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingEndretTilstand
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.modell.PersonObservatør.PersonEvent
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
import no.nav.dagpenger.modell.hendelser.PersonHendelse
import no.nav.dagpenger.modell.hendelser.PåminnelseHendelse
import no.nav.dagpenger.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat

class Person(
    val ident: Ident,
    behandlinger: List<Behandlingkjede>,
    private val rettighetstidslinje: Rettighetstidslinje = Rettighetstidslinje(),
) : Aktivitetskontekst,
    PersonHåndter,
    PersonObservatør {
    private val observatører =
        mutableSetOf<PersonObservatør>()

    fun rettighetstidslinje(): Rettighetstidslinje = rettighetstidslinje

    private val behandlingkjeder = behandlinger.toMutableList()

    constructor(ident: Ident) : this(ident, mutableListOf())

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun ferdig(event: BehandlingFerdig) {
        rettighetstidslinje.oppdater(event.regelverk, event.behandlingId, event.behandlingskjedeId, event.rettighetsperioder)
    }

    override fun håndter(hendelse: StartHendelse) {
        if (behandlingkjeder.flatten().filterNot { it.harTilstand(Avbrutt) }.any { it.behandler.eksternId == hendelse.eksternId }) {
            hendelse.varsel("${hendelse.type} med eksternId ${hendelse.eksternId} er allerede mottatt")
            return
        }

        // 1. Det finnes ingen tidligere behandling = ingen kjede
        // 2. Det finnes tidligere behandling, men ikke ferdig = ingen kjede
        // 3. Det finnes tidligere behandling, men ikke rett på dagpenger = ingen kjede
        // 4. Det finnes tidligere behandling, med rett på dagpenger = kjede
        //      (velger den kjeden som har nyest behandling)
        val kjede =
            behandlingkjeder
                .filter { it.tilhørerRegelverk(hendelse.forretningsprosess.regelverk) }
                .filter { it.nesteSomKanBaseresPå != null }
                .maxByOrNull { it.nesteSomKanBaseresPå!!.behandlingId }

        // Oppskrift for å opprette en behandling
        hendelse.leggTilKontekst(this)
        val rettighetsperioder = rettighetstidslinje.forRegelverk(hendelse.forretningsprosess.regelverk.ident)
        val behandling =
            when (val resultat = hendelse.behandling(kjede?.nesteSomKanBaseresPå, rettighetsperioder)) {
                is StartHendelseResultat.IkkeOpprettet -> {
                    val melding =
                        "Kan ikke starte behandling av ${hendelse.type} med id ${hendelse.eksternId.id}: ${resultat.årsak}"
                    hendelse.varsel(melding)
                    logger.info { melding }
                    return
                }

                is StartHendelseResultat.Opprettet -> {
                    resultat.behandling
                }

                is StartHendelseResultat.OppdaterBehandling -> {
                    val eldsteAktiveBehandling =
                        behandlingkjeder
                            .filter { it.tilhørerRegelverk(hendelse.forretningsprosess.regelverk) }
                            .filterNot { it.rot.harTilstand(Ferdig) || it.rot.harTilstand(Avbrutt) }
                            .minByOrNull { it.rot.behandlingId }
                            ?.rot

                    if (eldsteAktiveBehandling == null) {
                        val melding = "Fant ikke en aktiv behandling å legge til oppylysninger i"
                        hendelse.varsel(melding)
                        logger.info { melding }
                        return
                    }

                    eldsteAktiveBehandling
                }
            }

        logger.info {
            """
            Oppretter behandling med behandlingId=${behandling.behandlingId} for 
            hendelse ${hendelse.type} av ${hendelse.eksternId.id}
            basert på behandlingId=${behandling.basertPå?.behandlingId}
            """.trimIndent()
        }
        if (kjede == null || behandling.basertPå == null) {
            behandlingkjeder.add(behandling.somKjede())
        } else {
            val index = behandlingkjeder.indexOf(kjede)
            behandlingkjeder[index] = kjede leggTil behandling
        }
        observatører.forEach {
            behandling.registrer(
                PersonObservatørAdapter(ident.identifikator(), it),
            )
        }

        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: AvklaringIkkeRelevantHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: AvklaringKvittertHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: OpplysningSvarHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: AvbrytBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: ForslagGodkjentHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: LåsHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: LåsOppHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: PåminnelseHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: RekjørBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: MeldekortInnsendtHendelse) {
        hendelse.leggTilKontekst(this)
        logger.info { "Vet ikke hvordan vi skal behandle meldekort ${hendelse.meldekort.eksternMeldekortId}" }
    }

    override fun håndter(hendelse: GodkjennBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        krevLineærBehandlingskjede(behandling)

        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: BesluttBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        krevLineærBehandlingskjede(behandling)

        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: SendTilbakeHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: FjernOpplysningHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    override fun håndter(hendelse: FlyttBehandlingHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlingkjeder.finn(hendelse.behandlingId)
        behandling.håndter(hendelse)
    }

    fun behandlinger() = behandlingkjeder.flatten()

    fun registrer(observatør: PersonObservatør) {
        observatører.add(observatør)
        behandlingkjeder.flatten().forEach {
            it.registrer(PersonObservatørAdapter(ident.identifikator(), observatør))
            it.registrer(this)
        }
    }

    private fun PersonHendelse.leggTilKontekst(kontekst: Aktivitetskontekst) {
        kontekst(this)
        kontekst(kontekst)
    }

    private fun krevLineærBehandlingskjede(behandling: Behandling) {
        val relevanteKjeder = behandlingkjeder.filter { it.tilhørerRegelverk(behandling.regelverk) }
        if (!relevanteKjeder.harParallelleBehandlinger(behandling)) return
        throw IllegalStateException(
            """Vedtaket kan ikke fattes fordi en nyere åpen behandling er blitt opprettet. Avbryt denne eldre behandlingen og gjør 
            |endringene i siste opprettede behandling. Dersom endringene går tilbake i tid bør en revurderingsbehandling opprettes.
            """.trimMargin(),
        )
    }

    private fun List<Behandlingkjede>.harParallelleBehandlinger(behandling: Behandling): Boolean {
        if (behandling.basertPå == null) return false
        return flatten().any {
            it.harTilstand(Ferdig) && it.basertPå?.behandlingId == behandling.basertPå.behandlingId
        }
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

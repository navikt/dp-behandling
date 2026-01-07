package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.avklaring.Avklaringer
import no.nav.dagpenger.behandling.modell.Behandling.BehandlingTilstand.Companion.fraType
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.ForslagTilVedtak
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.TilGodkjenning
import no.nav.dagpenger.behandling.modell.Behandling.VedtakOpplysninger
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.AvklaringLukket
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingAvbrutt
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingOpprettet
import no.nav.dagpenger.behandling.modell.PersonObservatør.PersonEvent
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.EksternId
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
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.uuid.UUIDv7
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Behandling private constructor(
    val behandlingId: UUID,
    val behandler: StartHendelse,
    gjeldendeOpplysninger: Opplysninger,
    val basertPå: Behandling? = null,
    var opprettet: LocalDateTime,
    val godkjent: Arbeidssteg = Arbeidssteg(Arbeidssteg.Oppgave.Godkjent),
    val besluttet: Arbeidssteg = Arbeidssteg(Arbeidssteg.Oppgave.Besluttet),
    private var tilstand: BehandlingTilstand,
    avklaringer: List<Avklaring>,
) : Aktivitetskontekst,
    BehandlingHåndter {
    constructor(
        behandler: StartHendelse,
        opplysninger: List<Opplysning<*>>,
        basertPå: Behandling? = null,
        avklaringer: List<Avklaring> = emptyList(),
    ) : this(
        behandlingId = UUIDv7.ny(),
        behandler = behandler,
        gjeldendeOpplysninger = opplysninger.somOpplysninger(),
        basertPå = basertPå,
        opprettet = LocalDateTime.now(),
        tilstand = UnderOpprettelse(LocalDateTime.now()),
        avklaringer = avklaringer,
    )

    init {
        require(basertPå == null || basertPå.tilstand is Ferdig) {
            "Kan ikke basere en ny behandling på en som ikke er ferdig"
        }
    }

    val behandlingskjedeId: UUID = basertPå?.behandlingskjedeId ?: behandlingId

    private val observatører = mutableListOf<BehandlingObservatør>()
    private val tidligereOpplysninger = basertPå?.opplysninger
    private val forretningsprosess = behandler.forretningsprosess

    val opplysninger: Opplysninger = gjeldendeOpplysninger.baserPå(tidligereOpplysninger)

    private val regelkjøring: Regelkjøring
        get() =
            forretningsprosess.regelkjøring(opplysninger).apply {
                leggTilObservatør(avklaringer)
            }
    private val kontrollpunkter =
        when (tilstand) {
            is Avbrutt -> emptyList()
            is Ferdig -> emptyList()
            else -> forretningsprosess.kontrollpunkter()
        }

    private val avklaringer = Avklaringer(kontrollpunkter, avklaringer)

    fun avklaringer() = avklaringer.avklaringer()

    fun aktiveAvklaringer() = avklaringer.måAvklares()

    fun kreverTotrinnskontroll() = forretningsprosess.kreverTotrinnskontroll(opplysninger)

    companion object {
        fun rehydrer(
            behandlingId: UUID,
            behandler: StartHendelse,
            gjeldendeOpplysninger: Opplysninger,
            basertPå: Behandling? = null,
            opprettet: LocalDateTime,
            tilstand: TilstandType,
            sistEndretTilstand: LocalDateTime,
            avklaringer: List<Avklaring>,
            godkjent: Arbeidssteg = Arbeidssteg(Arbeidssteg.Oppgave.Godkjent),
            besluttet: Arbeidssteg = Arbeidssteg(Arbeidssteg.Oppgave.Besluttet),
        ) = Behandling(
            behandlingId = behandlingId,
            behandler = behandler,
            gjeldendeOpplysninger = gjeldendeOpplysninger,
            basertPå = basertPå,
            opprettet = opprettet,
            godkjent = godkjent,
            besluttet = besluttet,
            tilstand = fraType(tilstand, sistEndretTilstand),
            avklaringer = avklaringer,
        )

        fun List<Behandling>.finn(behandlingId: UUID) =
            try {
                single { it.behandlingId == behandlingId }
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Fant flere behandlinger med samme id, id=$behandlingId", e)
            }

        private const val MAKS_ITERASJONER = 10
    }

    fun tilstand() = Pair(tilstand.type, tilstand.opprettet)

    val sistEndret get() = tilstand.opprettet

    fun harTilstand(tilstand: TilstandType) = this.tilstand.type == tilstand

    fun kanRedigeres() = harTilstand(ForslagTilVedtak) || harTilstand(TilGodkjenning)

    fun opplysninger(): LesbarOpplysninger = opplysninger

    override fun håndter(hendelse: StartHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: AvklaringIkkeRelevantHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: AvklaringKvittertHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: OpplysningSvarHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: AvbrytBehandlingHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: ForslagGodkjentHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: LåsHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: LåsOppHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: PåminnelseHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: RekjørBehandlingHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: GodkjennBehandlingHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: BesluttBehandlingHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: SendTilbakeHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: MeldekortInnsendtHendelse) {
        hendelse.kontekst(this)
        hendelse.info("Mottok meldekort")
    }

    override fun håndter(hendelse: FjernOpplysningHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: FlyttBehandlingHendelse) {
        hendelse.kontekst(this)
        // Behandlingen har blitt flytten fra en kjede til en ny eller annen kjede
        // Ved å gå til redigert tilstand kjøres regler og avklaringer på nytt
        tilstand.håndter(this, hendelse)
    }

    fun registrer(observatør: BehandlingObservatør) {
        observatører.add(observatør)
    }

    override fun toSpesifikkKontekst() = BehandlingKontekst(behandlingId, behandler.kontekstMap())

    override fun equals(other: Any?) = other is Behandling && behandlingId == other.behandlingId

    override fun hashCode() = behandlingId.hashCode()

    data class BehandlingKontekst(
        val behandlingId: UUID,
        val behandlerKontekst: Map<String, String>,
    ) : SpesifikkKontekst("Behandling") {
        override val kontekstMap = mapOf("behandlingId" to behandlingId.toString()) + behandlerKontekst
    }

    enum class TilstandType {
        UnderOpprettelse,
        UnderBehandling,
        ForslagTilVedtak,
        Låst,
        Avbrutt,
        Ferdig,
        Redigert,
        TilGodkjenning,
        TilBeslutning,
    }

    private sealed interface BehandlingTilstand : Aktivitetskontekst {
        val type: TilstandType
        val opprettet: LocalDateTime

        val forventetFerdig: LocalDateTime get() = LocalDateTime.MAX

        companion object {
            fun fraType(
                type: TilstandType,
                opprettet: LocalDateTime,
            ) = when (type) {
                TilstandType.UnderOpprettelse -> UnderOpprettelse(opprettet)
                TilstandType.UnderBehandling -> UnderBehandling(opprettet)
                ForslagTilVedtak -> ForslagTilVedtak(opprettet)
                TilstandType.Låst -> Låst(opprettet)
                TilstandType.Avbrutt -> Avbrutt(opprettet)
                TilstandType.Ferdig -> Ferdig(opprettet)
                TilstandType.Redigert -> Redigert(opprettet)
                TilGodkjenning -> TilGodkjenning(opprettet)
                TilstandType.TilBeslutning -> TilBeslutning(opprettet)
            }
        }

        fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
        }

        fun håndter(
            behandling: Behandling,
            hendelse: StartHendelse,
        ): Unit =
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )

        fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ): Unit =
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )

        fun håndter(
            behandling: Behandling,
            hendelse: AvbrytBehandlingHendelse,
        ) {
            hendelse.info("Avbryter behandlingen")
            behandling.tilstand(Avbrutt(årsak = hendelse.årsak), hendelse)
        }

        fun håndter(
            behandling: Behandling,
            hendelse: ForslagGodkjentHendelse,
        ): Unit =
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )

        fun håndter(
            behandling: Behandling,
            hendelse: LåsHendelse,
        ): Unit =
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )

        fun håndter(
            behandling: Behandling,
            hendelse: LåsOppHendelse,
        ): Unit =
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )

        fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ): Unit =
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )

        fun håndter(
            behandling: Behandling,
            hendelse: AvklaringKvittertHendelse,
        ): Unit =
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )

        fun håndter(
            behandling: Behandling,
            hendelse: PåminnelseHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen mottok påminnelse, men tilstanden støtter ikke dette")
        }

        fun håndter(
            behandling: Behandling,
            hendelse: RekjørBehandlingHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen mottok beskjed om rekjøring, men tilstanden støtter ikke dette")
        }

        fun håndter(
            behandling: Behandling,
            hendelse: GodkjennBehandlingHendelse,
        ): Unit = throw IllegalStateException("Behandlingen skal godkjennes, men tilstanden ${this.type.name} støtter ikke dette")

        fun håndter(
            behandling: Behandling,
            hendelse: BesluttBehandlingHendelse,
        ): Unit = throw IllegalStateException("Behandlingen skal besluttes, men tilstanden ${this.type.name} støtter ikke dette")

        fun håndter(
            behandling: Behandling,
            hendelse: SendTilbakeHendelse,
        ): Unit =
            throw IllegalStateException(
                "Behandlingen skal sendest tilbake fra totrinnskontroll, men tilstanden ${this.type.name} støtter ikke dette",
            )

        fun håndter(
            behandling: Behandling,
            hendelse: FjernOpplysningHendelse,
        ): Unit = throw IllegalStateException("Opplysning skal fjernes, men tilstanden ${this.type.name} støtter ikke dette")

        fun håndter(
            behandling: Behandling,
            hendelse: FlyttBehandlingHendelse,
        ): Unit =
            throw IllegalStateException(
                "Behandlingen skal flyttes til en ny behandlingsskjede, men tilstanden ${this.type.name} støtter ikke dette",
            )

        fun leaving(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
        }

        override fun toSpesifikkKontekst() =
            SpesifikkKontekst(
                type.name,
                mapOf(
                    "opprettet" to opprettet.toString(),
                ),
            )
    }

    private data class UnderOpprettelse(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : BehandlingTilstand {
        override val type = TilstandType.UnderOpprettelse

        override fun håndter(
            behandling: Behandling,
            hendelse: StartHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Mottatt ${hendelse.type} og startet behandling")
            behandling.emitOpprettet()

            behandling.forretningsprosess.kjørStart(Prosesskontekst(behandling.opplysninger))

            behandling.tilstand(UnderBehandling(), hendelse)
        }
    }

    private data class UnderBehandling(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : BehandlingTilstand {
        override val type = TilstandType.UnderBehandling
        override val forventetFerdig: LocalDateTime get() = opprettet.plusHours(1)

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            behandling.kjørRegler(hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: PåminnelseHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Mottok påminnelse om at behandlingen står fast")
            val rapport = behandling.regelkjøring.evaluer()
            if (rapport.erFerdig()) {
                hendelse.logiskFeil("Behandlingen er ferdig men vi er fortsatt i ${this.type.name}")
            }
            hendelse.lagBehov(rapport.informasjonsbehov)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.opplysninger.forEach { opplysning ->
                hendelse.info("Mottok svar på opplysning om ${opplysning.opplysningstype}")
                opplysning.leggTil(behandling.opplysninger)
            }

            // Kjør regelkjøring for alle opplysninger
            behandling.kjørRegler(hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ) {
            hendelse.kontekst(this)
            if (behandling.avklaringer.avklar(hendelse.avklaringId, hendelse.kilde)) {
                hendelse.info("Avklaring ${hendelse.kode} er ikke lenger relevant")
            }
        }
    }

    private data class ForslagTilVedtak(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : BehandlingTilstand {
        override val type = ForslagTilVedtak

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Alle opplysninger mottatt, lager forslag til vedtak")

            behandling.emitForslagTilVedtak()
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: LåsHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandling sendt til kontroll")

            behandling.tilstand(Låst(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: ForslagGodkjentHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Forslag til vedtak godkjent")

            behandling.avgjørNesteTilstand(hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ) {
            hendelse.kontekst(this)
            if (behandling.avklaringer.avklar(hendelse.avklaringId, hendelse.kilde)) {
                hendelse.info("Avklaring ${hendelse.kode} er ikke lenger relevant")
                behandling.emitAvklaringLukket(hendelse.avklaringId, hendelse.kode)
            }

            behandling.avgjørNesteTilstand(hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringKvittertHendelse,
        ) {
            hendelse.kontekst(this)

            behandling.avklaringer.kvitter(hendelse.avklaringId, hendelse.kilde, hendelse.begrunnelse)
            hendelse.info("Avklaring er kvittert")

            behandling.avgjørNesteTilstand(hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Fikk svar på opplysning i ${this.type.name}.")

            hendelse.opplysninger.forEach { opplysning ->
                hendelse.info("Mottok svar på opplysning om ${opplysning.opplysningstype}")
                opplysning.leggTil(behandling.opplysninger)
            }

            behandling.tilstand(Redigert(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: RekjørBehandlingHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Mottok beskjed om rekjøring av behandling")
            hendelse.oppfriskOpplysningIder.map {
                behandling.opplysninger.fjern(it)
            }
            behandling.tilstand(Redigert(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: FjernOpplysningHendelse,
        ) {
            hendelse.info("Skal fjerne opplysning ${hendelse.opplysningId}")
            behandling.opplysninger.fjern(hendelse.opplysningId)

            behandling.tilstand(Redigert(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: FlyttBehandlingHendelse,
        ) {
            hendelse.info("Flytter behandlingen ${behandling.behandlingId} til ${hendelse.nyBasertPåId ?: "ny kjede"}")

            val egneOpplysninger = behandling.opplysninger.somListe()
            behandling.opplysninger.fjernHvis { opplysning ->
                val utledetAvOpplysninger = opplysning.utledetAv?.opplysninger ?: return@fjernHvis false
                // Fjern opplysning hvis det finnes noen opplysninger i utledetAv er basert på opplysninger fra tidligere behandlinger
                utledetAvOpplysninger.any { it !in egneOpplysninger }
            }

            // Legg til ekstra opplysninger som er nødvendige for å starte ny kjede
            hendelse.leggTilOpplysninger(behandling.opplysninger)

            behandling.tilstand(Redigert(), hendelse)
        }
    }

    private data class Redigert(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : BehandlingTilstand {
        override val type: TilstandType
            get() = TilstandType.Redigert

        override val forventetFerdig: LocalDateTime get() = opprettet.plusHours(1)

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Endret tilstand til redigert")

            // TODO: Denne må vi tenke litt på plassering og rekkefølge
            behandling.forretningsprosess.kjørStart(Prosesskontekst(behandling.opplysninger))

            // Kjør regelkjøring for alle opplysninger
            behandling.kjørRegler(hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.opplysninger.forEach { opplysning ->
                hendelse.info("Mottok svar på opplysning om ${opplysning.opplysningstype}")
                opplysning.leggTil(behandling.opplysninger)
            }

            behandling.kjørRegler(hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: PåminnelseHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Mottak påminnelse")

            val rapport = behandling.regelkjøring.evaluer()

            if (rapport.erFerdig()) {
                behandling.avgjørNesteTilstand(hendelse)
            } else {
                hendelse.lagBehov(rapport.informasjonsbehov)
            }
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ) {
            hendelse.kontekst(this)
            if (behandling.avklaringer.avklar(hendelse.avklaringId, hendelse.kilde)) {
                hendelse.info("Avklaring ${hendelse.kode} er ikke lenger relevant")
                behandling.emitAvklaringLukket(hendelse.avklaringId, hendelse.kode)
            }
        }
    }

    private data class Avbrutt(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
        val årsak: String? = null,
    ) : BehandlingTilstand {
        override val type = TilstandType.Avbrutt

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            hendelse.info("Behandling avbrutt")
            behandling.emitAvbrutt(årsak)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvbrytBehandlingHendelse,
        ) { // No-op
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er avbrutt, ignorerer opplysningssvar")
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er avbrutt, ignorerer avklaringer")
        }
    }

    private data class Låst(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : BehandlingTilstand {
        override val type = TilstandType.Låst

        override fun håndter(
            behandling: Behandling,
            hendelse: LåsOppHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen ble ikke godkjent, settes tilbake til forslag")

            behandling.tilstand(ForslagTilVedtak(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: ForslagGodkjentHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Forslag til vedtak godkjent")

            behandling.avgjørNesteTilstand(hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvbrytBehandlingHendelse,
        ): Unit = throw IllegalStateException("Kan ikke avbryte en låst behandling")

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er låst, ignorerer avklaringer")
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er låst, ignorerer opplysningssvar")
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: LåsHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er allerede låst")
        }
    }

    private data class Ferdig(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : BehandlingTilstand {
        override val type = TilstandType.Ferdig

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            if (!behandling.harRettighetsperioder()) {
                throw IllegalStateException("Kan ikke ferdigstille en behandling uten rettighetsperioder")
            }

            behandling.emitFerdig()
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvbrytBehandlingHendelse,
        ): Unit = throw IllegalStateException("Kan ikke avbryte en ferdig behandling")

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er ferdig, ignorerer avklaringer")
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er ferdig, ignorerer opplysningssvar")
        }
    }

    private class TilGodkjenning(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : BehandlingTilstand {
        override val type = TilGodkjenning

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Har et nytt forslag til vedtak som må godkjennes")

            behandling.emitForslagTilVedtak()
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: GodkjennBehandlingHendelse,
        ) {
            hendelse.kontekst(this)

            if (!behandling.harRettighetsperioder()) {
                throw IllegalStateException("Kan ikke godkjenne en behandling uten rettighetsperioder")
            }

            behandling.godkjent.utførtAv(hendelse.godkjentAv)
            if (!behandling.forretningsprosess.kreverTotrinnskontroll(behandling.opplysninger)) {
                hendelse.info("Ble godkjent, men krever ikke totrinnskontroll")
                behandling.tilstand(Ferdig(), hendelse)
            }

            hendelse.info("Ble godkjent og krever totrinnskontroll")
            // Om behandlingen ikke krever totrinnskontroller vi ferdige
            if (!behandling.forretningsprosess.kreverTotrinnskontroll(behandling.opplysninger)) {
                return behandling.tilstand(Ferdig(), hendelse)
            }

            // Behandlinger som krever totrinnskontroll må sendes til beslutning
            behandling.tilstand(TilBeslutning(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Fikk svar på opplysning i ${this.type.name}.")

            hendelse.opplysninger.forEach { opplysning ->
                hendelse.info("Mottok svar på opplysning om ${opplysning.opplysningstype}")
                opplysning.leggTil(behandling.opplysninger)
            }

            behandling.tilstand(Redigert(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: FjernOpplysningHendelse,
        ) {
            hendelse.info("Skal fjerne opplysning ${hendelse.opplysningId}")
            behandling.opplysninger.fjern(hendelse.opplysningId)

            behandling.tilstand(Redigert(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvbrytBehandlingHendelse,
        ) {
            hendelse.kontekst(this)
            behandling.tilstand(Avbrutt(årsak = hendelse.årsak), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringKvittertHendelse,
        ) {
            hendelse.kontekst(this)

            behandling.avklaringer.kvitter(hendelse.avklaringId, hendelse.kilde, hendelse.begrunnelse)
            hendelse.info("Avklaring er kvittert")
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er låst, ignorerer avklaringer")
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: RekjørBehandlingHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Mottok beskjed om rekjøring av behandling")
            hendelse.oppfriskOpplysningIder.map {
                behandling.opplysninger.fjern(it)
            }
            behandling.tilstand(Redigert(), hendelse)
        }
    }

    private class TilBeslutning(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : BehandlingTilstand {
        override val type = TilstandType.TilBeslutning

        override fun håndter(
            behandling: Behandling,
            hendelse: BesluttBehandlingHendelse,
        ) {
            hendelse.kontekst(this)

            if (!behandling.harRettighetsperioder()) {
                throw IllegalStateException("Kan ikke beslutte en behandling uten rettighetsperioder")
            }

            if (behandling.godkjent.erUtførtAv(hendelse.besluttetAv)) {
                throw IllegalArgumentException("Beslutter kan ikke være samme som saksbehandler")
            }

            behandling.besluttet.utførtAv(hendelse.besluttetAv)
            behandling.tilstand(Ferdig(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: SendTilbakeHendelse,
        ) {
            hendelse.kontekst(this)
            behandling.godkjent.ikkeUtført()
            behandling.tilstand(TilGodkjenning(), hendelse)
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvklaringIkkeRelevantHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er låst, ignorerer avklaringer")
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er låst, ignorerer opplysningssvar")
        }
    }

    // Kjører alle regler, logger hvilke regler som er kjørt , sender ut behov, og avgjør neste tilstand
    private tailrec fun kjørRegler(
        hendelse: PersonHendelse,
        iterasjon: Int = 0,
    ) {
        if (iterasjon > MAKS_ITERASJONER) {
            throw IllegalStateException("Fanget i uendelig løkke: Regler og plugins blir aldri enige.")
        }

        // 1. Evaluerer regler
        val rapport = regelkjøring.evaluer()

        // Logger hva som skjedde (kan evt flyttes til egne funksjoner for ryddighet)
        rapport.kjørteRegler.forEach { hendelse.info(it.toString()) }
        hendelse.lagBehov(rapport.informasjonsbehov)

        // 2. Kjører plugins via Kontekst
        val kontekst = Prosesskontekst(opplysninger)
        forretningsprosess.kjørUnderveis(kontekst)

        if (rapport.erFerdig()) {
            forretningsprosess.kjørFerdig(kontekst)
        }

        // 3. Sjekker status - Det "magiske" punktet
        if (kontekst.kreverRekjøring) {
            hendelse.info("Plugin endret datagrunnlaget. Rekjører regler (Runde $iterasjon).")
            // Her kaller vi funksjonen på nytt - kompilatoren optimaliserer dette til en loop
            return kjørRegler(hendelse, iterasjon + 1)
        }

        // 4. Hvis vi kommer hit, er dataene stabile. Håndter ferdigstillelse.
        if (rapport.erFerdig()) {
            avgjørNesteTilstand(hendelse)
        }
    }

    // Behandlingen er ferdig og vi må rute til forslag eller godkjenning
    private fun avgjørNesteTilstand(hendelse: PersonHendelse) {
        if (aktiveAvklaringer().isNotEmpty()) {
            return tilstand(ForslagTilVedtak(), hendelse)
        }

        if (!erAutomatiskBehandlet()) {
            return tilstand(TilGodkjenning(), hendelse)
        }

        if (kreverTotrinnskontroll()) {
            return tilstand(TilGodkjenning(), hendelse)
        }

        return tilstand(Ferdig(), hendelse)
    }

    private fun harRettighetsperioder(): Boolean {
        val perioder = forretningsprosess.regelverk.rettighetsperioder(opplysninger())
        return perioder.isNotEmpty()
    }

    private fun erAutomatiskBehandlet(): Boolean {
        val ingenAvklaringerLøstAvSaksbehandler = avklaringer().none { it.løstAvSaksbehandler() }
        val ingenOpplysningerFraSaksbehandler = opplysninger.kunEgne.somListe().none { it.kilde is Saksbehandlerkilde }
        val ikkeGodkjentUtført = !godkjent.erUtført

        return ingenAvklaringerLøstAvSaksbehandler && ingenOpplysningerFraSaksbehandler && ikkeGodkjentUtført
    }

    private fun tilstand(
        nyTilstand: BehandlingTilstand,
        hendelse: PersonHendelse,
    ) {
        if (tilstand.type == nyTilstand.type) return
        tilstand.leaving(this, hendelse)

        val forrigeTilstand = tilstand
        tilstand = nyTilstand

        hendelse.kontekst(tilstand)
        emitVedtaksperiodeEndret(forrigeTilstand)

        tilstand.entering(this, hendelse)
    }

    private fun basertPåBehandlinger() = basertPå?.behandlingId

    val vedtakopplysninger
        get() =
            Resultat(
                behandlingId = behandlingId,
                basertPåBehandling = basertPåBehandlinger(),
                behandlingskjedeId = behandlingskjedeId,
                rettighetsperioder = forretningsprosess.rettighetsperioder(opplysninger()),
                virkningsdato = forretningsprosess.virkningsdato(opplysninger()),
                behandlingAv = behandler,
                opplysninger = opplysninger,
                automatiskBehandlet = erAutomatiskBehandlet(),
                godkjentAv = godkjent,
                besluttetAv = besluttet,
                opprettet = opprettet,
                sistEndret = sistEndret,
            )

    private fun emitOpprettet() {
        val event =
            BehandlingOpprettet(
                behandlingId = behandlingId,
                basertPåBehandlinger = basertPåBehandlinger(),
                behandlingskjedeId = behandlingskjedeId,
                hendelse = behandler,
            )

        observatører.forEach { it.opprettet(event) }
    }

    private fun emitForslagTilVedtak() {
        val event =
            BehandlingObservatør.BehandlingForslagTilVedtak(
                this.vedtakopplysninger,
            )

        observatører.forEach { it.forslagTilVedtak(event) }
    }

    private fun emitFerdig() {
        val event = BehandlingFerdig(this.vedtakopplysninger)

        observatører.forEach { it.ferdig(event) }
    }

    private fun emitAvbrutt(årsak: String?) {
        val event =
            BehandlingAvbrutt(
                behandlingId = behandlingId,
                hendelse = behandler.eksternId,
                årsak = årsak,
            )

        observatører.forEach { it.avbrutt(event) }
    }

    private fun emitAvklaringLukket(
        avklaringId: UUID,
        kode: String,
    ) {
        val event =
            AvklaringLukket(
                behandlingId = behandlingId,
                hendelse = behandler.eksternId,
                avklaringId = avklaringId,
                kode = kode,
            )
        observatører.forEach { it.avklaringLukket(event) }
    }

    private fun emitVedtaksperiodeEndret(forrigeTilstand: BehandlingTilstand) {
        val event =
            BehandlingObservatør.BehandlingEndretTilstand(
                behandlingId = behandlingId,
                gjeldendeTilstand = tilstand.type,
                forrigeTilstand = forrigeTilstand.type,
                forventetFerdig = tilstand.forventetFerdig,
                tidBrukt = Duration.between(forrigeTilstand.opprettet, tilstand.opprettet),
            )

        observatører.forEach { it.endretTilstand(event) }
    }

    interface VedtakOpplysninger {
        val behandlingId: UUID
        val basertPåBehandling: UUID?
        val behandlingskjedeId: UUID
        val rettighetsperioder: List<Rettighetsperiode>
        val virkningsdato: LocalDate
        val behandlingAv: StartHendelse
        val opplysninger: LesbarOpplysninger
        val automatiskBehandlet: Boolean
        val godkjentAv: Arbeidssteg
        val besluttetAv: Arbeidssteg
        val opprettet: LocalDateTime
        val sistEndret: LocalDateTime

        fun relevanteVilkår() = behandlingAv.forretningsprosess.regelverk.relevanteVilkår(opplysningerPåVirkningsdato())

        fun opplysningerPåVirkningsdato() = opplysninger.forDato(virkningsdato)
    }

    data class Resultat(
        override val behandlingId: UUID,
        override val basertPåBehandling: UUID?,
        override val behandlingskjedeId: UUID,
        override val rettighetsperioder: List<Rettighetsperiode>,
        override val virkningsdato: LocalDate,
        override val behandlingAv: StartHendelse,
        override val opplysninger: LesbarOpplysninger,
        override val automatiskBehandlet: Boolean,
        override val godkjentAv: Arbeidssteg,
        override val besluttetAv: Arbeidssteg,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
    ) : VedtakOpplysninger
}

interface BehandlingObservatør {
    data class BehandlingOpprettet(
        val behandlingId: UUID,
        val basertPåBehandlinger: UUID?,
        val behandlingskjedeId: UUID,
        val hendelse: StartHendelse,
    ) : PersonEvent()

    sealed class VedtakEvent(
        vedtakOpplysninger: VedtakOpplysninger,
    ) : PersonEvent(),
        VedtakOpplysninger by vedtakOpplysninger

    data class BehandlingForslagTilVedtak(
        private val vedtakOpplysninger: VedtakOpplysninger,
    ) : VedtakEvent(vedtakOpplysninger)

    data class BehandlingFerdig(
        private val vedtakOpplysninger: VedtakOpplysninger,
    ) : VedtakEvent(vedtakOpplysninger)

    data class BehandlingEndretTilstand(
        val behandlingId: UUID,
        val gjeldendeTilstand: Behandling.TilstandType,
        val forrigeTilstand: Behandling.TilstandType,
        val forventetFerdig: LocalDateTime,
        val tidBrukt: Duration,
    ) : PersonEvent()

    data class BehandlingAvbrutt(
        val behandlingId: UUID,
        val hendelse: EksternId<*>,
        val årsak: String? = null,
    ) : PersonEvent()

    data class AvklaringLukket(
        val behandlingId: UUID,
        val hendelse: EksternId<*>,
        val avklaringId: UUID,
        val kode: String,
    ) : PersonEvent()

    fun opprettet(event: BehandlingOpprettet) {}

    fun forslagTilVedtak(event: BehandlingForslagTilVedtak) {}

    fun avbrutt(event: BehandlingAvbrutt) {}

    fun ferdig(event: BehandlingFerdig) {}

    fun endretTilstand(event: BehandlingEndretTilstand) {}

    // TODO: Burde flyttes til en egen observer
    fun avklaringLukket(event: AvklaringLukket) {}
}

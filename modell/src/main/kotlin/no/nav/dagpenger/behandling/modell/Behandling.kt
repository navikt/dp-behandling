package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.aktivitetslogg.Varselkode
import no.nav.dagpenger.aktivitetslogg.aktivitet.Hendelse
import no.nav.dagpenger.behandling.modell.Behandling.BehandlingTilstand.Companion.fraType
import no.nav.dagpenger.behandling.modell.BehandlingHendelser.VedtakFattetHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ManuellBehandlingAvklartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Ulid
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Søknadstidspunkt
import java.time.LocalDateTime
import java.util.UUID

class Behandling private constructor(
    val behandlingId: UUID,
    val behandler: StartHendelse,
    gjeldendeOpplysninger: Opplysninger,
    val basertPå: List<Behandling> = emptyList(),
    private var tilstand: BehandlingTilstand,
) : Aktivitetskontekst, BehandlingHåndter {
    constructor(
        behandler: StartHendelse,
        opplysninger: List<Opplysning<*>>,
        basertPå: List<Behandling> = emptyList(),
    ) : this(UUIDv7.ny(), behandler, Opplysninger(opplysninger), basertPå, UnderOpprettelse(LocalDateTime.now()))

    init {
        require(basertPå.all { it.tilstand is Ferdig }) {
            "Kan ikke basere en ny behandling på en som ikke er ferdig"
        }
    }

    private val observatører = mutableListOf<BehandlingObservatør>()

    private val tidligereOpplysninger: List<Opplysninger> = basertPå.map { it.opplysninger }
    private val opplysninger: Opplysninger = gjeldendeOpplysninger + tidligereOpplysninger

    private val regelkjøring = Regelkjøring(behandler.skjedde, opplysninger, *behandler.regelsett().toTypedArray())

    companion object {
        fun rehydrer(
            behandlingId: UUID,
            behandler: StartHendelse,
            gjeldendeOpplysninger: Opplysninger,
            basertPå: List<Behandling> = emptyList(),
            tilstand: TilstandType,
            sistEndretTilstand: LocalDateTime,
        ) = Behandling(behandlingId, behandler, gjeldendeOpplysninger, basertPå, fraType(tilstand, sistEndretTilstand))

        fun List<Behandling>.finn(behandlingId: UUID) =
            try {
                single { it.behandlingId == behandlingId }
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Fant flere behandlinger med samme id, id=$behandlingId", e)
            }
    }

    fun tilstand() = Pair(tilstand.type, tilstand.opprettet)

    fun opplysninger(): LesbarOpplysninger = opplysninger

    private fun informasjonsbehov() = regelkjøring.informasjonsbehov(behandler.avklarer())

    override fun håndter(hendelse: SøknadInnsendtHendelse) {
        hendelse.kontekst(this)
        tilstand.håndter(this, hendelse)
    }

    override fun håndter(hendelse: ManuellBehandlingAvklartHendelse) {
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

    private fun hvaTrengerViNå(hendelse: PersonHendelse) =
        informasjonsbehov().onEach { (behov, avhengigheter) ->
            hendelse.behov(
                type = OpplysningBehov(behov.id),
                melding = "Trenger en opplysning (${behov.id})",
                detaljer =
                    mapOf("@opplysningsbehov" to true) +
                        avhengigheter.associate { avhengighet ->
                            val verdi =
                                when (avhengighet.verdi) {
                                    is Ulid -> (avhengighet.verdi as Ulid).verdi
                                    else -> avhengighet.verdi
                                }
                            avhengighet.opplysningstype.id to verdi
                        } +
                        // TODO: Midlertidlig hack for å få med søknadId for gamle behovløsere
                        mapOf(
                            "InnsendtSøknadsId" to mapOf("urn" to "urn:soknad:${behandler.eksternId.id}"),
                            // søknad_uuid er egentlig ID på prosessen i quiz, som ikke er det samme som søknaden som behandles sin ID
                            "søknad_uuid" to behandlingId.toString(),
                        ),
            )
        }

    fun registrer(observatør: BehandlingObservatør) {
        observatører.add(observatør)
    }

    override fun toSpesifikkKontekst() = BehandlingKontekst(behandlingId, behandler.kontekstMap())

    override fun equals(other: Any?) = other is Behandling && behandlingId == other.behandlingId

    override fun hashCode() = behandlingId.hashCode()

    data class BehandlingKontekst(val behandlingId: UUID, val behandlerKontekst: Map<String, String>) : SpesifikkKontekst("Behandling") {
        override val kontekstMap = mapOf("behandlingId" to behandlingId.toString()) + behandlerKontekst
    }

    enum class TilstandType {
        UnderOpprettelse,
        UnderBehandling,
        ForslagTilVedtak,
        Avbrutt,
        Ferdig,
    }

    private sealed interface BehandlingTilstand : Aktivitetskontekst {
        val type: TilstandType
        val opprettet: LocalDateTime

        companion object {
            fun fraType(
                type: TilstandType,
                opprettet: LocalDateTime,
            ) = when (type) {
                TilstandType.UnderOpprettelse -> UnderOpprettelse(opprettet)
                TilstandType.UnderBehandling -> UnderBehandling(opprettet)
                TilstandType.ForslagTilVedtak -> ForslagTilVedtak(opprettet)
                TilstandType.Avbrutt -> Avbrutt(opprettet)
                TilstandType.Ferdig -> Ferdig(opprettet)
            }
        }

        fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
        }

        fun håndter(
            behandling: Behandling,
            hendelse: SøknadInnsendtHendelse,
        ) {
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )
        }

        fun håndter(
            behandling: Behandling,
            hendelse: ManuellBehandlingAvklartHendelse,
        ) {
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )
        }

        fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )
        }

        fun håndter(
            behandling: Behandling,
            hendelse: AvbrytBehandlingHendelse,
        ) {
            hendelse.info("Avbryter behandlingen")
            behandling.tilstand(Avbrutt(), hendelse)
        }

        fun håndter(
            behandling: Behandling,
            hendelse: ForslagGodkjentHendelse,
        ) {
            throw IllegalStateException(
                "Kan ikke håndtere hendelse ${hendelse.javaClass.simpleName} i tilstand ${this.javaClass.simpleName}",
            )
        }

        override fun toSpesifikkKontekst() =
            SpesifikkKontekst(
                type.name,
                mapOf(
                    "opprettet" to opprettet.toString(),
                ),
            )
    }

    private data class UnderOpprettelse(override val opprettet: LocalDateTime = LocalDateTime.now()) : BehandlingTilstand {
        override val type = TilstandType.UnderOpprettelse

        override fun håndter(
            behandling: Behandling,
            hendelse: SøknadInnsendtHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Mottatt søknad og startet behandling")
            hendelse.hendelse(BehandlingHendelser.BehandlingOpprettetHendelse, "Behandling opprettet")

            behandling.tilstand(UnderBehandling(), hendelse)
        }
    }

    private data class UnderBehandling(override val opprettet: LocalDateTime = LocalDateTime.now()) : BehandlingTilstand {
        override val type = TilstandType.UnderBehandling

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            behandling.observatører.forEach { it.behandlingStartet() }
            val trenger = behandling.hvaTrengerViNå(hendelse)

            if (trenger.isEmpty()) {
                behandling.tilstand(ForslagTilVedtak(), hendelse)
            }
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.opplysninger.forEach { opplysning ->
                behandling.opplysninger.leggTil(opplysning.opplysning())
            }
            val trenger = behandling.hvaTrengerViNå(hendelse)

            if (trenger.isEmpty()) {
                // TODO: requireNotNull(behandling.opplysninger.finnOpplysning(behandling.behandler.avklarer())) { "Har ikke klart å avklare behandlingen" }

                val avklaring = behandling.opplysninger.finnOpplysning(behandling.behandler.avklarer())
                if (avklaring.verdi) {
                    hendelse.info("Behandling fører ikke til avslag, det støtter vi ikke enda")
                    behandling.tilstand(Avbrutt(årsak = "Førte ikke til avslag"), hendelse)
                    return
                }

                // TODO: Dette burde vel også vært en avklaring-ting
                val kravTilInntekt = behandling.opplysninger.finnOpplysning(Minsteinntekt.minsteinntekt)
                if (kravTilInntekt.verdi) {
                    hendelse.info("Behandling er avslag, men kravet til inntekt er oppfylt, det støtter vi ikke enda")
                    behandling.tilstand(Avbrutt(årsak = "Førte ikke til avslag på grunn av inntekt"), hendelse)
                    return
                }

                val søknadstidspunkt = behandling.opplysninger.finnOpplysning(Søknadstidspunkt.søknadstidspunkt).verdi
                if (søknadstidspunkt.isAfter(behandling.behandler.skjedde.plusDays(14))) {
                    hendelse.info("Behandling kunne vært automatisk avslag, men ligger for langt fram i tid")
                    behandling.tilstand(Avbrutt(årsak = "Virkningstidspunkt ligger mer enn 14 dager fram i tid"), hendelse)
                    return
                }

                hendelse.behov(BehandlingBehov.AvklaringManuellBehandling, "Trenger informasjon for å avklare manuell behandling")
            }
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: ManuellBehandlingAvklartHendelse,
        ) {
            // Her vet vi at det skal være avslag på grunn av minste arbeidsinntekt.
            if (hendelse.behandlesManuelt) {
                behandling.tilstand(ForslagTilVedtak(), hendelse)
                return
            }

            behandling.tilstand(Ferdig(), hendelse)
        }
    }

    private data class ForslagTilVedtak(override val opprettet: LocalDateTime = LocalDateTime.now()) : BehandlingTilstand {
        override val type = TilstandType.ForslagTilVedtak

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Alle opplysninger mottatt, lager forslag til vedtak")

            // @todo: PoC for å sende avklaringer til STBS gjengen. Vi burde lage en mekanisme som tar vare på avklaringer gitt en behandling.
            val avklaringer =
                if (hendelse is ManuellBehandlingAvklartHendelse) {
                    hendelse.avklaringer.filter { it.utfall == "Manuell" }.map {
                        mapOf(
                            "type" to it.type,
                            "utfall" to it.utfall,
                            "begrunnelse" to it.begrunnelse,
                        )
                    }
                } else {
                    emptyList()
                }
            hendelse.hendelse(
                BehandlingHendelser.ForslagTilVedtakHendelse,
                "Foreslår vedtak",
                mapOf(
                    "utfall" to behandling.opplysninger.finnOpplysning(behandling.behandler.avklarer()).verdi,
                    "harAvklart" to behandling.opplysninger.finnOpplysning(behandling.behandler.avklarer()).opplysningstype.navn,
                    "avklaringer" to avklaringer,
                ),
            )
            behandling.observatører.forEach { it.forslagTilVedtak() }
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: ForslagGodkjentHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Forslag til vedtak godkjent")
            // TODO: Hva mer gjør vi når vi har godkjent forslaget?
            // Sjekke aksjonspunkter/varsel/hypoteser?
            if (behandling.opplysninger.finnAlle().any { it is Hypotese<*> }) {
                // TODO: Vi bør sannsynligvis gjøre dette
                // throw IllegalStateException("Forslaget inneholder hypoteser, kan ikke godkjennes")
            }

            behandling.tilstand(Ferdig(), hendelse)
        }
    }

    private data class Avbrutt(override val opprettet: LocalDateTime = LocalDateTime.now(), val årsak: String? = null) :
        BehandlingTilstand {
        override val type = TilstandType.Avbrutt

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            hendelse.info("Behandling avbrutt")
            hendelse.hendelse(
                BehandlingHendelser.AvbrytBehandlingHendelse,
                "Behandling avbrutt",
                årsak?.let { mapOf("årsak" to it) } ?: emptyMap(),
            )
            behandling.observatører.forEach { it.avbrutt() }
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvbrytBehandlingHendelse,
        ) { // No-op
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: ManuellBehandlingAvklartHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er avbrutt, ignorerer manuell behandling")
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: OpplysningSvarHendelse,
        ) {
            hendelse.kontekst(this)
            hendelse.info("Behandlingen er avbrutt, ignorerer opplysningssvar")
        }
    }

    private data class Ferdig(override val opprettet: LocalDateTime = LocalDateTime.now()) : BehandlingTilstand {
        override val type = TilstandType.Ferdig

        override fun entering(
            behandling: Behandling,
            hendelse: PersonHendelse,
        ) {
            behandling.observatører.forEach { it.ferdig() }
            // TODO: Dette er vel strengt tatt ikke vedtak fattet?
            val avklaring = behandling.opplysninger.finnOpplysning(behandling.behandler.avklarer())
            hendelse.hendelse(
                VedtakFattetHendelse,
                "Vedtak fattet",
                mapOf(
                    "utfall" to avklaring.verdi,
                    "harAvklart" to avklaring.opplysningstype.navn,
                    "opplysninger" to behandling.opplysninger.finnAlle(),
                ),
            )
        }

        override fun håndter(
            behandling: Behandling,
            hendelse: AvbrytBehandlingHendelse,
        ) {
            throw IllegalStateException("Kan ikke avbryte en ferdig behandling")
        }
    }

    private fun tilstand(
        nyTilstand: BehandlingTilstand,
        hendelse: PersonHendelse,
    ) {
        if (tilstand.type == nyTilstand.type) return

        val forrigeTilstand = tilstand
        tilstand = nyTilstand
        hendelse.kontekst(tilstand)
        tilstand.entering(this, hendelse)
    }
}

interface BehandlingObservatør {
    fun behandlingStartet() {}

    fun forslagTilVedtak() {}

    fun avbrutt() {}

    fun ferdig() {}
}

@Suppress("ktlint:standard:class-naming")
object Behandlingsvarsler {
    data object SØKNAD_MOTTATT : Varselkode("Søknad mottatt - midlertidlig test av varsel")
}

// TODO: Vi bør ha bedre kontroll på navnene og kanskje henge sammen med behov?
@Suppress("ktlint:standard:enum-entry-name-case")
sealed class BehandlingHendelser(override val name: String) : Hendelse.Hendelsetype {
    data object BehandlingOpprettetHendelse : BehandlingHendelser("behandling_opprettet")

    data object ForslagTilVedtakHendelse : BehandlingHendelser("forslag_til_vedtak")

    data object VedtakFattetHendelse : BehandlingHendelser("vedtak_fattet")

    data object AvbrytBehandlingHendelse : BehandlingHendelser("behandling_avbrutt")
}

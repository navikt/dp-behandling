package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.aktivitetslogg.Varselkode
<<<<<<< HEAD
import no.nav.dagpenger.aktivitetslogg.aktivitet.Hendelse
=======
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingEvent
import no.nav.dagpenger.behandling.modell.hendelser.BehandlingHendelse
>>>>>>> a7d05b9 (WIP: Tilstand på behandling og kvitte seg med observatører.)
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøkerHendelse
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Ulid
import java.util.UUID

interface Behandler {
    fun behandle(hendelse: SøkerHendelse) {}
}

class Behandling private constructor(
    val behandlingId: UUID,
    private val behandler: BehandlingHendelse,
    aktiveOpplysninger: List<Opplysning<*>> = emptyList(),
    basertPå: List<Behandling> = emptyList(),
) : Aktivitetskontekst, Behandler {
    constructor(
        behandler: SøkerHendelse,
        opplysninger: List<Opplysning<*>>,
        basertPå: List<Behandling> = emptyList(),
    ) : this(UUIDv7.ny(), behandler, opplysninger, basertPå)

    private var tilstand: Tilstand = Underbehandling().also { it.onEntering(behandler) }

    private val tidligereOpplysninger: List<Opplysninger> = basertPå.map { it.opplysninger }
    private val opplysninger = Opplysninger(aktiveOpplysninger, tidligereOpplysninger)

    private val regelkjøring = Regelkjøring(behandler.gjelderDato, opplysninger, *behandler.regelsett().toTypedArray())

    fun opplysninger(): LesbarOpplysninger = opplysninger

    private fun informasjonsbehov() = regelkjøring.informasjonsbehov(behandler.avklarer())


    override fun behandle(hendelse: SøkerHendelse) {
        tilstand.behandle(hendelse)
    }


    fun håndter(hendelse: OpplysningSvarHendelse) {
        hendelse.kontekst(this)
        hendelse.opplysninger.forEach { opplysning ->
            opplysninger.leggTil(opplysning.opplysning())
        }
        val trenger = hvaTrengerViNå(hendelse)

        if (trenger.isEmpty()) {
            // TODO: Tilstand?
            hendelse.info("Alle opplysninger mottatt")
            hendelse.hendelse(BehandlingHendelser.forslag_til_vedtak, "Foreslår vedtak")

        }
    }


    private sealed class Tilstand : Aktivitetskontekst, Behandler {

        open fun onEntering(hendelse: BehandlingHendelse) {}
        enum class Type {
            UNDER_BEHANDLING,
        }
        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst(this.javaClass.simpleName, emptyMap())
        }

        abstract val tilstandType: Type


    }

    private inner class Underbehandling : Tilstand() {

        override fun onEntering(hendelse: BehandlingHendelse) {

           //  hendelse.hendelse(BehandlingEvent.Opprettet)

        }
        override val tilstandType: Type
            get() = Type.UNDER_BEHANDLING

        override fun behandle(hendelse: SøkerHendelse) {
            hendelse.info("Mottatt søknad og startet behandling")
            hendelse.varsel(Behandlingsvarsler.SØKNAD_MOTTATT)
            hendelse.hendelse(BehandlingHendelser.behandling_opprettet, "Behandling opprettet")
        }
    }

    private fun hvaTrengerViNå(hendelse: PersonHendelse) =
        informasjonsbehov().onEach { (behov, avhengigheter) ->
            hendelse.behov(
                type = OpplysningBehov(behov.id),
                melding = "Trenger en opplysning (${behov.id})",
                detaljer =
                    avhengigheter.associate { avhengighet ->
                        val verdi =
                            when (avhengighet.verdi) {
                                is Ulid -> (avhengighet.verdi as Ulid).verdi
                                else -> avhengighet.verdi
                            }
                        avhengighet.opplysningstype.id to verdi
                    }
            )
        }

    override fun toSpesifikkKontekst() = BehandlingKontekst(behandlingId, behandler.søknadId)

    // TODO: VIl helst ikke ha søknadId inn her
    data class BehandlingKontekst(val behandlingId: UUID, val søknadId: UUID) : SpesifikkKontekst("Behandling") {
        override val kontekstMap = mapOf("behandlingId" to behandlingId.toString(), "søknadId" to søknadId.toString())
    }
}

@Suppress("ktlint:standard:class-naming")
object Behandlingsvarsler {
    data object SØKNAD_MOTTATT : Varselkode("Søknad mottatt - midlertidlig test av varsel")
}

@Suppress("ktlint:standard:enum-entry-name-case")
enum class BehandlingHendelser : Hendelse.Hendelsetype {
    behandling_opprettet,
    forslag_til_vedtak,
}

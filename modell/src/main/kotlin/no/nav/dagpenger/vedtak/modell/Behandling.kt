package no.nav.dagpenger.vedtak.modell

import no.nav.dagpenger.aktivitetslogg.Aktivitet
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.regel.RettTilDagpenger
import no.nav.dagpenger.vedtak.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.vedtak.modell.hendelser.PersonHendelse
import no.nav.dagpenger.vedtak.modell.hendelser.SøkerHendelse
import no.nav.dagpenger.vedtak.modell.hendelser.SøknadInnsendtHendelse
import java.util.UUID

class Behandling private constructor(
    internal val behandlingId: UUID,
    private val behandler: SøkerHendelse,
    private val opplysninger: Opplysninger,
    private vararg val regelsett: Regelsett,
) : Aktivitetskontekst {
    constructor(
        behandler: SøkerHendelse,
        opplysninger: Opplysninger,
        vararg regelsett: Regelsett,
    ) : this(UUIDv7.ny(), behandler, opplysninger, *regelsett)

    private val regelkjøring = Regelkjøring(behandler.gjelderDato, opplysninger, *regelsett)

    private fun informasjonsbehov() = regelkjøring.informasjonsbehov(RettTilDagpenger.kravPåDagpenger)

    fun håndter(hendelse: SøknadInnsendtHendelse) {
        hendelse.kontekst(this)

        hvaTrengerViNå(hendelse)
    }

    fun håndter(hendelse: OpplysningSvarHendelse) {
        hendelse.kontekst(this)
        hendelse.opplysninger.forEach { opplysning ->
            opplysninger.leggTil(opplysning.opplysning())
        }
        hvaTrengerViNå(hendelse)
    }

    private fun hvaTrengerViNå(hendelse: PersonHendelse) {
        informasjonsbehov().forEach { (behov, avhengigheter) ->
            hendelse.behov(
                OpplysningBehov(behov.id),
                "Trenger en opplysning",
                avhengigheter.associate { av -> av.opplysningstype.id to av.verdi },
            )
        }
    }

    override fun toSpesifikkKontekst() = SpesifikkKontekst("Behandling", mapOf("behandlingId" to behandlingId.toString()))
}

data class OpplysningBehov(override val name: String) : Aktivitet.Behov.Behovtype

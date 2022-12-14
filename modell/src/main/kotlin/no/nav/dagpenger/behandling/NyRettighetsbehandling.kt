package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakAvslåttBehov
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilgetBehov
import no.nav.dagpenger.behandling.hendelser.AldersvilkårLøsning
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.SøknadHendelse
import no.nav.dagpenger.behandling.vilkår.AldersVilkårvurdering
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Companion.erFerdig
import no.nav.dagpenger.behandling.vilkår.Vilkårsvurdering.Tilstand.Type.Oppfylt
import java.util.UUID

class NyRettighetsbehandling private constructor(private val søknadUUID: UUID, behandlingsId: UUID) : Behandling(behandlingsId) {

    // todo : Behandling har tilstander ?

    companion object {
        fun List<Behandling>.harSøknadUUID(søknadUUID: UUID) = this.any { it is NyRettighetsbehandling && it.søknadUUID == søknadUUID }
    }

    constructor(søknadUUID: UUID) : this(søknadUUID, UUID.randomUUID())

    override val vilkårsvurderinger = listOf(
        AldersVilkårvurdering(),
    )

    override fun håndter(hendelse: SøknadHendelse) {
        kontekst(hendelse, "Opprettet ny rettighetsbehandling basert på søknadhendelse")
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(hendelse)
        }
    }

    override fun håndter(aldersvilkårLøsning: AldersvilkårLøsning) {
        if (this.behandlingId != aldersvilkårLøsning.behandlingId()) return
        kontekst(aldersvilkårLøsning, "Mottok løsning for vilkårsvurdering av alder")
        vilkårsvurderinger.forEach { vurdering ->
            vurdering.håndter(aldersvilkårLøsning)
        }
        ferdigstillRettighetsbehandling(aldersvilkårLøsning)
    }

    private fun ferdigstillRettighetsbehandling(hendelse: Hendelse) {
        if (vilkårsvurderinger.erFerdig()) {
            if (vilkårsvurderinger.all { it.tilstand.tilstandType == Oppfylt }) {
                hendelse.behov(VedtakInnvilgetBehov, "Vedtak innvilget", mapOf("sats" to 123))
            } else {
                hendelse.behov(VedtakAvslåttBehov, "Vedtak avslått")
            }
        }
    }

    private fun kontekst(hendelse: Hendelse, melding: String? = null) {
        hendelse.kontekst(this)
        melding?.let {
            hendelse.info(it)
        }
    }
}

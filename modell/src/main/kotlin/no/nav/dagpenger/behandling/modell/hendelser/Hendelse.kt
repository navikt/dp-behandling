package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.BehandletAv
import no.nav.dagpenger.opplysning.Fastsatt
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Hendelse
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.VedtakOpplysninger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Hendelse(
    meldingsreferanseId: UUID,
    override val type: String,
    ident: String,
    eksternId: EksternId<*>,
    skjedde: LocalDate,
    opprettet: LocalDateTime,
    override val forretningsprosess: Forretningsprosess,
) : StartHendelse(meldingsreferanseId, ident, eksternId, skjedde, opprettet),
    Forretningsprosess by forretningsprosess {
    override fun lagVedtak(behandling: Behandling): VedtakOpplysninger {
        val vilkår = forretningsprosess.regelverk.vilkår(behandling.opplysninger)
        val utfall = vilkår.all { it.status }
        val fastsatt =
            Fastsatt
                .FastsattBuilder()
                .utfall(utfall)
                .also {
                    forretningsprosess.regelverk.giMegFastsettelser(it)
                }.build(behandling.opplysninger)

        return VedtakOpplysninger(
            vedtakId = behandling.behandlingId,
            vedtaksdato = LocalDateTime.now().toLocalDate(),
            virkningsdato = skjedde,
            vilkår = vilkår,
            fastsatt = fastsatt,
            utbetalinger = emptyList(),
            behandletAv =
                listOfNotNull(
                    behandling.godkjent.takeIf { it.erUtført }?.let {
                        BehandletAv(
                            BehandletAv.Rolle.saksbehandler,
                            Saksbehandler(it.utførtAv.ident),
                        )
                    },
                    behandling.besluttet.takeIf { it.erUtført }?.let {
                        BehandletAv(
                            BehandletAv.Rolle.beslutter,
                            Saksbehandler(it.utførtAv.ident),
                        )
                    },
                ),
            behandletHendelse =
                with(behandling.behandler.eksternId) {
                    Hendelse(
                        id = this.id.toString(),
                        datatype = this.datatype,
                        type =
                            when (this) {
                                is MeldekortId -> Hendelse.Type.Meldekort
                                is SøknadId -> Hendelse.Type.Søknad
                            },
                    )
                },
        )
    }

    override fun behandling(forrigeBehandling: Behandling?): Behandling =
        throw IllegalStateException("Skal ikke opprettet behandling her, skal allerede ha skjedd")
}

package no.nav.dagpenger.behandling.mediator.api

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.behandling.api.models.BehandletAvDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTORolleDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.UtbetalingDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato

internal fun Behandling.VedtakOpplysninger.tilBehandlingsresultatDTO(ident: String): BehandlingsresultatDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val opplysningSet = opplysninger.somListe()
        val egneId = opplysninger.somListe(Egne).map { it.id }

        BehandlingsresultatDTO(
            behandlingId = behandlingId,
            ident = ident,
            automatisk = automatiskBehandlet,
            basertPå = basertPåBehandling,
            behandlingskjedeId = behandlingskjedeId,
            behandletHendelse = behandlingAv.tilHendelseDTO(),
            rettighetsperioder = rettighetsperioder(),
            opplysninger =
                opplysningSet.somOpplysningperiode { type, opplysninger ->
                    OpplysningerDTO(
                        opplysningTypeId = type.id.uuid,
                        navn = type.navn,
                        datatype = type.datatype.tilDataTypeDTO(),
                        perioder = opplysninger.map { opplysning -> opplysning.tilOpplysningsperiodeDTO(egneId) },
                    )
                },
            utbetalinger = tilUtbetalingDTO(opplysninger),
            behandletAv = this.tilBehandletAvDTO(),
            førteTil = rettighetsperioder.avgjørelse(),
            opprettet = opprettet,
            sistEndret = sistEndret,
        )
    }

private fun Behandling.VedtakOpplysninger.tilBehandletAvDTO(): List<BehandletAvDTO> =
    listOfNotNull(
        godkjentAv.takeIf { it.erUtført }?.let {
            BehandletAvDTO(
                BehandletAvDTORolleDTO.SAKSBEHANDLER,
                SaksbehandlerDTO(it.utførtAv.ident),
            )
        },
        besluttetAv.takeIf { it.erUtført }?.let {
            BehandletAvDTO(
                BehandletAvDTORolleDTO.BESLUTTER,
                SaksbehandlerDTO(it.utførtAv.ident),
            )
        },
    )

internal fun Behandling.VedtakOpplysninger.tilUtbetalingDTO(opplysninger: LesbarOpplysninger): List<UtbetalingDTO> {
    val utbetalinger = behandlingAv.forretningsprosess.regelverk.utbetalinger(opplysninger)
    return utbetalinger.map {
        UtbetalingDTO(
            meldeperiode = it.meldeperiode,
            dato = it.dato,
            sats = it.sats,
            utbetaling = it.utbetaling,
            opprinnelse = it.endret.tilOpprinnelseDTO(),
        )
    }
}

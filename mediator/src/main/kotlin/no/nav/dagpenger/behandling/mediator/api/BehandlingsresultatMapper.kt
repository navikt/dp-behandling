package no.nav.dagpenger.behandling.mediator.api

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.behandling.api.models.BehandletAvDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTORolleDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTOKlassifiseringDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.UtbetalingBaseDTO
import no.nav.dagpenger.behandling.api.models.UtbetalingFerietilleggDTO
import no.nav.dagpenger.behandling.api.models.UtbetalingMeldekortDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Utbetaling

internal fun Behandling.VedtakOpplysninger.tilBehandlingsresultatDTO(ident: String): BehandlingsresultatDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val opplysningSet = opplysninger.somListe()
        val egneId = opplysninger.somListe(Egne).map { it.id }
        val regelsett = behandlingAv.forretningsprosess.regelsett()
        val produsertAv =
            regelsett
                .flatMap { rs -> rs.produserer.map { it to rs } }
                .toMap()

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
                    val tilhører = produsertAv.getOrDefault(type, null)

                    OpplysningerDTO(
                        opplysningTypeId = type.id.uuid,
                        navn = type.navn,
                        datatype = type.datatype.tilDataTypeDTO(),
                        klassifisering = type.tilKlassifiseringDTO(tilhører),
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

private fun Opplysningstype<*>.tilKlassifiseringDTO(tilhører: Regelsett?): OpplysningerDTOKlassifiseringDTO? {
    if (tilhører == null) return null
    return when (tilhører.type) {
        RegelsettType.Vilkår -> if (tilhører.utfall == this) OpplysningerDTOKlassifiseringDTO.UTFALL else null
        else -> null
    }
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

internal fun Behandling.VedtakOpplysninger.tilUtbetalingDTO(opplysninger: LesbarOpplysninger): List<UtbetalingBaseDTO> {
    val utbetalinger = behandlingAv.forretningsprosess.regelverk.utbetalinger(opplysninger)
    return utbetalinger.map {
        when (it) {
            is Utbetaling.Ferietillegg -> {
                UtbetalingFerietilleggDTO(
                    dato = it.dato,
                    utbetaling = it.utbetaling,
                    opprinnelse = it.endret.tilOpprinnelseDTO(),
                )
            }

            is Utbetaling.Meldekort -> {
                UtbetalingMeldekortDTO(
                    meldeperiode = it.meldeperiode,
                    dato = it.dato,
                    sats = it.sats,
                    utbetaling = it.utbetaling,
                    opprinnelse = it.endret.tilOpprinnelseDTO(),
                )
            }
        }
    }
}

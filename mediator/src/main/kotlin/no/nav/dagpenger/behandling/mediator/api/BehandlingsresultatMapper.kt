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
import no.nav.dagpenger.regel.KravPåDagpenger
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import java.util.UUID

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
            utbetalinger = opplysninger.utbetalinger(egneId),
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

internal fun LesbarOpplysninger.utbetalinger(egneId: List<UUID>): List<UtbetalingDTO> {
    val meldeperioder = finnAlle(Beregning.meldeperiode)

    val løpendeRett = finnAlle(KravPåDagpenger.harLøpendeRett)
    val satser = finnAlle(dagsatsEtterSamordningMedBarnetillegg)
    val dager = finnAlle(Beregning.utbetaling).associateBy { it.gyldighetsperiode.fraOgMed }

    return meldeperioder.flatMap { periode ->
        periode.verdi.mapNotNull { dato ->
            if (løpendeRett.filter { it.verdi }.none { it.gyldighetsperiode.inneholder(dato) }) {
                // Har ikke løpende rett i denne perioden, så ingen utbetaling
                return@mapNotNull null
            }

            val dag = dager[dato] ?: throw IllegalStateException("Mangler utbetaling for dag $dato")
            val sats = satser.first { it.gyldighetsperiode.inneholder(dato) }.verdi
            UtbetalingDTO(
                meldeperiode = periode.verdi.hashCode().toString(),
                dato = dato,
                sats = sats.verdien.toInt(),
                utbetaling = dag.verdi.heleKroner.toInt(),
                opprinnelse = (dag.id in egneId).tilOpprinnelseDTO(),
            )
        }
    }
}

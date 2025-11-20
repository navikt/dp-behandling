package no.nav.dagpenger.behandling.mediator.api

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.behandling.api.models.AvgjørelseDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTORolleDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
import no.nav.dagpenger.behandling.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.mediator.utbetalinger
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Rettighetsperiode

internal fun Behandling.VedtakOpplysninger.tilBehandlingsresultatDTO(ident: String): BehandlingsresultatDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val opplysningSet = opplysninger.somListe()
        val egneId = opplysninger.somListe(Egne).map { it.id }

        BehandlingsresultatDTO(
            behandlingId = behandlingId,
            ident = ident,
            rettighetsperioder = rettighetsperioder(),
            automatisk = automatiskBehandlet,
            basertPå = basertPåBehandling,
            behandlingskjedeId = behandlingskjedeId,
            behandletHendelse =
                HendelseDTO(
                    id = behandlingAv.eksternId.id.toString(),
                    datatype = behandlingAv.eksternId.datatype,
                    type =
                        when (behandlingAv.eksternId) {
                            is MeldekortId -> HendelseDTOTypeDTO.MELDEKORT
                            is SøknadId -> HendelseDTOTypeDTO.SØKNAD
                            is ManuellId -> HendelseDTOTypeDTO.MANUELL
                        },
                    skjedde = behandlingAv.skjedde,
                ),
            opplysninger =
                opplysningSet.groupBy { it.opplysningstype }.map { (type, opplysninger) ->
                    OpplysningerDTO(
                        opplysningTypeId = type.id.uuid,
                        navn = type.navn,
                        datatype = type.datatype.tilDataTypeDTO(),
                        perioder = opplysninger.map { opplysning -> opplysning.tilOpplysningsperiodeDTO(egneId) },
                    )
                },
            utbetalinger = opplysninger.utbetalinger(),
            behandletAv =
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
                ),
            førteTil = this.rettighetsperioder.avgjørelse(),
        )
    }

fun List<Rettighetsperiode>.avgjørelse(): AvgjørelseDTO {
    val (nye, arvede) = this.partition { it.endret }

    return when {
        // Ingen endring
        nye.isEmpty() -> AvgjørelseDTO.ENDRING
        // Ny kjede
        arvede.isEmpty() -> if (nye.harRett()) AvgjørelseDTO.INNVILGELSE else AvgjørelseDTO.AVSLAG
        // Bygger videre på en kjede
        arvede.sisteHarRett() && !nye.harRett() -> AvgjørelseDTO.STANS
        else -> AvgjørelseDTO.GJENOPPTAK
    }
}

private fun List<Rettighetsperiode>.harRett() = any { it.harRett }

private fun List<Rettighetsperiode>.sisteHarRett() = last().harRett

private fun Behandling.VedtakOpplysninger.rettighetsperioder(): List<RettighetsperiodeDTO> {
    val perioder = behandlingAv.forretningsprosess.regelverk.rettighetsperioder(opplysninger)
    return perioder.map {
        RettighetsperiodeDTO(
            fraOgMed = it.fraOgMed,
            tilOgMed = it.tilOgMed.tilApiDato(),
            harRett = it.harRett,
            opprinnelse = it.endret.tilOpprinnelseDTO(),
        )
    }
}

private fun Boolean.tilOpprinnelseDTO() =
    when (this) {
        true -> OpprinnelseDTO.NY
        false -> OpprinnelseDTO.ARVET
    }

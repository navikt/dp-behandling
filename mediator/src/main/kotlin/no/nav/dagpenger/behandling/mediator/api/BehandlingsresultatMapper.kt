package no.nav.dagpenger.behandling.mediator.api

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
import no.nav.dagpenger.behandling.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne

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
        )
    }

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

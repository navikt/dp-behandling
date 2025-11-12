package no.nav.dagpenger.behandling.mediator.api

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.behandling.api.models.BarnVerdiDTO
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BegrunnelseDTO
import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.BehandlingTilstandDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.FormålDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.HjemmelDTO
import no.nav.dagpenger.behandling.api.models.LovkildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsperiodeDTO
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.api.models.RedigerbareOpplysningerDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.RegelsettTypeDTO
import no.nav.dagpenger.behandling.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.api.models.VurderingsresultatV2DTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import java.time.LocalDate
import java.util.UUID

internal fun Behandling.tilBehandlingDTO(): BehandlingDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val opplysningSet = opplysninger.somListe()
        val egneId = opplysninger.somListe(Egne).map { it.id }

        BehandlingDTO(
            behandlingId = behandlingId,
            ident = behandler.ident,
            automatisk = vedtakopplysninger.automatiskBehandlet,
            basertPå = vedtakopplysninger.basertPåBehandling,
            behandletHendelse =
                HendelseDTO(
                    id =
                        vedtakopplysninger.behandlingAv.eksternId.id
                            .toString(),
                    datatype = vedtakopplysninger.behandlingAv.eksternId.datatype,
                    type =
                        when (vedtakopplysninger.behandlingAv.eksternId) {
                            is MeldekortId -> HendelseDTOTypeDTO.MELDEKORT
                            is SøknadId -> HendelseDTOTypeDTO.SØKNAD
                            is ManuellId -> HendelseDTOTypeDTO.MANUELL
                        },
                    skjedde = behandler.skjedde,
                ),
            kreverTotrinnskontroll = this.kreverTotrinnskontroll(),
            tilstand =
                when (this.tilstand().first) {
                    Behandling.TilstandType.UnderOpprettelse -> BehandlingTilstandDTO.UNDER_OPPRETTELSE
                    Behandling.TilstandType.UnderBehandling -> BehandlingTilstandDTO.UNDER_BEHANDLING
                    Behandling.TilstandType.ForslagTilVedtak -> BehandlingTilstandDTO.FORSLAG_TIL_VEDTAK
                    Behandling.TilstandType.Låst -> BehandlingTilstandDTO.LÅST
                    Behandling.TilstandType.Avbrutt -> BehandlingTilstandDTO.AVBRUTT
                    Behandling.TilstandType.Ferdig -> BehandlingTilstandDTO.FERDIG
                    Behandling.TilstandType.Redigert -> BehandlingTilstandDTO.REDIGERT
                    Behandling.TilstandType.TilGodkjenning -> BehandlingTilstandDTO.TIL_GODKJENNING
                    Behandling.TilstandType.TilBeslutning -> BehandlingTilstandDTO.TIL_BESLUTNING
                },
            avklaringer = this.avklaringer().map { it.tilAvklaringDTO() },
            vilkår =
                vedtakopplysninger.behandlingAv.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Vilkår)
                    .mapNotNull { it.tilVurderingsresultatDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            fastsettelser =
                vedtakopplysninger.behandlingAv.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Fastsettelse)
                    .mapNotNull { it.tilVurderingsresultatDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            rettighetsperioder = vedtakopplysninger.rettighetsperioder(),
            opplysninger =
                opplysninger().somListe().groupBy { it.opplysningstype }.map { (type, opplysninger) ->
                    RedigerbareOpplysningerDTO(
                        opplysningTypeId = type.id.uuid,
                        navn = type.navn,
                        perioder = opplysninger.map { opplysning -> opplysning.tilOpplysningsperiodeDTO(egneId) },
                        datatype = type.datatype.tilDataTypeDTO(),
                        synlig = type.synlig(this.opplysninger),
                        redigerbar = opplysninger.last().kanRedigeres(redigerbareOpplysninger),
                        redigertAvSaksbehandler = opplysninger.last().kilde is Saksbehandlerkilde,
                        formål = type.tilFormålDTO(),
                    )
                },
        )
    }

private fun Opplysningstype<*>.tilFormålDTO(): FormålDTO =
    when (formål) {
        Opplysningsformål.Legacy -> FormålDTO.LEGACY
        Opplysningsformål.Bruker -> FormålDTO.BRUKER
        Opplysningsformål.Register -> FormålDTO.REGISTER
        Opplysningsformål.Regel -> FormålDTO.REGEL
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

private fun Regelsett.tilVurderingsresultatDTO(alleOpplysninger: List<Opplysning<*>>): VurderingsresultatV2DTO {
    // Vi ønsker kun å ta med produkter som faktisk har vært produsert i løpet av behandlingsskjeden
    val typer = alleOpplysninger.map { it.opplysningstype }.toSet()
    val produkter = produserer.filter { it in typer }

    return VurderingsresultatV2DTO(
        id = hjemmel.hashCode().toString(),
        navn = hjemmel.kortnavn,
        hjemmel =
            HjemmelDTO(
                kilde = LovkildeDTO(hjemmel.kilde.navn, hjemmel.kilde.kortnavn),
                kapittel = hjemmel.kapittel.toString(),
                paragraf = hjemmel.paragraf.toString(),
                tittel = hjemmel.toString(),
                url = hjemmel.url,
            ),
        relevantForResultat = påvirkerResultat(alleOpplysninger.somOpplysninger()),
        type =
            when (type) {
                RegelsettType.Vilkår -> RegelsettTypeDTO.VILKÅR
                RegelsettType.Fastsettelse -> RegelsettTypeDTO.FASTSETTELSE
            },
        // Litt rart navn. Dette er opplysningstypene som utgjør "utfallet" av et regelsett.
        opplysningTypeId = utfall?.id?.uuid,
        opplysninger = produkter.map { it.id.uuid },
    )
}

private fun Boolean.tilOpprinnelseDTO() =
    when (this) {
        true -> OpprinnelseDTO.NY
        false -> OpprinnelseDTO.ARVET
    }

internal fun Opplysning<*>.tilOpplysningsperiodeDTO(egneId: List<UUID>) =
    OpplysningsperiodeDTO(
        id = this.id,
        opprettet = opprettet,
        status = (id in egneId).tilOpprinnelseDTO(),
        opprinnelse = (id in egneId).tilOpprinnelseDTO(),
        gyldigFraOgMed = this.gyldighetsperiode.fraOgMed.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tilOgMed.tilApiDato(),
        verdi =
            when (this.opplysningstype.datatype) {
                BarnDatatype ->
                    with(this.verdi as BarnListe) {
                        BarnelisteDTO(
                            søknadBarnId = søknadbarnId,
                            verdi =
                                barn.map {
                                    BarnVerdiDTO(
                                        it.fødselsdato,
                                        it.fornavnOgMellomnavn,
                                        it.etternavn,
                                        it.statsborgerskap,
                                        it.kvalifiserer,
                                    )
                                },
                        )
                    }

                Boolsk -> BoolskVerdiDTO(this.verdi as Boolean)
                Dato -> DatoVerdiDTO(this.verdi as LocalDate)
                Desimaltall -> DesimaltallVerdiDTO(this.verdi as Double, this.opplysningstype.tilEnhetDTO())
                Heltall -> HeltallVerdiDTO(this.verdi as Int, this.opplysningstype.tilEnhetDTO())
                InntektDataType -> TekstVerdiDTO((this.verdi as Inntekt).verdi.inntektsId)
                Penger ->
                    PengeVerdiDTO(
                        verdi = (this.verdi as Beløp).verdien,
                    )

                PeriodeDataType ->
                    (this.verdi as Periode).let {
                        PeriodeVerdiDTO(it.fraOgMed, it.tilOgMed)
                    }

                Tekst, ULID -> TekstVerdiDTO(this.verdi.toString())
            },
        kilde =
            this.kilde?.let {
                val registrert = it.registrert
                when (it) {
                    is Saksbehandlerkilde ->
                        OpplysningskildeDTO(
                            OpplysningskildeDTOTypeDTO.SAKSBEHANDLER,
                            ident = it.saksbehandler.ident,
                            begrunnelse = it.begrunnelse?.let { BegrunnelseDTO(it.verdi, it.sistEndret) },
                            registrert = registrert,
                        )

                    is Systemkilde ->
                        OpplysningskildeDTO(
                            OpplysningskildeDTOTypeDTO.SYSTEM,
                            meldingId = it.meldingsreferanseId,
                            registrert = registrert,
                        )
                }
            },
        utledetAv =
            utledetAv?.let { utledning ->
                UtledningDTO(
                    regel = RegelDTO(navn = utledning.regel),
                    opplysninger = utledning.opplysninger.map { it.id },
                    versjon = utledning.versjon,
                )
            },
    )

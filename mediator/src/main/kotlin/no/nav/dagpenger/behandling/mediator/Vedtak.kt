package no.nav.dagpenger.behandling.mediator

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.dagpenger.behandling.api.models.BehandletAvDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTORolleDTO
import no.nav.dagpenger.behandling.api.models.FormålDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.PeriodeDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOFastsattDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOFastsattVanligArbeidstidDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOGjenståendeDTO
import no.nav.dagpenger.behandling.api.models.VilkaarDTO
import no.nav.dagpenger.behandling.api.models.VilkaarDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.VilkaarNavnDTO
import no.nav.dagpenger.behandling.mediator.api.redigerbareOpplysninger
import no.nav.dagpenger.behandling.mediator.api.tilApiDato
import no.nav.dagpenger.behandling.mediator.api.tilDataTypeDTO
import no.nav.dagpenger.behandling.mediator.api.tilOpplysningskildeDTO
import no.nav.dagpenger.behandling.mediator.api.tilOpplysningsverdiDTO
import no.nav.dagpenger.behandling.mediator.api.utbetalinger
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.OmgjøringId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.FulleYtelser
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Minsteinntekt.inntektFraSkatt
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.Permittering
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien
import no.nav.dagpenger.regel.ReellArbeidssøker
import no.nav.dagpenger.regel.RegistrertArbeidssøker
import no.nav.dagpenger.regel.Samordning
import no.nav.dagpenger.regel.StreikOgLockout
import no.nav.dagpenger.regel.Søknad
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.nyArbeidstid
import no.nav.dagpenger.regel.Uriktigeopplysninger
import no.nav.dagpenger.regel.Utdanning
import no.nav.dagpenger.regel.Utestengning
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import java.time.LocalDateTime

fun Behandling.VedtakOpplysninger.lagVedtakDTO(ident: Ident): VedtakDTO {
    require(!rettighetsperioder.last().harRett) { "VedtakDTO kan kun lages for avslag" }

// TODO: Det her må vi slutte med. Innholdet i vedtaktet må periodiseres
    val opplysningerSomGjelderPåPrøvingsdato = opplysningerPåVirkningsdato()
    val relevanteVilkår: List<Regelsett> = relevanteVilkår()
    val vilkår: List<VilkaarDTO> =
        relevanteVilkår
            .flatMap { regelsett ->
                regelsett.betingelser
                    .map {
                        it to regelsett.hjemmel
                    }
            }.toMap()
            .flatMap { (opplysningstype, hjemmel) ->
                opplysninger
                    .somListe()
                    .filter { it.opplysningstype == opplysningstype }
                    .filterIsInstance<Opplysning<Boolean>>()
                    .map { opplysning ->
                        opplysning.tilVilkårDTO(hjemmel.toString())
                    }
            }
    val fastsatt = vedtakFastsattDTO(opplysningerSomGjelderPåPrøvingsdato)
    val egneId = opplysninger.somListe(Egne).map { it.id }

    return VedtakDTO(
        behandlingId = behandlingId,
        basertPåBehandlinger = listOfNotNull(basertPåBehandling),
        basertPåBehandling = basertPåBehandling,
        behandletHendelse =
            HendelseDTO(
                id = behandlingAv.eksternId.id.toString(),
                datatype = behandlingAv.eksternId.datatype,
                type =
                    when (behandlingAv.eksternId) {
                        is MeldekortId -> HendelseDTOTypeDTO.MELDEKORT
                        is SøknadId -> HendelseDTOTypeDTO.SØKNAD
                        is ManuellId -> HendelseDTOTypeDTO.MANUELL
                        is OmgjøringId -> HendelseDTOTypeDTO.MANUELL
                    },
                skjedde = behandlingAv.skjedde,
            ),
        fagsakId = opplysningerSomGjelderPåPrøvingsdato.finnNullableOpplysning(fagsakIdOpplysningstype)?.verdi?.toString() ?: "0",
        automatisk = automatiskBehandlet,
        ident = ident.identifikator(),
        vedtakstidspunkt = LocalDateTime.now(),
        virkningsdato = virkningsdato,
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
        vilkår = vilkår,
        fastsatt = fastsatt,
        gjenstående = VedtakDTOGjenståendeDTO(),
        utbetalinger = opplysninger.utbetalinger(egneId),
        opplysninger = opplysningerSomGjelderPåPrøvingsdato.somListe().map { it.tilOpplysningDTO(opplysningerSomGjelderPåPrøvingsdato) },
    )
}

private fun Opplysning<*>.tilOpplysningDTO(opplysninger: LesbarOpplysninger): OpplysningDTO =
    OpplysningDTO(
        id = this.id,
        opplysningTypeId = this.opplysningstype.id.uuid,
        navn = this.opplysningstype.navn,
        verdi =
            when (this.opplysningstype.datatype) {
                // todo: Frontenden burde vite om det er penger og håndtere det med valuta
                Penger -> (this.verdi as Beløp).uavrundet.toString()

                else -> this.verdi.toString()
            },
        status =
            when (this) {
                is Faktum -> OpplysningDTOStatusDTO.FAKTUM
                is Hypotese -> OpplysningDTOStatusDTO.HYPOTESE
            },
        verdien = tilOpplysningsverdiDTO(),
        gyldigFraOgMed = this.gyldighetsperiode.fraOgMed.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tilOgMed.tilApiDato(),
        datatype = this.opplysningstype.datatype.tilDataTypeDTO(),
        kilde = tilOpplysningskildeDTO(),
        utledetAv =
            utledetAv?.let { utledning ->
                UtledningDTO(
                    regel = RegelDTO(navn = utledning.regel),
                    opplysninger = utledning.opplysninger.map { it.id },
                    // Tom med vilje, sannsynligvis ikke nyttig behandlingskontekst
                    versjon = null,
                )
            },
        redigerbar = this.kanRedigeres(redigerbareOpplysninger),
        kanOppfriskes = this.opplysningstype.kanOppfriskes(),
        synlig = this.opplysningstype.synlig(opplysninger),
        formål = opplysningstype.tilFormålDTO(),
    )

private fun Opplysningstype<*>.tilFormålDTO(): FormålDTO =
    when (formål) {
        Opplysningsformål.Legacy -> FormålDTO.LEGACY
        Opplysningsformål.Bruker -> FormålDTO.BRUKER
        Opplysningsformål.Register -> FormålDTO.REGISTER
        Opplysningsformål.Regel -> FormålDTO.REGEL
    }

private fun Opplysningstype<*>.kanOppfriskes(): Boolean =
    this in
        setOf(
            inntektFraSkatt,
            grunnbeløpForDagpengeGrunnlag,
        )

private fun vedtakFastsattDTO(opplysninger: LesbarOpplysninger) =
    VedtakDTOFastsattDTO(
        utfall = false,
        fastsattVanligArbeidstid =
            opplysninger.har(fastsattVanligArbeidstid).takeIf { it }?.let {
                VedtakDTOFastsattVanligArbeidstidDTO(
                    vanligArbeidstidPerUke = opplysninger.finnOpplysning(fastsattVanligArbeidstid).verdi.toBigDecimal(),
                    nyArbeidstidPerUke = opplysninger.finnOpplysning(nyArbeidstid).verdi.toBigDecimal(),
                )
            },
        samordning = emptyList(),
    )

private fun Opplysning<Boolean>.tilVilkårDTO(hjemmel: String?): VilkaarDTO =
    VilkaarDTO(
        navn = this.opplysningstype.tilVilkårNavn(),
        hjemmel = hjemmel ?: "Mangler mapping til hjemmel",
        status =
            when (this.verdi) {
                true -> VilkaarDTOStatusDTO.OPPFYLT
                else -> VilkaarDTOStatusDTO.IKKE_OPPFYLT
            },
        vurderingstidspunkt = this.opprettet,
        periode =
            PeriodeDTO(
                fraOgMed = this.gyldighetsperiode.fraOgMed,
                tilOgMed = this.gyldighetsperiode.tilOgMed.tilApiDato(),
            ),
    )

internal val opplysningTilVilkårMap =
    mapOf(
        Alderskrav.kravTilAlder to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_ALDER,
        FulleYtelser.ikkeFulleYtelser to VilkaarNavnDTO.MOTTAR_IKKE_ANDRE_FULLE_YTELSER,
        Minsteinntekt.minsteinntekt to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_MINSTEINNTEKT,
        Opphold.oppfyllerKravetTilOpphold to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_OPPHOLD_I_NORGE_ELLER_UNNTAK,
        Opphold.oppfyllerMedlemskap to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_MEDLEMSKAP,
        Opphold.oppfyllerKravet to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_OPPHOLD_I_NORGE,
        ReellArbeidssøker.oppfyllerKravTilArbeidssøker to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_HELTID__OG_DELTIDSARBEID,
        ReellArbeidssøker.oppfyllerKravTilMobilitet to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_MOBILITET,
        ReellArbeidssøker.oppfyllerKravTilArbeidsfør to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_Å_VÆRE_ARBEIDSFØR,
        ReellArbeidssøker.oppfyllerKravetTilEthvertArbeid to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_Å_TA_ETHVERT_ARBEID,
        ReellArbeidssøker.kravTilArbeidssøker to VilkaarNavnDTO.KRAV_TIL_ARBEIDSSØKER,
        RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker to VilkaarNavnDTO.REGISTRERT_SOM_ARBEIDSSØKER_PÅ_SØKNADSTIDSPUNKTET,
        StreikOgLockout.ikkeStreikEllerLockout to VilkaarNavnDTO.ER_MEDLEMMET_IKKE_PÅVIRKET_AV_STREIK_ELLER_LOCK_OUT_,
        TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntekt to VilkaarNavnDTO.KRAV_TIL_TAP_AV_ARBEIDSINNTEKT,
        TapAvArbeidsinntektOgArbeidstid.kravTilTaptArbeidstid to VilkaarNavnDTO.TAP_AV_ARBEIDSTID_ER_MINST_TERSKEL,
        TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntektOgArbeidstid to
            VilkaarNavnDTO.KRAV_TIL_TAP_AV_ARBEIDSINNTEKT_OG_ARBEIDSTID,
        Utdanning.kravTilUtdanning to VilkaarNavnDTO.KRAV_TIL_UTDANNING_ELLER_OPPLÆRING,
        Utestengning.oppfyllerKravetTilIkkeUtestengt to VilkaarNavnDTO.OPPFYLLER_KRAV_TIL_IKKE_UTESTENGT,
        Permittering.oppfyllerKravetTilPermittering to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_PERMITTERING,
        Verneplikt.oppfyllerKravetTilVerneplikt to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_VERNEPLIKT,
        PermitteringFraFiskeindustrien.oppfyllerKravetTilPermitteringFiskeindustri to
            VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_PERMITTERING_I_FISKEINDUSTRIEN,
        Samordning.utfallEtterSamordning to VilkaarNavnDTO.UTFALL_ETTER_SAMORDNING,
        Uriktigeopplysninger.oppfyllerVilkårManglendeEllerUriktigeOpplysninger to
            VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_IKKE_GI_MANGELFULL_INFORMASJON,
        Søknad.oppfyllerKravetTilSøknad to VilkaarNavnDTO.OPPFYLLER_KRAVET_TIL_FRAMSATT_SØKNAD,
    )

private fun Opplysningstype<*>.tilVilkårNavn() = opplysningTilVilkårMap[this] ?: error("Mangler mapping for vilkårnavn $this")

fun toMap(it: Any) = objectMapper.convertValue<Map<String, Any>>(it)

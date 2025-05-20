package no.nav.dagpenger.behandling.mediator

import com.fasterxml.jackson.module.kotlin.convertValue
import mu.KotlinLogging
import no.nav.dagpenger.behandling.api.models.BarnDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTORolleDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.KvoteDTO
import no.nav.dagpenger.behandling.api.models.KvoteDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.SamordningDTO
import no.nav.dagpenger.behandling.api.models.UtbetalingDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOFastsattDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOFastsattVanligArbeidstidDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOGjenståendeDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOGrunnlagDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOSatsDTO
import no.nav.dagpenger.behandling.api.models.VilkaarDTO
import no.nav.dagpenger.behandling.api.models.VilkaarDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.VilkaarNavnDTO
import no.nav.dagpenger.behandling.mediator.api.tilOpplysningDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.FulleYtelser
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.Permittering
import no.nav.dagpenger.regel.Permittering.oppfyllerKravetTilPermittering
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.oppfyllerKravetTilPermitteringFiskeindustri
import no.nav.dagpenger.regel.ReellArbeidssøker
import no.nav.dagpenger.regel.RegistrertArbeidssøker
import no.nav.dagpenger.regel.Samordning
import no.nav.dagpenger.regel.StreikOgLockout
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.nyArbeidstid
import no.nav.dagpenger.regel.Utdanning
import no.nav.dagpenger.regel.Utestengning
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.barn
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.PermitteringFastsetting.permitteringsperiode
import no.nav.dagpenger.regel.fastsetting.PermitteringFraFiskeindustrienFastsetting.permitteringFraFiskeindustriPeriode
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.vernepliktPeriode
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import java.time.LocalDateTime

private fun LesbarOpplysninger.samordninger(): List<SamordningDTO> {
    @Suppress("UNCHECKED_CAST")
    val ytelser: List<Opplysning<Beløp>> =
        (
            finnAlle(
                listOf(
                    Samordning.sykepengerDagsats,
                    Samordning.pleiepengerDagsats,
                    Samordning.omsorgspengerDagsats,
                    Samordning.opplæringspengerDagsats,
                    Samordning.uføreDagsats,
                    Samordning.foreldrepengerDagsats,
                    Samordning.svangerskapspengerDagsats,
                    SamordingUtenforFolketrygden.pensjonFraOffentligTjenestepensjonsordningBeløp,
                    SamordingUtenforFolketrygden.redusertUførepensjonBeløp,
                    SamordingUtenforFolketrygden.vartpengerBeløp,
                    SamordingUtenforFolketrygden.ventelønnBeløp,
                    SamordingUtenforFolketrygden.etterlønnBeløp,
                    SamordingUtenforFolketrygden.garantilottGFFBeløp,
                ),
            ) as List<Opplysning<Beløp>>
        ).filterNot {
            it.verdi == Beløp(0.0)
        }

    return ytelser.map {
        SamordningDTO(
            type = it.opplysningstype.navn,
            beløp = it.verdi.verdien,
            // TODO: Vi må mappe til riktig grad
            grad = 50.toBigDecimal(),
        )
    }
}

private val logger = KotlinLogging.logger { }

fun Behandling.VedtakOpplysninger.lagVedtakDTO(ident: Ident): VedtakDTO {
    // TODO: Det her må vi slutte med. Innholdet i vedtaktet må periodiseres
    val opplysningerSomGjelderPåPrøvingsdato = opplysningerPåVirkningsdato().utenErstattet
    val relevanteVilkår: List<Regelsett> = relevanteVilkår()
    val vilkår: List<VilkaarDTO> =
        relevanteVilkår
            .flatMap { regelsett ->
                regelsett.utfall
                    .map {
                        it to regelsett.hjemmel
                    }
            }.toMap()
            .map {
                val opplysning = opplysningerSomGjelderPåPrøvingsdato.finnOpplysning(it.key)
                opplysning.tilVilkårDTO(it.value.toString())
            }

    logger.info {
        "VedtakDTO med utfall $utfall, dette var alle vilkårene:\n${vilkår.joinToString("\n") { it.navn.value + " -> " + it.status.name }}"
    }
    val fastsatt = vedtakFastsattDTO(utfall, opplysningerSomGjelderPåPrøvingsdato)
    return VedtakDTO(
        behandlingId = behandlingId,
        basertPåBehandlinger = basertPåBehandlinger,
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
            ),
        fagsakId = opplysningerSomGjelderPåPrøvingsdato.finnOpplysning(fagsakIdOpplysningstype).verdi.toString(),
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
        utbetalinger = opplysninger.utbetalinger(),
        opplysninger = opplysningerSomGjelderPåPrøvingsdato.finnAlle().map { it.tilOpplysningDTO(opplysningerSomGjelderPåPrøvingsdato) },
    )
}

private fun LesbarOpplysninger.utbetalinger(): List<UtbetalingDTO> {
    val meldeperioder: List<Opplysning<*>> = finnAlle(listOf(Beregning.meldeperiode))

    return finnAlle(listOf(Beregning.utbetaling)).filterIsInstance<Opplysning<Int>>().map { dag ->
        val sats = finnOpplysning(dagsatsEtterSamordningMedBarnetillegg).verdi
        val periode: Opplysning<*> = meldeperioder.first { it.gyldighetsperiode.overlapp(dag.gyldighetsperiode) }

        UtbetalingDTO(
            meldeperiode = periode.verdi.hashCode().toString(),
            dato = dag.gyldighetsperiode.fom,
            sats = sats.verdien.toInt(),
            utbetaling = dag.verdi,
        )
    }
}

private fun vedtakFastsattDTO(
    utfall: Boolean,
    opplysninger: LesbarOpplysninger,
) = when (utfall) {
    true ->
        VedtakDTOFastsattDTO(
            utfall = true,
            grunnlag =
                VedtakDTOGrunnlagDTO(
                    opplysninger
                        .finnOpplysning(Dagpengegrunnlag.grunnlag)
                        .verdi.verdien
                        .toInt(),
                ),
            fastsattVanligArbeidstid =
                VedtakDTOFastsattVanligArbeidstidDTO(
                    vanligArbeidstidPerUke = opplysninger.finnOpplysning(fastsattVanligArbeidstid).verdi.toBigDecimal(),
                    nyArbeidstidPerUke = opplysninger.finnOpplysning(nyArbeidstid).verdi.toBigDecimal(),
                ),
            sats =
                VedtakDTOSatsDTO(
                    dagsatsMedBarnetillegg =
                        opplysninger
                            .finnOpplysning(dagsatsEtterSamordningMedBarnetillegg)
                            .verdi.verdien
                            .toInt(),
                    barn =
                        opplysninger.finnOpplysning(barn).verdi.map {
                            BarnDTO(it.fødselsdato, it.kvalifiserer)
                        },
                ),
            samordning = opplysninger.samordninger(),
            kvoter =
                listOfNotNull(
                    opplysninger.finnOpplysning(minsteinntekt).takeIf { it.verdi }?.let {
                        KvoteDTO(
                            "Dagpengeperiode",
                            KvoteDTOTypeDTO.UKER,
                            opplysninger.finnOpplysning(Dagpengeperiode.ordinærPeriode).verdi.toBigDecimal(),
                        )
                    },
                    runCatching { opplysninger.finnOpplysning(grunnlagForVernepliktErGunstigst) }
                        .getOrNull()
                        .takeIf { it?.verdi == true }
                        ?.let {
                            KvoteDTO(
                                "Verneplikt",
                                KvoteDTOTypeDTO.UKER,
                                opplysninger.finnOpplysning(vernepliktPeriode).verdi.toBigDecimal(),
                            )
                        },
                    KvoteDTO(
                        "Egenandel",
                        KvoteDTOTypeDTO.BELØP,
                        opplysninger.finnOpplysning(Egenandel.egenandel).verdi.verdien,
                    ),
                    runCatching { opplysninger.finnOpplysning(oppfyllerKravetTilPermittering) }
                        .getOrNull()
                        .takeIf { it?.verdi == true }
                        ?.let {
                            KvoteDTO(
                                "Permitteringsperiode",
                                KvoteDTOTypeDTO.UKER,
                                opplysninger.finnOpplysning(permitteringsperiode).verdi.toBigDecimal(),
                            )
                        },
                    runCatching { opplysninger.finnOpplysning(oppfyllerKravetTilPermitteringFiskeindustri) }
                        .getOrNull()
                        .takeIf { it?.verdi == true }
                        ?.let {
                            KvoteDTO(
                                "FiskePermitteringsperiode",
                                KvoteDTOTypeDTO.UKER,
                                opplysninger.finnOpplysning(permitteringFraFiskeindustriPeriode).verdi.toBigDecimal(),
                            )
                        },
                ),
        )

    false ->
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
}

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
        id = this.opplysningstype.id.uuid,
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
    )

private fun Opplysningstype<*>.tilVilkårNavn() = opplysningTilVilkårMap[this] ?: error("Mangler mapping for vilkårnavn $this")

fun VedtakDTO.toMap() = objectMapper.convertValue<Map<String, Any>>(this)

package no.nav.dagpenger.behandling.mediator

import com.fasterxml.jackson.module.kotlin.convertValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.api.models.BarnDTO
import no.nav.dagpenger.behandling.api.models.BarnVerdiDTO
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BegrunnelseDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTORolleDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.FormålDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.KvoteDTO
import no.nav.dagpenger.behandling.api.models.KvoteDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.SamordningDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.behandling.api.models.UtbetalingDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOFastsattDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOFastsattVanligArbeidstidDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOGjenståendeDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOGrunnlagDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTOSatsDTO
import no.nav.dagpenger.behandling.api.models.VilkaarDTO
import no.nav.dagpenger.behandling.api.models.VilkaarDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.VilkaarNavnDTO
import no.nav.dagpenger.behandling.mediator.api.redigerbareOpplysninger
import no.nav.dagpenger.behandling.mediator.api.tilApiDato
import no.nav.dagpenger.behandling.mediator.api.tilDataTypeDTO
import no.nav.dagpenger.behandling.mediator.api.tilEnhetDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.FulleYtelser
import no.nav.dagpenger.regel.KravPåDagpenger
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Minsteinntekt.inntektFraSkatt
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
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
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
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.contains
import kotlin.collections.map

private fun LesbarOpplysninger.samordninger(): List<SamordningDTO> {
    val ytelser: List<Opplysning<Beløp>> =
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

    val fastsatt = vedtakFastsattDTO(rettighetsperioder.last().harRett, opplysningerSomGjelderPåPrøvingsdato)
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
        utbetalinger = opplysninger.utbetalinger(),
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
        verdien =
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
        gyldigFraOgMed = this.gyldighetsperiode.fraOgMed.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tilOgMed.tilApiDato(),
        datatype = this.opplysningstype.datatype.tilDataTypeDTO(),
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

private fun LesbarOpplysninger.utbetalinger(): List<UtbetalingDTO> {
    val meldeperioder = finnAlle(Beregning.meldeperiode)

    val løpendeRett = finnAlle(KravPåDagpenger.harLøpendeRett)
    val satser = finnAlle(dagsatsEtterSamordningMedBarnetillegg)
    val dager = finnAlle(Beregning.utbetaling).associateBy { it.gyldighetsperiode.fraOgMed }

    return meldeperioder.flatMap { periode ->
        periode.verdi.mapNotNull { dato ->
            if (løpendeRett.none { it.gyldighetsperiode.inneholder(dato) }) {
                // Har ikke løpende rett i denne perioden, så ingen utbetaling
                return@mapNotNull null
            }

            val dag = dager[dato] ?: throw IllegalStateException("Mangler utbetaling for dag $dato")
            val sats = satser.first { it.gyldighetsperiode.inneholder(dato) }.verdi
            UtbetalingDTO(
                periode.verdi.hashCode().toString(),
                dato = dato,
                sats = sats.verdien.toInt(),
                utbetaling = dag.verdi.heleKroner.toInt(),
            )
        }
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
                        opplysninger.finnOpplysning(barn).verdi.barn.map {
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
    )

private fun Opplysningstype<*>.tilVilkårNavn() = opplysningTilVilkårMap[this] ?: error("Mangler mapping for vilkårnavn $this")

fun toMap(it: Any) = objectMapper.convertValue<Map<String, Any>>(it)

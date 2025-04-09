package no.nav.dagpenger.behandling.mediator

import com.fasterxml.jackson.module.kotlin.convertValue
import mu.KotlinLogging
import no.nav.dagpenger.behandling.api.models.BarnDTO
import no.nav.dagpenger.behandling.api.models.BehandletAvDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.KvoteDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.SamordningDTO
import no.nav.dagpenger.behandling.api.models.UtbetalingDTO
import no.nav.dagpenger.behandling.api.models.VedtakDTO
import no.nav.dagpenger.behandling.api.models.VedtakFastsattDTO
import no.nav.dagpenger.behandling.api.models.VedtakFastsattFastsattVanligArbeidstidDTO
import no.nav.dagpenger.behandling.api.models.VedtakFastsattGrunnlagDTO
import no.nav.dagpenger.behandling.api.models.VedtakFastsattSatsDTO
import no.nav.dagpenger.behandling.api.models.VedtakGjenstEndeDTO
import no.nav.dagpenger.behandling.api.models.VilkaarDTO
import no.nav.dagpenger.behandling.mediator.api.tilOpplysningDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.Permittering.oppfyllerKravetTilPermittering
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.oppfyllerKravetTilPermitteringFiskeindustri
import no.nav.dagpenger.regel.Samordning
import no.nav.dagpenger.regel.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.nyArbeidstid
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
    val opplysningerSomGjelderPåPrøvingsdato = opplysningerPåVirkningsdato()
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
        "VedtakDTO med utfall $utfall, dette var alle vilkårene:\n${vilkår.joinToString("\n") { it.navn + " -> " + it.status.name }}"
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
                        is MeldekortId -> HendelseDTO.Type.Meldekort
                        is SøknadId -> HendelseDTO.Type.Søknad
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
                        BehandletAvDTO.Rolle.saksbehandler,
                        SaksbehandlerDTO(it.utførtAv.ident),
                    )
                },
                besluttetAv.takeIf { it.erUtført }?.let {
                    BehandletAvDTO(
                        BehandletAvDTO.Rolle.beslutter,
                        SaksbehandlerDTO(it.utførtAv.ident),
                    )
                },
            ),
        vilkår = vilkår,
        fastsatt = fastsatt,
        gjenstående = VedtakGjenstEndeDTO(),
        utbetalinger = opplysninger.utbetalinger(),
        opplysninger = opplysningerSomGjelderPåPrøvingsdato.finnAlle().map { it.tilOpplysningDTO(opplysningerSomGjelderPåPrøvingsdato) },
    )
}

private fun LesbarOpplysninger.utbetalinger() =
    finnAlle().filter { it.er(Beregning.utbetaling) }.filterIsInstance<Opplysning<Int>>().map {
        val sats = finnOpplysning(dagsatsEtterSamordningMedBarnetillegg).verdi
        UtbetalingDTO(
            dato = it.gyldighetsperiode.fom,
            sats = sats.verdien.toInt(),
            utbetaling = it.verdi,
        )
    }

private fun vedtakFastsattDTO(
    utfall: Boolean,
    opplysninger: LesbarOpplysninger,
) = when (utfall) {
    true ->
        VedtakFastsattDTO(
            utfall = true,
            grunnlag =
                VedtakFastsattGrunnlagDTO(
                    opplysninger
                        .finnOpplysning(Dagpengegrunnlag.grunnlag)
                        .verdi.verdien
                        .toInt(),
                ),
            fastsattVanligArbeidstid =
                VedtakFastsattFastsattVanligArbeidstidDTO(
                    vanligArbeidstidPerUke = opplysninger.finnOpplysning(fastsattVanligArbeidstid).verdi.toBigDecimal(),
                    nyArbeidstidPerUke = opplysninger.finnOpplysning(nyArbeidstid).verdi.toBigDecimal(),
                ),
            sats =
                VedtakFastsattSatsDTO(
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
                            KvoteDTO.Type.uker,
                            opplysninger.finnOpplysning(Dagpengeperiode.ordinærPeriode).verdi.toBigDecimal(),
                        )
                    },
                    runCatching { opplysninger.finnOpplysning(grunnlagForVernepliktErGunstigst) }
                        .getOrNull()
                        .takeIf { it?.verdi == true }
                        ?.let {
                            KvoteDTO(
                                "Verneplikt",
                                KvoteDTO.Type.uker,
                                opplysninger.finnOpplysning(vernepliktPeriode).verdi.toBigDecimal(),
                            )
                        },
                    KvoteDTO(
                        "Egenandel",
                        KvoteDTO.Type.beløp,
                        opplysninger.finnOpplysning(Egenandel.egenandel).verdi.verdien,
                    ),
                    runCatching { opplysninger.finnOpplysning(oppfyllerKravetTilPermittering) }
                        .getOrNull()
                        .takeIf { it?.verdi == true }
                        ?.let {
                            KvoteDTO(
                                "Permitteringsperiode",
                                KvoteDTO.Type.uker,
                                opplysninger.finnOpplysning(permitteringsperiode).verdi.toBigDecimal(),
                            )
                        },
                    runCatching { opplysninger.finnOpplysning(oppfyllerKravetTilPermitteringFiskeindustri) }
                        .getOrNull()
                        .takeIf { it?.verdi == true }
                        ?.let {
                            KvoteDTO(
                                "FiskePermitteringsperiode",
                                KvoteDTO.Type.uker,
                                opplysninger.finnOpplysning(permitteringFraFiskeindustriPeriode).verdi.toBigDecimal(),
                            )
                        },
                ),
        )

    false ->
        VedtakFastsattDTO(
            utfall = false,
            fastsattVanligArbeidstid =
                opplysninger.har(fastsattVanligArbeidstid).takeIf { it }?.let {
                    VedtakFastsattFastsattVanligArbeidstidDTO(
                        vanligArbeidstidPerUke = opplysninger.finnOpplysning(fastsattVanligArbeidstid).verdi.toBigDecimal(),
                        nyArbeidstidPerUke = opplysninger.finnOpplysning(nyArbeidstid).verdi.toBigDecimal(),
                    )
                },
            samordning = emptyList(),
        )
}

private fun Opplysning<Boolean>.tilVilkårDTO(hjemmel: String?): VilkaarDTO =
    VilkaarDTO(
        navn = this.opplysningstype.toString(),
        hjemmel = hjemmel ?: "Mangler mapping til hjemmel",
        status =
            when (this.verdi) {
                true -> VilkaarDTO.Status.Oppfylt
                else -> VilkaarDTO.Status.IkkeOppfylt
            },
        vurderingstidspunkt = this.opprettet,
    )

fun VedtakDTO.toMap() = objectMapper.convertValue<Map<String, Any>>(this)

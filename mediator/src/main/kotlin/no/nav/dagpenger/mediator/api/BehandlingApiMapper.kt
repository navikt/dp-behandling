package no.nav.dagpenger.mediator.api

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.ferietillegg.FerietilleggBeløp.sumUtbetaltForÅr
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg.antallDagerForbruk
import no.nav.dagpenger.konfigurasjon.Feature
import no.nav.dagpenger.konfigurasjon.unleash
import no.nav.dagpenger.mediator.api.models.BehandlingDTO
import no.nav.dagpenger.mediator.api.models.BehandlingTilstandDTO
import no.nav.dagpenger.mediator.api.models.FormålDTO
import no.nav.dagpenger.mediator.api.models.HjemmelDTO
import no.nav.dagpenger.mediator.api.models.LovkildeDTO
import no.nav.dagpenger.mediator.api.models.OpplysningerDTO
import no.nav.dagpenger.mediator.api.models.RedigerbareOpplysningerDTO
import no.nav.dagpenger.mediator.api.models.RegelsettDTO
import no.nav.dagpenger.mediator.api.models.RegelsettTypeDTO
import no.nav.dagpenger.mediator.api.models.SaksbehandlersVurderingerDTO
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Redigerbar
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.antallBarn
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.barn
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.barnetilleggetsStørrelse
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage.ansesUgyldigVedtak
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage.endringIkkeTilSkade
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage.ikkeUnderretning
import no.nav.dagpenger.regel.regelsett.prosessvilkår.Uriktigeopplysninger.holderTilbake
import no.nav.dagpenger.regel.regelsett.prosessvilkår.Uriktigeopplysninger.unnlateråEtterkommePålegg
import no.nav.dagpenger.regel.regelsett.prosessvilkår.Uriktigeopplysninger.uriktigeOpplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport.antallDagerFristForRegistrering
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport.registrertIVertsland
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport.skalHaEksport
import no.nav.dagpenger.regel.regelsett.vilkår.FulleYtelser.ikkeFulleYtelser
import no.nav.dagpenger.regel.regelsett.vilkår.Gjenopptak.oppholdMedArbeidI12ukerEllerMer
import no.nav.dagpenger.regel.regelsett.vilkår.Meldeplikt.oppfyllerMeldeplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.inntektFraSkatt
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold.medlemFolketrygden
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold.oppholdINorge
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold.unntakForOpphold
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering.erPermitteringenMidlertidig
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering.godkjentPermitteringsårsak
import no.nav.dagpenger.regel.regelsett.vilkår.PermitteringFraFiskeindustrien.erPermitteringenFraFiskeindustriMidlertidig
import no.nav.dagpenger.regel.regelsett.vilkår.PermitteringFraFiskeindustrien.godkjentÅrsakPermitteringFraFiskindustri
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.erArbeidsfør
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.godkjentArbeidsufør
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.godkjentDeltidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.godkjentLokalArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.kanJobbeDeltid
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.kanJobbeHvorSomHelst
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.minimumVanligArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.villigTilEthvertArbeid
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.ønsketArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.erPermittert
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.kravetReellArbeidsøkerSkalVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.permitteringFiskeforedling
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalEksportVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalGjenopptakVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalVernepliktVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.foreldrepenger
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.foreldrepengerDagsats
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.omsorgspenger
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.omsorgspengerDagsats
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.opplæringspenger
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.opplæringspengerDagsats
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.pleiepenger
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.pleiepengerDagsats
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.samordnetArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.skalUføreSamordnes
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.svangerskapspenger
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.svangerskapspengerDagsats
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.sykepenger
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.sykepengerDagsats
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.uføre
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.uføreDagsats
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode.antallSanksjonsuker
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode.harSanksjon
import no.nav.dagpenger.regel.regelsett.vilkår.StreikOgLockout.deltarIStreikOgLockout
import no.nav.dagpenger.regel.regelsett.vilkår.StreikOgLockout.sammeBedriftOgPåvirket
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.ønsketdato
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.arbeidstidsreduksjonIkkeBruktTidligere
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregningsregel12mnd
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregningsregel36mnd
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregningsregel6mnd
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.kravPåLønn
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.nyArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall.antallBortfallsuker
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall.harTidsbegrensetBortfall
import no.nav.dagpenger.regel.regelsett.vilkår.TreMeldePerioderUtentilstrekkeligTapAvArbeidstid.trePåfølgendePerioderUtenTilstrekkeligTap
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.deltakelseIArbeidsmarkedstiltak
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.deltakelsePåKurs
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.grunnskoleopplæring
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.høyereUtdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.høyereYrkesfagligUtdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.opplæringForInnvandrere
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.tarUtdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utestengning.utestengt
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.oppfyllerKravetTilVerneplikt
import java.time.LocalDateTime
import kotlin.io.encoding.Base64

internal fun Behandling.tilBehandlingDTO(): BehandlingDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val opplysningSet = opplysninger.somListe()
        val egneId = opplysninger.somListe(Egne).map { it.id }
        val behandlingsresultat = vedtakopplysninger

        BehandlingDTO(
            behandlingId = behandlingId,
            ident = behandler.ident,
            automatisk = behandlingsresultat.automatiskBehandlet,
            basertPå = behandlingsresultat.basertPåBehandling,
            regelverk = regelverk.navn,
            behandlingskjedeId = behandlingskjedeId,
            behandletHendelse = behandler.tilHendelseDTO(),
            rettighetsperioder = behandlingsresultat.rettighetsperioder(),
            kreverTotrinnskontroll = this.kreverTotrinnskontroll(),
            tilstand = tilstand().tilTilstandDTO(),
            avklaringer = this.avklaringer().map { it.tilAvklaringDTO() },
            vilkår =
                behandler.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Vilkår)
                    .map { it.tilVurderingsresultatDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            fastsettelser =
                behandler.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Fastsettelse)
                    .map { it.tilVurderingsresultatDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() } +
                    // TODO: Fjerne prosessregler fra fastsettelsesreglene når de er tatt i bruk (saksbehandlingsregler) i frontend
                    behandler.forretningsprosess.regelverk
                        .regelsettAvType(RegelsettType.Prosess)
                        .map { it.tilVurderingsresultatDTO(opplysningSet) }
                        .sortedBy { it.hjemmel.paragraf.toInt() },
            opplysninger =
                opplysningSet.somOpplysningperiode({ type, opplysninger ->
                    RedigerbareOpplysningerDTO(
                        opplysningTypeId = type.id.uuid,
                        navn = type.navn,
                        perioder = opplysninger.map { opplysning -> opplysning.tilOpplysningsperiodeDTO(egneId) },
                        datatype = type.datatype.tilDataTypeDTO(),
                        synlig = type.synlig(this.opplysninger),
                        redigerbar = opplysninger.last().kanRedigeres(redigerbareOpplysninger),
                        redigertAvSaksbehandler = opplysninger.last().kilde is Saksbehandlerkilde,
                        kanOppfriskes = type.kanOppfriskes(opplysninger.any { it.id in egneId }),
                        formål = type.tilFormålDTO(),
                    )
                }),
            saksbehandlingsregler =
                behandler.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Prosess)
                    .map { it.tilVurderingsresultatDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            forslagOm = vedtakopplysninger.avgjørelse.tilAvgjørelseDTO(),
            opprettet = opprettet,
            sistEndret = sistEndret,
        )
    }

private val kanOppfriskes =
    setOf(
        inntektFraSkatt,
        grunnbeløpForDagpengeGrunnlag,
    )

private fun Opplysningstype<*>.kanOppfriskes(finnesIEgne: Boolean): Boolean = this in kanOppfriskes && finnesIEgne

private fun Pair<Behandling.TilstandType, LocalDateTime>.tilTilstandDTO() =
    when (first) {
        Behandling.TilstandType.UnderOpprettelse -> BehandlingTilstandDTO.UNDER_OPPRETTELSE
        Behandling.TilstandType.UnderBehandling -> BehandlingTilstandDTO.UNDER_BEHANDLING
        Behandling.TilstandType.ForslagTilVedtak -> BehandlingTilstandDTO.FORSLAG_TIL_VEDTAK
        Behandling.TilstandType.Låst -> BehandlingTilstandDTO.LÅST
        Behandling.TilstandType.Avbrutt -> BehandlingTilstandDTO.AVBRUTT
        Behandling.TilstandType.Ferdig -> BehandlingTilstandDTO.FERDIG
        Behandling.TilstandType.Redigert -> BehandlingTilstandDTO.REDIGERT
        Behandling.TilstandType.TilGodkjenning -> BehandlingTilstandDTO.TIL_GODKJENNING
        Behandling.TilstandType.TilBeslutning -> BehandlingTilstandDTO.TIL_BESLUTNING
    }

internal fun Behandling.tilSaksbehandlersVurderinger() =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val endredeOpplysninger = opplysninger().somListe().filter { it.kilde is Saksbehandlerkilde }
        val egneId = opplysninger.somListe(Egne).map { it.id }

        SaksbehandlersVurderingerDTO(
            behandlingId = behandlingId,
            opplysninger =
                endredeOpplysninger.groupBy { it.opplysningstype }.map { (type, opplysninger) ->
                    OpplysningerDTO(
                        opplysningTypeId = type.id.uuid,
                        navn = type.navn,
                        datatype = type.datatype.tilDataTypeDTO(),
                        perioder = opplysninger.map { opplysning -> opplysning.tilOpplysningsperiodeDTO(egneId) },
                    )
                },
        )
    }

private fun Regelsett.tilVurderingsresultatDTO(alleOpplysninger: List<Opplysning<*>>): RegelsettDTO {
    // Vi ønsker kun å ta med produkter som faktisk har vært produsert i løpet av behandlingsskjeden
    val typer = alleOpplysninger.map { it.opplysningstype }.toSet()
    val produkter = produserer.filter { it in typer }

    return RegelsettDTO(
        id = Base64.UrlSafe.encode(hjemmel.hashCode().toString().encodeToByteArray()),
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
                RegelsettType.Prosess -> RegelsettTypeDTO.PROSESS
            },
        // Litt rart navn. Dette er opplysningstypene som utgjør "utfallet" av et regelsett.
        opplysningTypeId = utfall?.id?.uuid,
        opplysninger = produkter.map { it.id.uuid },
    )
}

private fun Opplysningstype<*>.tilFormålDTO(): FormålDTO =
    when (formål) {
        Opplysningsformål.Legacy -> FormålDTO.LEGACY
        Opplysningsformål.Bruker -> FormålDTO.BRUKER
        Opplysningsformål.Register -> FormålDTO.REGISTER
        Opplysningsformål.Regel -> FormålDTO.REGEL
    }

// TODO: Denne bor nok et annet sted - men bare for å vise at det er mulig å ha en slik funksjon
internal val redigerbareOpplysninger =
    object : Redigerbar {
        private val redigerbare
            get() =
                buildSet {
                    addAll(
                        listOf(
                            // Det skal ikke være nødvendig å la saksbehandler manipulere lenger. Som en siste utvei kan vi legge den til igjen ved behov
                            // harLøpendeRett,
                            prøvingsdato,
                            ønsketdato,
                            // 4-2 Opphold
                            oppholdINorge,
                            unntakForOpphold,
                            medlemFolketrygden,
                            // 4-3
                            kravPåLønn,
                            kravTilArbeidstidsreduksjon,
                            beregningsregel6mnd,
                            beregningsregel12mnd,
                            beregningsregel36mnd,
                            beregnetArbeidstid,
                            nyArbeidstid,
                            // forskrift 4-10 Tre påfølgende meldeperioder uten tilstrekkelig tap av arbeidstid
                            trePåfølgendePerioderUtenTilstrekkeligTap,
                            // 4-5
                            ønsketArbeidstid,
                            minimumVanligArbeidstid,
                            kanJobbeDeltid,
                            kanJobbeHvorSomHelst,
                            erArbeidsfør,
                            villigTilEthvertArbeid,
                            godkjentDeltidssøker,
                            godkjentLokalArbeidssøker,
                            godkjentArbeidsufør,
                            kravetReellArbeidsøkerSkalVurderes,
                            // 4-6 Utdanning
                            tarUtdanning,
                            deltakelseIArbeidsmarkedstiltak,
                            opplæringForInnvandrere,
                            grunnskoleopplæring,
                            høyereYrkesfagligUtdanning,
                            høyereUtdanning,
                            deltakelsePåKurs,
                            // 4-7 Permittering
                            erPermittert,
                            godkjentPermitteringsårsak,
                            erPermitteringenMidlertidig,
                            permitteringFiskeforedling,
                            erPermitteringenFraFiskeindustriMidlertidig,
                            godkjentÅrsakPermitteringFraFiskindustri,
                            // 4-8 Meldeplikt og møteplikt
                            oppfyllerMeldeplikt,
                            // 4-10 Sanksjon
                            harSanksjon,
                            antallSanksjonsuker,
                            // 4-19 Verneplikt
                            oppfyllerKravetTilVerneplikt,
                            skalVernepliktVurderes,
                            // 4-20 Tidsbegrenset
                            harTidsbegrensetBortfall,
                            antallBortfallsuker,
                            // 4-22 Streik og lockout
                            deltarIStreikOgLockout,
                            sammeBedriftOgPåvirket,
                            // 4-23 Krav til alder
                            kravTilAlder,
                            // 4-24 Fulle ytelser
                            ikkeFulleYtelser,
                            // 4-25 Samordning
                            samordnetArbeidstid,
                            sykepenger,
                            pleiepenger,
                            omsorgspenger,
                            opplæringspenger,
                            uføre,
                            foreldrepenger,
                            svangerskapspenger,
                            sykepengerDagsats,
                            pleiepengerDagsats,
                            omsorgspengerDagsats,
                            opplæringspengerDagsats,
                            uføreDagsats,
                            skalUføreSamordnes,
                            foreldrepengerDagsats,
                            svangerskapspengerDagsats,
                            // 4-28 Utestenging
                            utestengt,
                            // 4-12 Redigering av barne opplysninger
                            barn,
                            antallBarn,
                            arbeidstidsreduksjonIkkeBruktTidligere,
                            barnetilleggetsStørrelse,
                            // 21-7 Uriktige opplysninger
                            uriktigeOpplysninger,
                            holderTilbake,
                            unnlateråEtterkommePålegg,
                            // Forvaltningsloven kapittel 6 - paragraf 35
                            endringIkkeTilSkade,
                            ikkeUnderretning,
                            ansesUgyldigVedtak,
                            // Gjenoppptak
                            oppholdMedArbeidI12ukerEllerMer,
                            skalGjenopptakVurderes,
                            // Ferietillegg
                            antallDagerForbruk,
                            sumUtbetaltForÅr,
                            // Eksport
                            skalEksportVurderes,
                            skalHaEksport,
                            registrertIVertsland,
                            antallDagerFristForRegistrering,
                        ),
                    )

                    // 4-5 Registrert arbeidssøker
                    // add(RegistrertArbeidssøker.registrertArbeidssøker)
                    add(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker)

                    // Redigering for å kunne tilbakedatere søknader i dev
                    if (unleash.isEnabled(Feature.REDIGERING_AV_REGISTRERT_ARBEIDSSØKER.navn)) {
                        add(søknadIdOpplysningstype)
                    }

                    // MANUELL overstyring av beregning
                    if (unleash.isEnabled(Feature.REDIGERING_AV_BEREGNING.navn)) {
                        add(Beregning.meldt)
                        add(Beregning.forbruk)
                        add(Beregning.forbruktEgenandel)
                        add(Beregning.gjenståendeEgenandel)
                        add(Beregning.gjenståendeDager)
                        add(Beregning.utbetaling)
                    }
                }

        override fun kanRedigere(opplysningstype: Opplysningstype<*>): Boolean = redigerbare.contains(opplysningstype)
    }

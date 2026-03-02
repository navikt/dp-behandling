package no.nav.dagpenger.behandling.mediator.api

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.BehandlingTilstandDTO
import no.nav.dagpenger.behandling.api.models.FormålDTO
import no.nav.dagpenger.behandling.api.models.HjemmelDTO
import no.nav.dagpenger.behandling.api.models.LovkildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.RedigerbareOpplysningerDTO
import no.nav.dagpenger.behandling.api.models.RegelsettDTO
import no.nav.dagpenger.behandling.api.models.RegelsettTypeDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlersVurderingerDTO
import no.nav.dagpenger.behandling.konfigurasjon.Feature
import no.nav.dagpenger.behandling.konfigurasjon.unleash
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Redigerbar
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.FulleYtelser.ikkeFulleYtelser
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Opphold.medlemFolketrygden
import no.nav.dagpenger.regel.Opphold.oppholdINorge
import no.nav.dagpenger.regel.Opphold.unntakForOpphold
import no.nav.dagpenger.regel.Permittering.erPermitteringenMidlertidig
import no.nav.dagpenger.regel.Permittering.godkjentPermitteringsårsak
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.erPermitteringenFraFiskeindustriMidlertidig
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.godkjentÅrsakPermitteringFraFiskindustri
import no.nav.dagpenger.regel.ReellArbeidssøker.erArbeidsfør
import no.nav.dagpenger.regel.ReellArbeidssøker.godkjentArbeidsufør
import no.nav.dagpenger.regel.ReellArbeidssøker.godkjentDeltidssøker
import no.nav.dagpenger.regel.ReellArbeidssøker.godkjentLokalArbeidssøker
import no.nav.dagpenger.regel.ReellArbeidssøker.kanJobbeDeltid
import no.nav.dagpenger.regel.ReellArbeidssøker.kanJobbeHvorSomHelst
import no.nav.dagpenger.regel.ReellArbeidssøker.minimumVanligArbeidstid
import no.nav.dagpenger.regel.ReellArbeidssøker.villigTilEthvertArbeid
import no.nav.dagpenger.regel.ReellArbeidssøker.ønsketArbeidstid
import no.nav.dagpenger.regel.RegistrertArbeidssøker
import no.nav.dagpenger.regel.Rettighetstype.erPermittert
import no.nav.dagpenger.regel.Rettighetstype.erReellArbeidssøkerVurdert
import no.nav.dagpenger.regel.Rettighetstype.permitteringFiskeforedling
import no.nav.dagpenger.regel.Rettighetstype.skalVernepliktVurderes
import no.nav.dagpenger.regel.Samordning.foreldrepenger
import no.nav.dagpenger.regel.Samordning.foreldrepengerDagsats
import no.nav.dagpenger.regel.Samordning.omsorgspenger
import no.nav.dagpenger.regel.Samordning.omsorgspengerDagsats
import no.nav.dagpenger.regel.Samordning.opplæringspenger
import no.nav.dagpenger.regel.Samordning.opplæringspengerDagsats
import no.nav.dagpenger.regel.Samordning.pleiepenger
import no.nav.dagpenger.regel.Samordning.pleiepengerDagsats
import no.nav.dagpenger.regel.Samordning.samordnetArbeidstid
import no.nav.dagpenger.regel.Samordning.skalUføreSamordnes
import no.nav.dagpenger.regel.Samordning.svangerskapspenger
import no.nav.dagpenger.regel.Samordning.svangerskapspengerDagsats
import no.nav.dagpenger.regel.Samordning.sykepenger
import no.nav.dagpenger.regel.Samordning.sykepengerDagsats
import no.nav.dagpenger.regel.Samordning.uføre
import no.nav.dagpenger.regel.Samordning.uføreDagsats
import no.nav.dagpenger.regel.StreikOgLockout.deltarIStreikOgLockout
import no.nav.dagpenger.regel.StreikOgLockout.sammeBedriftOgPåvirket
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.arbeidstidsreduksjonIkkeBruktTidligere
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregningsregel12mnd
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregningsregel36mnd
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregningsregel6mnd
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.kravPåLønn
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.nyArbeidstid
import no.nav.dagpenger.regel.Utdanning.deltakelseIArbeidsmarkedstiltak
import no.nav.dagpenger.regel.Utdanning.deltakelsePåKurs
import no.nav.dagpenger.regel.Utdanning.grunnskoleopplæring
import no.nav.dagpenger.regel.Utdanning.høyereUtdanning
import no.nav.dagpenger.regel.Utdanning.høyereYrkesfagligUtdanning
import no.nav.dagpenger.regel.Utdanning.opplæringForInnvandrere
import no.nav.dagpenger.regel.Utdanning.tarUtdanning
import no.nav.dagpenger.regel.Utestengning.utestengt
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.antallBarn
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.barn
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.barnetilleggetsStørrelse
import no.nav.dagpenger.regel.prosessvilkår.OmgjøringUtenKlage.ansesUgyldigVedtak
import no.nav.dagpenger.regel.prosessvilkår.OmgjøringUtenKlage.endringIkkeTilSkade
import no.nav.dagpenger.regel.prosessvilkår.OmgjøringUtenKlage.ikkeUnderretning
import no.nav.dagpenger.regel.`prosessvilkår`.Uriktigeopplysninger.holderTilbake
import no.nav.dagpenger.regel.`prosessvilkår`.Uriktigeopplysninger.`unnlateråEtterkommePålegg`
import no.nav.dagpenger.regel.`prosessvilkår`.Uriktigeopplysninger.uriktigeOpplysninger
import java.time.LocalDateTime

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
                        formål = type.tilFormålDTO(),
                    )
                }),
            saksbehandlingsregler =
                behandler.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Prosess)
                    .map { it.tilVurderingsresultatDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            forslagOm = vedtakopplysninger.rettighetsperioder.avgjørelse(),
            opprettet = opprettet,
            sistEndret = sistEndret,
        )
    }

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
        relevantForResultat = produkter.isNotEmpty(),
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
                            harLøpendeRett,
                            prøvingsdato,
                            // 4-2 Opphold
                            oppholdINorge,
                            unntakForOpphold,
                            medlemFolketrygden,
                            // 4-3
                            kravPåLønn,
                            beregningsregel6mnd,
                            beregningsregel12mnd,
                            beregningsregel36mnd,
                            beregnetArbeidstid,
                            nyArbeidstid,
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
                            erReellArbeidssøkerVurdert,
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
                            // 4-19 Verneplikt
                            oppfyllerKravetTilVerneplikt,
                            skalVernepliktVurderes,
                            // 4-22 Streik og lockout
                            deltarIStreikOgLockout,
                            sammeBedriftOgPåvirket,
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
                        ),
                    )

                    // 4-5 Registrert arbeidssøker
                    add(RegistrertArbeidssøker.registrertArbeidssøker)
                    add(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker)

                    // Redigering for å kunne tilbakedatere søknader i dev
                    if (unleash.isEnabled(Feature.REDIGERING_AV_REGISTRERT_ARBEIDSSØKER.navn)) {
                        add(søknadIdOpplysningstype)
                    }

                    // MANUELL overstyring av beregning
                    if (unleash.isEnabled(Feature.REDIGERING_AV_BEREGNING.navn)) {
                        add(Beregning.meldtITide)
                        add(Beregning.forbruk)
                        add(Beregning.forbruktEgenandel)
                        add(Beregning.gjenståendeEgenandel)
                        add(Beregning.gjenståendeDager)
                        add(Beregning.utbetaling)
                    }
                }

        override fun kanRedigere(opplysningstype: Opplysningstype<*>): Boolean = redigerbare.contains(opplysningstype)
    }

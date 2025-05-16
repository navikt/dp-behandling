package no.nav.dagpenger.behandling.mediator.api

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.api.models.AvklaringDTO
import no.nav.dagpenger.behandling.api.models.AvklaringDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.BarnVerdiDTO
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BegrunnelseDTO
import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.BehandlingDTOTilstandDTO
import no.nav.dagpenger.behandling.api.models.BehandlingOpplysningerDTO
import no.nav.dagpenger.behandling.api.models.BehandlingOpplysningerDTOTilstandDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.HjemmelDTO
import no.nav.dagpenger.behandling.api.models.LovkildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTOFormålDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.RegelsettDTO
import no.nav.dagpenger.behandling.api.models.RegelsettDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlersVurderingerDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
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
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Redigerbar
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
import no.nav.dagpenger.regel.FulleYtelser.ikkeFulleYtelser
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
import no.nav.dagpenger.regel.ReellArbeidssøker.kanReellArbeidssøkerVurderes
import no.nav.dagpenger.regel.ReellArbeidssøker.minimumVanligArbeidstid
import no.nav.dagpenger.regel.ReellArbeidssøker.villigTilEthvertArbeid
import no.nav.dagpenger.regel.ReellArbeidssøker.ønsketArbeidstid
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Rettighetstype.erPermittert
import no.nav.dagpenger.regel.Rettighetstype.permitteringFiskeforedling
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
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.barn
import java.time.LocalDate
import kotlin.collections.map

private val logger = KotlinLogging.logger { }

internal fun Behandling.tilBehandlingDTO(): BehandlingDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        // TODO: Det her må vi slutte med. Innholdet i vedtaktet må periodiseres
        val lesbareOpplysninger = opplysninger().forDato(opplysninger.finnOpplysning(prøvingsdato).verdi)
        val opplysningSet = lesbareOpplysninger.finnAlle().toSet()
        val avklaringer = avklaringer().toSet()
        val spesifikkeAvklaringskoder =
            behandler.regelverk.regelsett
                .asSequence()
                .flatMap { it.avklaringer }
                .toSet()
        val generelleAvklaringer = avklaringer.filterNot { it.kode in spesifikkeAvklaringskoder }

        val relevanteVilkår: List<Regelsett> = RegelverkDagpenger.relevanteVilkår(lesbareOpplysninger)
        val utfall = relevanteVilkår.flatMap { it.utfall }.all { lesbareOpplysninger.oppfyller(it) }

        BehandlingDTO(
            behandlingId = this.behandlingId,
            utfall = utfall,
            tilstand =
                when (this.tilstand().first) {
                    Behandling.TilstandType.UnderOpprettelse -> BehandlingDTOTilstandDTO.UNDER_OPPRETTELSE
                    Behandling.TilstandType.UnderBehandling -> BehandlingDTOTilstandDTO.UNDER_BEHANDLING
                    Behandling.TilstandType.ForslagTilVedtak -> BehandlingDTOTilstandDTO.FORSLAG_TIL_VEDTAK
                    Behandling.TilstandType.Låst -> BehandlingDTOTilstandDTO.LÅST
                    Behandling.TilstandType.Avbrutt -> BehandlingDTOTilstandDTO.AVBRUTT
                    Behandling.TilstandType.Ferdig -> BehandlingDTOTilstandDTO.FERDIG
                    Behandling.TilstandType.Redigert -> BehandlingDTOTilstandDTO.REDIGERT
                    Behandling.TilstandType.TilGodkjenning -> BehandlingDTOTilstandDTO.TIL_GODKJENNING
                    Behandling.TilstandType.TilBeslutning -> BehandlingDTOTilstandDTO.TIL_BESLUTNING
                },
            vilkår =
                behandler.regelverk
                    .regelsettAvType(RegelsettType.Vilkår)
                    .map { it.tilRegelsettDTO(opplysningSet, avklaringer, lesbareOpplysninger) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            fastsettelser =
                behandler.regelverk
                    .regelsettAvType(RegelsettType.Fastsettelse)
                    .map { it.tilRegelsettDTO(opplysningSet, avklaringer, lesbareOpplysninger) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            behandletHendelse =
                HendelseDTO(
                    id =
                        this.behandler.eksternId.id
                            .toString(),
                    datatype = this.behandler.eksternId.datatype,
                    type =
                        when (this.behandler.eksternId) {
                            is MeldekortId -> HendelseDTOTypeDTO.MELDEKORT
                            is SøknadId -> HendelseDTOTypeDTO.SØKNAD
                            is ManuellId -> HendelseDTOTypeDTO.MANUELL
                        },
                ),
            kreverTotrinnskontroll = this.kreverTotrinnskontroll(),
            avklaringer = generelleAvklaringer.map { it.tilAvklaringDTO() },
            opplysninger = opplysningSet.map { it.tilOpplysningDTO(lesbareOpplysninger) },
        )
    }

internal fun Behandling.tilSaksbehandlersVurderinger() =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val avklaringer = avklaringer().filter { it.løstAvSaksbehandler() }.toSet()
        val endredeOpplysninger = opplysninger().finnAlle().filter { it.kilde is Saksbehandlerkilde }.toSet()

        val regelsett = behandler.regelverk.regelsett

        // Ta med kun regelsett som har endrede opplysninger
        val endredRegelsett =
            regelsett.filter { regelsett ->
                regelsett.produserer.any { opplysningstype ->
                    endredeOpplysninger.any { it.opplysningstype == opplysningstype }
                }
            }

        SaksbehandlersVurderingerDTO(
            behandlingId = behandlingId,
            regelsett = endredRegelsett.map { it.tilRegelsettDTO(endredeOpplysninger, avklaringer, opplysninger()) },
            avklaringer = avklaringer.map { it.tilAvklaringDTO() },
            opplysninger = endredeOpplysninger.map { it.tilOpplysningDTO(opplysninger()) },
        )
    }

private fun Regelsett.tilRegelsettDTO(
    opplysninger: Set<Opplysning<*>>,
    avklaringer: Set<Avklaring>,
    lesbarOpplysninger: LesbarOpplysninger,
): RegelsettDTO {
    val produkter =
        opplysninger
            .filter { opplysning -> opplysning.opplysningstype in produserer }
            .sortedBy { produserer.indexOf(it.opplysningstype) }

    val egneAvklaringer = avklaringer.filter { it.kode in this.avklaringer }

    val opplysningMedUtfall = opplysninger.filterIsInstance<Opplysning<Boolean>>().filter { utfall.contains(it.opplysningstype) }
    var status = tilStatus(opplysningMedUtfall)
    val erRelevant = påvirkerResultat(lesbarOpplysninger)

    if (!erRelevant) {
        status = RegelsettDTOStatusDTO.IKKE_RELEVANT
    }

    if (egneAvklaringer.any { it.måAvklares() }) {
        status = RegelsettDTOStatusDTO.HAR_AVKLARING
    }

    return RegelsettDTO(
        navn = hjemmel.kortnavn,
        hjemmel =
            HjemmelDTO(
                kilde = LovkildeDTO(hjemmel.kilde.navn, hjemmel.kilde.kortnavn),
                kapittel = hjemmel.kapittel.toString(),
                paragraf = hjemmel.paragraf.toString(),
                tittel = hjemmel.toString(),
                url = hjemmel.url,
            ),
        avklaringer = egneAvklaringer.map { it.tilAvklaringDTO() },
        opplysningIder = produkter.map { opplysning -> opplysning.id },
        status = status,
        relevantForVedtak = erRelevant,
    )
}

private fun tilStatus(utfall: List<Opplysning<Boolean>>): RegelsettDTOStatusDTO {
    if (utfall.isEmpty()) return RegelsettDTOStatusDTO.INFO

    return if (utfall.all { it.verdi }) {
        RegelsettDTOStatusDTO.OPPFYLT
    } else {
        RegelsettDTOStatusDTO.IKKE_OPPFYLT
    }
}

internal fun Behandling.tilBehandlingOpplysningerDTO(): BehandlingOpplysningerDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val lesbareOpplysninger = this.opplysninger()
        BehandlingOpplysningerDTO(
            behandlingId = this.behandlingId,
            tilstand =
                when (this.tilstand().first) {
                    Behandling.TilstandType.UnderOpprettelse -> BehandlingOpplysningerDTOTilstandDTO.UNDER_OPPRETTELSE
                    Behandling.TilstandType.UnderBehandling -> BehandlingOpplysningerDTOTilstandDTO.UNDER_BEHANDLING
                    Behandling.TilstandType.ForslagTilVedtak -> BehandlingOpplysningerDTOTilstandDTO.FORSLAG_TIL_VEDTAK
                    Behandling.TilstandType.Låst -> BehandlingOpplysningerDTOTilstandDTO.LÅST
                    Behandling.TilstandType.Avbrutt -> BehandlingOpplysningerDTOTilstandDTO.AVBRUTT
                    Behandling.TilstandType.Ferdig -> BehandlingOpplysningerDTOTilstandDTO.FERDIG
                    Behandling.TilstandType.Redigert -> BehandlingOpplysningerDTOTilstandDTO.REDIGERT
                    Behandling.TilstandType.TilGodkjenning -> BehandlingOpplysningerDTOTilstandDTO.TIL_GODKJENNING
                    Behandling.TilstandType.TilBeslutning -> BehandlingOpplysningerDTOTilstandDTO.TIL_BESLUTNING
                },
            opplysning =
                lesbareOpplysninger.finnAlle().map { opplysning ->
                    opplysning.tilOpplysningDTO(lesbareOpplysninger)
                },
            kreverTotrinnskontroll = this.kreverTotrinnskontroll(),
            aktiveAvklaringer =
                this
                    .aktiveAvklaringer()
                    .map { avklaring ->
                        avklaring.tilAvklaringDTO()
                    }.also {
                        logger.info { "Mapper '${it.size}' (aktive) avklaringer til AvklaringDTO " }
                    },
            avklaringer =
                this
                    .avklaringer()
                    .map { avklaring ->
                        avklaring.tilAvklaringDTO()
                    }.also {
                        logger.info { "Mapper '${it.size}' (alle) avklaringer til AvklaringDTO " }
                    },
        )
    }

internal fun Avklaring.tilAvklaringDTO(): AvklaringDTO {
    val sisteEndring = this.endringer.last()
    val saksbehandlerEndring =
        sisteEndring.takeIf {
            it is Avklaring.Endring.Avklart && it.avklartAv is Saksbehandlerkilde
        } as Avklaring.Endring.Avklart?
    val saksbehandler =
        (saksbehandlerEndring?.avklartAv as Saksbehandlerkilde?)
            ?.let { SaksbehandlerDTO(it.saksbehandler.ident) }

    return AvklaringDTO(
        id = this.id,
        kode = this.kode.kode,
        tittel = this.kode.tittel,
        beskrivelse = this.kode.beskrivelse,
        kanKvitteres = kanKvitteres,
        status =
            when (sisteEndring) {
                is Avklaring.Endring.Avbrutt -> AvklaringDTOStatusDTO.AVBRUTT
                is Avklaring.Endring.Avklart -> AvklaringDTOStatusDTO.AVKLART
                is Avklaring.Endring.UnderBehandling -> AvklaringDTOStatusDTO.ÅPEN
            },
        maskinelt = sisteEndring !is Avklaring.Endring.UnderBehandling && saksbehandler == null,
        begrunnelse = saksbehandlerEndring?.begrunnelse,
        avklartAv = saksbehandler,
        sistEndret = sisteEndring.endret,
    )
}

internal fun Opplysning<*>.tilOpplysningDTO(opplysninger: LesbarOpplysninger): OpplysningDTO =
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
                    BarnelisteDTO(
                        (this.verdi as BarnListe).map {
                            BarnVerdiDTO(
                                it.fødselsdato,
                                it.fornavnOgMellomnavn,
                                it.etternavn,
                                it.statsborgerskap,
                                it.kvalifiserer,
                            )
                        },
                    )
                Boolsk -> BoolskVerdiDTO(this.verdi as Boolean)
                Dato -> DatoVerdiDTO(this.verdi as LocalDate)
                Desimaltall -> DesimaltallVerdiDTO(this.verdi as Double)
                Heltall -> HeltallVerdiDTO(this.verdi as Int)
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
        gyldigFraOgMed = this.gyldighetsperiode.fom.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tom.tilApiDato(),
        datatype =
            when (this.opplysningstype.datatype) {
                Boolsk -> DataTypeDTO.BOOLSK
                Dato -> DataTypeDTO.DATO
                Desimaltall -> DataTypeDTO.DESIMALTALL
                Heltall -> DataTypeDTO.HELTALL
                ULID -> DataTypeDTO.ULID
                Penger -> DataTypeDTO.PENGER
                InntektDataType -> DataTypeDTO.INNTEKT
                BarnDatatype -> DataTypeDTO.BARN
                Tekst -> DataTypeDTO.TEKST
                PeriodeDataType -> DataTypeDTO.DATO // TODO
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
                )
            },
        redigerbar = this.kanRedigeres(redigerbareOpplysninger),
        kanOppfriskes = this.kanOppfriskes(),
        synlig = this.opplysningstype.synlig(opplysninger),
        formål =
            when (this.opplysningstype.formål) {
                Opplysningsformål.Legacy -> OpplysningDTOFormålDTO.LEGACY
                Opplysningsformål.Bruker -> OpplysningDTOFormålDTO.BRUKER
                Opplysningsformål.Register -> OpplysningDTOFormålDTO.REGISTER
                Opplysningsformål.Regel -> OpplysningDTOFormålDTO.REGEL
            },
    )

private fun LocalDate.tilApiDato(): LocalDate? =
    when (this) {
        LocalDate.MIN -> null
        LocalDate.MAX -> null
        else -> this
    }

private fun Opplysning<*>.kanOppfriskes(): Boolean =
    this.opplysningstype in
        setOf(
            grunnbeløpForDagpengeGrunnlag,
        )

// TODO: Denne bor nok et annet sted - men bare for å vise at det er mulig å ha en slik funksjon
private val redigerbareOpplysninger =
    object : Redigerbar {
        private val redigerbare =
            setOf(
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
                kanReellArbeidssøkerVurderes,
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
                // 4-22 Streik og lockøout
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
                arbeidstidsreduksjonIkkeBruktTidligere,
            )

        override fun kanRedigere(opplysning: Opplysning<*>): Boolean = redigerbare.contains(opplysning.opplysningstype)
    }

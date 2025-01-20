package no.nav.dagpenger.behandling.mediator.api

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.api.models.AvklaringDTO
import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.BehandlingOpplysningerDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.HjemmelDTO
import no.nav.dagpenger.behandling.api.models.LovkildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.RegelsettDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.modell.Behandling
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
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Redigerbar
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.FulleYtelser.ikkeFulleYtelser
import no.nav.dagpenger.regel.Opphold.medlemFolketrygden
import no.nav.dagpenger.regel.Opphold.oppholdINorge
import no.nav.dagpenger.regel.Opphold.unntakForOpphold
import no.nav.dagpenger.regel.ReellArbeidssøker.erArbeidsfør
import no.nav.dagpenger.regel.ReellArbeidssøker.godkjentDeltidssøker
import no.nav.dagpenger.regel.ReellArbeidssøker.godkjentLokalArbeidssøker
import no.nav.dagpenger.regel.ReellArbeidssøker.kanJobbeDeltid
import no.nav.dagpenger.regel.ReellArbeidssøker.kanJobbeHvorSomHelst
import no.nav.dagpenger.regel.ReellArbeidssøker.villigTilEthvertArbeid
import no.nav.dagpenger.regel.Samordning.foreldrepenger
import no.nav.dagpenger.regel.Samordning.foreldrepengerDagsats
import no.nav.dagpenger.regel.Samordning.omsorgspenger
import no.nav.dagpenger.regel.Samordning.omsorgspengerDagsats
import no.nav.dagpenger.regel.Samordning.opplæringspenger
import no.nav.dagpenger.regel.Samordning.opplæringspengerDagsats
import no.nav.dagpenger.regel.Samordning.pleiepenger
import no.nav.dagpenger.regel.Samordning.pleiepengerDagsats
import no.nav.dagpenger.regel.Samordning.samordnetArbeidstid
import no.nav.dagpenger.regel.Samordning.svangerskapspenger
import no.nav.dagpenger.regel.Samordning.svangerskapspengerDagsats
import no.nav.dagpenger.regel.Samordning.sykepenger
import no.nav.dagpenger.regel.Samordning.sykepengerDagsats
import no.nav.dagpenger.regel.Samordning.uføre
import no.nav.dagpenger.regel.Samordning.uføreDagsats
import no.nav.dagpenger.regel.StreikOgLockout.deltarIStreikOgLockout
import no.nav.dagpenger.regel.StreikOgLockout.sammeBedriftOgPåvirket
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregningsregel12mnd
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregningsregel36mnd
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregningsregel6mnd
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.minimumVanligArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.nyArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.ønsketArbeidstid
import no.nav.dagpenger.regel.Utdanning.deltakelseIArbeidsmarkedstiltak
import no.nav.dagpenger.regel.Utdanning.deltakelsePåKurs
import no.nav.dagpenger.regel.Utdanning.grunnskoleopplæring
import no.nav.dagpenger.regel.Utdanning.høyereUtdanning
import no.nav.dagpenger.regel.Utdanning.høyereYrkesfagligUtdanning
import no.nav.dagpenger.regel.Utdanning.opplæringForInnvandrere
import no.nav.dagpenger.regel.Utdanning.tarUtdanning
import no.nav.dagpenger.regel.Utestengning.utestengt
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import java.time.LocalDate
import kotlin.collections.map

private val logger = KotlinLogging.logger { }

internal fun Behandling.tilBehandlingDTO(): BehandlingDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val lesbareOpplysninger = opplysninger()
        val opplysningSet = lesbareOpplysninger.finnAlle().toSet()
        val avklaringer = avklaringer().toSet()
        val spesifikkeAvklaringskoder =
            behandler.regelverk.regelsett
                .asSequence()
                .flatMap { it.avklaringer() }
                .toSet()
        val generelleAvklaringer = avklaringer.filterNot { it.kode in spesifikkeAvklaringskoder }

        BehandlingDTO(
            behandlingId = this.behandlingId,
            tilstand =
                when (this.tilstand().first) {
                    Behandling.TilstandType.UnderOpprettelse -> BehandlingDTO.Tilstand.UnderOpprettelse
                    Behandling.TilstandType.UnderBehandling -> BehandlingDTO.Tilstand.UnderBehandling
                    Behandling.TilstandType.ForslagTilVedtak -> BehandlingDTO.Tilstand.ForslagTilVedtak
                    Behandling.TilstandType.Låst -> BehandlingDTO.Tilstand.Låst
                    Behandling.TilstandType.Avbrutt -> BehandlingDTO.Tilstand.Avbrutt
                    Behandling.TilstandType.Ferdig -> BehandlingDTO.Tilstand.Ferdig
                    Behandling.TilstandType.Redigert -> BehandlingDTO.Tilstand.Redigert
                    Behandling.TilstandType.TilGodkjenning -> BehandlingDTO.Tilstand.TilGodkjenning
                    Behandling.TilstandType.TilBeslutning -> BehandlingDTO.Tilstand.TilBeslutning
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
            kreverTotrinnskontroll = this.kreverTotrinnskontroll(),
            avklaringer = generelleAvklaringer.map { it.tilAvklaringDTO() },
            opplysninger = opplysningSet.map { it.tilOpplysningDTO(lesbareOpplysninger) },
        )
    }

private fun Regelsett.tilRegelsettDTO(
    opplysninger: Set<Opplysning<*>>,
    avklaringer: Set<Avklaring>,
    lesbarOpplysninger: LesbarOpplysninger,
): RegelsettDTO {
    val produserer = opplysninger.filter { opplysning -> opplysning.opplysningstype in produserer }
    val avklaringskoder = avklaringer()
    val egneAvklaringer = avklaringer.filter { it.kode in avklaringskoder }

    val opplysningMedUtfall = opplysninger.singleOrNull { it.opplysningstype == utfall }
    var status = tilStatus(opplysningMedUtfall?.verdi as Boolean?)

    if (egneAvklaringer.any { it.måAvklares() }) {
        status = RegelsettDTO.Status.HarAvklaring
    }

    val erRelevant = erRelevant(lesbarOpplysninger)

    return RegelsettDTO(
        navn = hjemmel.kortnavn,
        hjemmel =
            HjemmelDTO(
                kilde = LovkildeDTO(hjemmel.kilde.navn, hjemmel.kilde.kortnavn),
                kapittel = hjemmel.kapittel.toString(),
                paragraf = hjemmel.paragraf.toString(),
                tittel = hjemmel.toString(),
            ),
        avklaringer = egneAvklaringer.map { it.tilAvklaringDTO() },
        opplysningIder = produserer.map { opplysning -> opplysning.id },
        status = status,
        relevantForVedtak = erRelevant,
    )
}

private fun tilStatus(utfall: Boolean?): RegelsettDTO.Status {
    if (utfall == null) return RegelsettDTO.Status.Info

    return if (utfall) {
        RegelsettDTO.Status.Oppfylt
    } else {
        RegelsettDTO.Status.IkkeOppfylt
    }
}

internal fun Behandling.tilBehandlingOpplysningerDTO(): BehandlingOpplysningerDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val lesbareOpplysninger = this.opplysninger()
        BehandlingOpplysningerDTO(
            behandlingId = this.behandlingId,
            tilstand =
                when (this.tilstand().first) {
                    Behandling.TilstandType.UnderOpprettelse -> BehandlingOpplysningerDTO.Tilstand.UnderOpprettelse
                    Behandling.TilstandType.UnderBehandling -> BehandlingOpplysningerDTO.Tilstand.UnderBehandling
                    Behandling.TilstandType.ForslagTilVedtak -> BehandlingOpplysningerDTO.Tilstand.ForslagTilVedtak
                    Behandling.TilstandType.Låst -> BehandlingOpplysningerDTO.Tilstand.Låst
                    Behandling.TilstandType.Avbrutt -> BehandlingOpplysningerDTO.Tilstand.Avbrutt
                    Behandling.TilstandType.Ferdig -> BehandlingOpplysningerDTO.Tilstand.Ferdig
                    Behandling.TilstandType.Redigert -> BehandlingOpplysningerDTO.Tilstand.Redigert
                    Behandling.TilstandType.TilGodkjenning -> BehandlingOpplysningerDTO.Tilstand.TilGodkjenning
                    Behandling.TilstandType.TilBeslutning -> BehandlingOpplysningerDTO.Tilstand.TilBeslutning
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
        status =
            when (sisteEndring) {
                is Avklaring.Endring.Avbrutt -> AvklaringDTO.Status.Avbrutt
                is Avklaring.Endring.Avklart -> AvklaringDTO.Status.Avklart
                is Avklaring.Endring.UnderBehandling -> AvklaringDTO.Status.Åpen
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
        navn = this.opplysningstype.navn,
        tekstId = this.opplysningstype.tekstId,
        verdi =
            when (this.opplysningstype.datatype) {
                // todo: Frontenden burde vite om det er penger og håndtere det med valuta
                Penger -> (this.verdi as Beløp).uavrundet.toString()
                else -> this.verdi.toString()
            },
        status =
            when (this) {
                is Faktum -> OpplysningDTO.Status.Faktum
                is Hypotese -> OpplysningDTO.Status.Hypotese
            },
        gyldigFraOgMed = this.gyldighetsperiode.fom.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tom.tilApiDato(),
        datatype =
            when (this.opplysningstype.datatype) {
                Boolsk -> DataTypeDTO.boolsk
                Dato -> DataTypeDTO.dato
                Desimaltall -> DataTypeDTO.desimaltall
                Heltall -> DataTypeDTO.heltall
                ULID -> DataTypeDTO.ulid
                Penger -> DataTypeDTO.penger
                InntektDataType -> DataTypeDTO.inntekt
                BarnDatatype -> DataTypeDTO.barn
                Tekst -> DataTypeDTO.tekst
            },
        kilde =
            this.kilde?.let {
                val registrert = it.registrert
                when (it) {
                    is Saksbehandlerkilde -> OpplysningskildeDTO("Saksbehandler", ident = it.saksbehandler.ident, registrert = registrert)
                    is Systemkilde -> OpplysningskildeDTO("System", meldingId = it.meldingsreferanseId, registrert = registrert)
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
        synlig = this.opplysningstype.synlig(opplysninger),
        formål = OpplysningDTO.Formål.valueOf(this.opplysningstype.formål.name),
    )

private fun LocalDate.tilApiDato(): LocalDate? =
    when (this) {
        LocalDate.MIN -> null
        LocalDate.MAX -> null
        else -> this
    }

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
                ønsketArbeidstid,
                beregningsregel6mnd,
                beregningsregel12mnd,
                beregningsregel36mnd,
                beregnetArbeidstid,
                nyArbeidstid,
                minimumVanligArbeidstid,
                // 4-5
                kanJobbeDeltid,
                kanJobbeHvorSomHelst,
                erArbeidsfør,
                villigTilEthvertArbeid,
                godkjentDeltidssøker,
                godkjentLokalArbeidssøker,
                // 4-6 Utdanning
                tarUtdanning,
                deltakelseIArbeidsmarkedstiltak,
                opplæringForInnvandrere,
                grunnskoleopplæring,
                høyereYrkesfagligUtdanning,
                høyereUtdanning,
                deltakelsePåKurs,
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
                foreldrepengerDagsats,
                svangerskapspengerDagsats,
                // 4-28 Utestenging
                utestengt,
            )

        override fun kanRedigere(opplysning: Opplysning<*>): Boolean = redigerbare.contains(opplysning.opplysningstype)
    }

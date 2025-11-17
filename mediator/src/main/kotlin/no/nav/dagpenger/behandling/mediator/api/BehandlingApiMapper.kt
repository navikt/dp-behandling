package no.nav.dagpenger.behandling.mediator.api

import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.api.models.AvklaringDTO
import no.nav.dagpenger.behandling.api.models.AvklaringDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlersVurderingerDTO
import no.nav.dagpenger.behandling.konfigurasjon.Feature
import no.nav.dagpenger.behandling.konfigurasjon.unleash
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Redigerbar
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
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
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.barn
import java.time.LocalDate

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

fun Datatype<*>.tilDataTypeDTO() =
    when (this) {
        Boolsk -> DataTypeDTO.BOOLSK
        Dato -> DataTypeDTO.DATO
        Desimaltall -> DataTypeDTO.DESIMALTALL
        Heltall -> DataTypeDTO.HELTALL
        ULID -> DataTypeDTO.ULID
        Penger -> DataTypeDTO.PENGER
        InntektDataType -> DataTypeDTO.INNTEKT
        BarnDatatype -> DataTypeDTO.BARN
        Tekst -> DataTypeDTO.TEKST
        PeriodeDataType -> DataTypeDTO.PERIODE
    }

internal fun LocalDate.tilApiDato(): LocalDate? =
    when {
        this.isEqual(LocalDate.MIN) -> null
        this.isEqual(LocalDate.MAX) -> null
        else -> this
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
                            arbeidstidsreduksjonIkkeBruktTidligere,
                        ),
                    )

                    // 4-5 Registrert arbeidssøker
                    if (unleash.isEnabled(Feature.REDIGERING_AV_REGISTRERT_ARBEIDSSØKER.navn)) {
                        add(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker)
                        add(Dagpengegrunnlag.grunnlag)
                        add(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg)
                        add(søknadIdOpplysningstype)
                    }

                    // MANUELL overstyring av beregning
                    if (unleash.isEnabled(Feature.REDIGERING_AV_BEREGNING.navn)) {
                        add(Beregning.arbeidsdag)
                        add(Beregning.forbruk)
                        add(Beregning.forbruktEgenandel)
                        add(Beregning.gjenståendeEgenandel)
                        add(Beregning.gjenståendePeriode)
                        add(Beregning.utbetaling)
                    }
                }

        override fun kanRedigere(opplysningstype: Opplysningstype<*>): Boolean = redigerbare.contains(opplysningstype)
    }

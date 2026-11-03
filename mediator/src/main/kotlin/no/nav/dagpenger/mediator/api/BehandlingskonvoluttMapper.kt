package no.nav.dagpenger.mediator.api

import no.nav.dagpenger.api.behandling.models.AvgjørelseDTO
import no.nav.dagpenger.api.behandling.models.AvklaringDTO
import no.nav.dagpenger.api.behandling.models.AvklaringDTOStatusDTO
import no.nav.dagpenger.api.behandling.models.BarnVerdiDTO
import no.nav.dagpenger.api.behandling.models.BarnelisteDTO
import no.nav.dagpenger.api.behandling.models.BegrunnelseDTO
import no.nav.dagpenger.api.behandling.models.BoolskVerdiDTO
import no.nav.dagpenger.api.behandling.models.DataTypeDTO
import no.nav.dagpenger.api.behandling.models.DatoVerdiDTO
import no.nav.dagpenger.api.behandling.models.DesimaltallVerdiDTO
import no.nav.dagpenger.api.behandling.models.HeltallVerdiDTO
import no.nav.dagpenger.api.behandling.models.HendelseDTO
import no.nav.dagpenger.api.behandling.models.HendelseDTOTypeDTO
import no.nav.dagpenger.api.behandling.models.HjemmelDTO
import no.nav.dagpenger.api.behandling.models.LovkildeDTO
import no.nav.dagpenger.api.behandling.models.OpplysningskildeDTO
import no.nav.dagpenger.api.behandling.models.OpplysningskildeDTOTypeDTO
import no.nav.dagpenger.api.behandling.models.OpplysningsperiodeDTO
import no.nav.dagpenger.api.behandling.models.OpplysningsperiodeDTOTypeDTO
import no.nav.dagpenger.api.behandling.models.OpplysningsverdiDTO
import no.nav.dagpenger.api.behandling.models.OpprinnelseDTO
import no.nav.dagpenger.api.behandling.models.PengeVerdiDTO
import no.nav.dagpenger.api.behandling.models.PeriodeVerdiDTO
import no.nav.dagpenger.api.behandling.models.RegelDTO
import no.nav.dagpenger.api.behandling.models.RegelsettMetaDTO
import no.nav.dagpenger.api.behandling.models.RegelsettTypeDTO
import no.nav.dagpenger.api.behandling.models.RettighetsperiodeDTO
import no.nav.dagpenger.api.behandling.models.SaksbehandlerDTO
import no.nav.dagpenger.api.behandling.models.TekstVerdiDTO
import no.nav.dagpenger.api.behandling.models.UtledningDTO
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.hendelser.ArbeidssøkerperiodeId
import no.nav.dagpenger.modell.hendelser.FerietilleggId
import no.nav.dagpenger.modell.hendelser.KlageFørsteinstansId
import no.nav.dagpenger.modell.hendelser.KlageKlageinstansId
import no.nav.dagpenger.modell.hendelser.KlageTrygderettenId
import no.nav.dagpenger.modell.hendelser.ManuellId
import no.nav.dagpenger.modell.hendelser.MeldekortId
import no.nav.dagpenger.modell.hendelser.MigreringId
import no.nav.dagpenger.modell.hendelser.OmgjøringId
import no.nav.dagpenger.modell.hendelser.SamordningId
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Behovsløserkilde
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.Tombstone
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.RegelverkDagpenger
import java.time.LocalDate
import kotlin.io.encoding.Base64

internal fun <T> Collection<Opplysning<*>>.somOpplysningperiode(block: (Opplysningstype<*>, Collection<Opplysning<*>>) -> T): List<T> =
    groupBy { it.opplysningstype }.map { (type, opplysninger) ->
        block(type, opplysninger)
    }

internal fun Opplysning<*>.tilOpplysningsperiodeDTO(erEgen: (Opplysning<*>) -> Boolean) =
    OpplysningsperiodeDTO(
        id = this.id,
        opprettet = opprettet,
        opprinnelse = erEgen(this).tilOpprinnelseDTO(),
        gyldigFraOgMed = this.gyldighetsperiode.fraOgMed.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tilOgMed.tilApiDato(),
        type =
            when (this) {
                is Faktum<*> -> OpplysningsperiodeDTOTypeDTO.FAKTUM
                is Hypotese<*> -> OpplysningsperiodeDTOTypeDTO.HYPOTESE
                is Tombstone<*> -> error("Tombstone skal ikke eksponeres")
            },
        verdi = tilOpplysningsverdiDTO(),
        kilde = tilOpplysningskildeDTO(),
        utledetAv =
            utledetAv?.let { utledning ->
                UtledningDTO(
                    regel = RegelDTO(navn = utledning.regel),
                    opplysninger = utledning.opplysninger.map { it.id },
                    versjon = utledning.versjon,
                )
            },
    )

internal fun StartHendelse.tilHendelseDTO(): HendelseDTO =
    HendelseDTO(
        id = eksternId.id.toString(),
        datatype = eksternId.datatype,
        type =
            when (eksternId) {
                is MeldekortId -> HendelseDTOTypeDTO.MELDEKORT
                is SøknadId -> HendelseDTOTypeDTO.SØKNAD
                is ManuellId -> HendelseDTOTypeDTO.MANUELL
                is MigreringId -> HendelseDTOTypeDTO.MIGRERING
                is OmgjøringId -> HendelseDTOTypeDTO.OMGJØRING
                is ArbeidssøkerperiodeId -> HendelseDTOTypeDTO.ARBEIDSSØKERPERIODE
                is FerietilleggId -> HendelseDTOTypeDTO.FERIETILLEGG
                is SamordningId -> HendelseDTOTypeDTO.SAMORDNING
                is KlageFørsteinstansId -> HendelseDTOTypeDTO.KLAGE_FØRSTEINSTANS
                is KlageKlageinstansId -> HendelseDTOTypeDTO.KLAGE_KLAGEINSTANS
                is KlageTrygderettenId -> HendelseDTOTypeDTO.KLAGE_TRYGDERETTEN
            },
        skjedde = skjedde,
    )

internal fun Opplysning<*>.tilOpplysningskildeDTO(): OpplysningskildeDTO? =
    this.kilde?.let {
        val registrert = it.registrert
        when (it) {
            is Saksbehandlerkilde -> {
                OpplysningskildeDTO(
                    OpplysningskildeDTOTypeDTO.SAKSBEHANDLER,
                    ident = it.saksbehandler.ident,
                    begrunnelse = it.begrunnelse?.let { BegrunnelseDTO(it.verdi, it.sistEndret) },
                    registrert = registrert,
                )
            }

            is Systemkilde -> {
                OpplysningskildeDTO(
                    OpplysningskildeDTOTypeDTO.SYSTEM,
                    meldingId = it.meldingsreferanseId,
                    registrert = registrert,
                )
            }

            is Behovsløserkilde -> {
                OpplysningskildeDTO(
                    OpplysningskildeDTOTypeDTO.BEHOVSLØSER,
                    meldingId = it.meldingsreferanseId,
                    registrert = registrert,
                )
            }
        }
    }

internal fun Opplysning<*>.tilOpplysningsverdiDTO(): OpplysningsverdiDTO =
    when (this.opplysningstype.datatype) {
        BarnDatatype -> {
            with(this.verdi as BarnListe) {
                BarnelisteDTO(
                    søknadBarnId = søknadbarnId,
                    verdi =
                        barn.map {
                            BarnVerdiDTO(
                                kilde = null,
                                ident = null,
                                fødselsdato = it.fødselsdato,
                                fornavnOgMellomnavn = it.fornavnOgMellomnavn,
                                etternavn = it.etternavn,
                                statsborgerskap = it.statsborgerskap,
                                oppholdsland = it.oppholdsland,
                                forsørgeransvar = it.forsørgeransvar,
                                kvalifiserer = it.kvalifiserer,
                                begrunnelse = it.begrunnelse,
                            )
                        },
                )
            }
        }

        Boolsk -> {
            BoolskVerdiDTO(this.verdi as Boolean)
        }

        Dato -> {
            DatoVerdiDTO(this.verdi as LocalDate)
        }

        Desimaltall -> {
            DesimaltallVerdiDTO(this.verdi as Double, this.opplysningstype.tilEnhetDTO())
        }

        Heltall -> {
            HeltallVerdiDTO(this.verdi as Int, this.opplysningstype.tilEnhetDTO())
        }

        InntektDataType -> {
            TekstVerdiDTO((this.verdi as Inntekt).verdi.inntektsId)
        }

        Penger -> {
            PengeVerdiDTO(
                verdi = (this.verdi as Beløp).verdien,
            )
        }

        PeriodeDataType -> {
            (this.verdi as Periode).let {
                PeriodeVerdiDTO(it.fraOgMed, it.tilOgMed)
            }
        }

        Tekst, ULID -> {
            TekstVerdiDTO(this.verdi.toString())
        }
    }

internal fun Behandling.VedtakOpplysninger.rettighetsperioder(): List<RettighetsperiodeDTO> {
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

internal fun Boolean.tilOpprinnelseDTO() =
    when (this) {
        true -> OpprinnelseDTO.NY
        false -> OpprinnelseDTO.ARVET
    }

internal fun LocalDate.tilApiDato(): LocalDate? =
    when {
        this.isEqual(LocalDate.MIN) -> null
        this.isEqual(LocalDate.MAX) -> null
        else -> this
    }

internal fun Avgjørelse.tilAvgjørelseDTO(): AvgjørelseDTO =
    when (this) {
        is Avgjørelse.Avslag -> AvgjørelseDTO.AVSLAG
        is Avgjørelse.Endring -> AvgjørelseDTO.ENDRING
        is Avgjørelse.Gjenopptak -> AvgjørelseDTO.GJENOPPTAK
        is Avgjørelse.Innvilgelse -> AvgjørelseDTO.INNVILGELSE
        is Avgjørelse.Opphør -> AvgjørelseDTO.OPPHØR
        is Avgjørelse.Stans -> AvgjørelseDTO.STANS
        is Avgjørelse.Uavklart -> AvgjørelseDTO.AVSLAG
    }

private val regelsettId = RegelverkDagpenger.regelsett.associateWith { it.avklaringer }

internal fun Avklaring.tilAvklaringDTO(): AvklaringDTO {
    val sisteEndring = this.endringer.last()
    val saksbehandlerEndring =
        sisteEndring.takeIf {
            it is Avklaring.Endring.Avklart && it.avklartAv is Saksbehandlerkilde
        } as Avklaring.Endring.Avklart?
    val saksbehandler =
        (saksbehandlerEndring?.avklartAv as Saksbehandlerkilde?)
            ?.let { SaksbehandlerDTO(it.saksbehandler.ident) }

    val påvirkerRegelsett =
        regelsettId.filter { (_, avklaringer) -> avklaringer.contains(this.kode) }.map { (regelsett, _) ->
            val hjemmel = regelsett.hjemmel

            RegelsettMetaDTO(
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
                type =
                    when (regelsett.type) {
                        RegelsettType.Vilkår -> RegelsettTypeDTO.VILKÅR
                        RegelsettType.Fastsettelse -> RegelsettTypeDTO.FASTSETTELSE
                        RegelsettType.Prosess -> RegelsettTypeDTO.PROSESS
                    },
            )
        }

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
        regelsett = påvirkerRegelsett,
        opplysninger = (sisteEndring as? Avklaring.Endring.UnderBehandling)?.opplysninger ?: emptyList(),
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

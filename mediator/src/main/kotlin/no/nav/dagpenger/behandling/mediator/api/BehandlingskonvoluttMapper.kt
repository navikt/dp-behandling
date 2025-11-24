package no.nav.dagpenger.behandling.mediator.api

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.api.models.AvgjørelseDTO
import no.nav.dagpenger.behandling.api.models.AvklaringDTO
import no.nav.dagpenger.behandling.api.models.AvklaringDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.BarnVerdiDTO
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BegrunnelseDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HjemmelDTO
import no.nav.dagpenger.behandling.api.models.LovkildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsperiodeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsverdiDTO
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.RegelsettMetaDTO
import no.nav.dagpenger.behandling.api.models.RegelsettTypeDTO
import no.nav.dagpenger.behandling.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.RegelverkDagpenger
import java.time.LocalDate
import java.util.UUID

internal fun Opplysning<*>.tilOpplysningsperiodeDTO(egneId: List<UUID>) =
    OpplysningsperiodeDTO(
        id = this.id,
        opprettet = opprettet,
        status = (id in egneId).tilOpprinnelseDTO(),
        opprinnelse = (id in egneId).tilOpprinnelseDTO(),
        gyldigFraOgMed = this.gyldighetsperiode.fraOgMed.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tilOgMed.tilApiDato(),
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

internal fun Opplysning<*>.tilOpplysningskildeDTO(): OpplysningskildeDTO? =
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
    }

internal fun Opplysning<*>.tilOpplysningsverdiDTO(): OpplysningsverdiDTO =
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

private fun Boolean.tilOpprinnelseDTO() =
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

internal fun List<Rettighetsperiode>.avgjørelse(): AvgjørelseDTO {
    val (nye, arvede) = this.partition { it.endret }

    return when {
        // Ingen endring
        nye.isEmpty() -> AvgjørelseDTO.ENDRING
        // Ny kjede
        arvede.isEmpty() -> if (nye.harRett()) AvgjørelseDTO.INNVILGELSE else AvgjørelseDTO.AVSLAG
        // Bygger videre på en kjede
        arvede.sisteHarRett() && !nye.harRett() -> AvgjørelseDTO.STANS
        else -> AvgjørelseDTO.GJENOPPTAK
    }
}

private fun List<Rettighetsperiode>.harRett() = any { it.harRett }

private fun List<Rettighetsperiode>.sisteHarRett() = last().harRett

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
                type =
                    when (regelsett.type) {
                        RegelsettType.Vilkår -> RegelsettTypeDTO.VILKÅR
                        RegelsettType.Fastsettelse -> RegelsettTypeDTO.FASTSETTELSE
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

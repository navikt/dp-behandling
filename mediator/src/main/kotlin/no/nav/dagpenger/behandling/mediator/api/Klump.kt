package no.nav.dagpenger.behandling.mediator.api

import mu.withLoggingContext
import no.nav.dagpenger.behandling.api.models.BarnVerdiDTO
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BegrunnelseDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.HjemmelDTO
import no.nav.dagpenger.behandling.api.models.LovkildeDTO
import no.nav.dagpenger.behandling.api.models.NoesomharblittvurdertDTO
import no.nav.dagpenger.behandling.api.models.OpplysningKlumpDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsgruppeKlumpDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.api.models.VerdenbesteklumpmeddataDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import java.time.LocalDate
import kotlin.collections.component1
import kotlin.collections.component2

internal fun Behandling.VedtakOpplysninger.tilKlumpDTO(ident: String): VerdenbesteklumpmeddataDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val opplysningSet = opplysninger.somListe().toSet()

        VerdenbesteklumpmeddataDTO(
            behandlingId = behandlingId,
            ident = ident,
            vilkår =
                behandlingAv.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Vilkår)
                    .mapNotNull { it.tilNoesomharblittvurdertDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            fastsettelser =
                behandlingAv.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Fastsettelse)
                    .mapNotNull { it.tilNoesomharblittvurdertDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
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
            opplysninger =
                opplysninger.somListe().groupBy { it.opplysningstype }.map { (type, opplysninger) ->
                    OpplysningsgruppeKlumpDTO(
                        opplysningTypeId = type.id.uuid,
                        navn = type.navn,
                        datatype = type.datatype.tilDataTypeDTO(),
                        opplysninger = opplysninger.map { opplysning -> opplysning.tilOpplysningDTO() },
                    )
                },
            rettighetsperioder = emptyList(),
        )
    }

private fun Regelsett.tilNoesomharblittvurdertDTO(opplysninger: Set<Opplysning<*>>): NoesomharblittvurdertDTO? {
    val produkter: Set<Opplysning<*>> =
        opplysninger
            .filter { opplysning -> opplysning.opplysningstype in produserer }
            .sortedBy { produserer.indexOf(it.opplysningstype) }
            .toSet()

    if (produkter.isEmpty()) return null

    val typerSomFinnes = produkter.map { it.opplysningstype }.toSet()

    val utfall1: List<Opplysningstype<*>> = typerSomFinnes.filter { it in utfall }
    val ønsket: List<Opplysningstype<*>> = typerSomFinnes.filter { it in ønsketInformasjon }
    return NoesomharblittvurdertDTO(
        navn = hjemmel.kortnavn,
        hjemmel =
            HjemmelDTO(
                kilde = LovkildeDTO(hjemmel.kilde.navn, hjemmel.kilde.kortnavn),
                kapittel = hjemmel.kapittel.toString(),
                paragraf = hjemmel.paragraf.toString(),
                tittel = hjemmel.toString(),
                url = hjemmel.url,
            ),
        utfall = utfall1.map { it.id.uuid },
        ønsketResultat = ønsket.map { it.id.uuid },
        opplysningTypeIder = typerSomFinnes.map { opplysningstype -> opplysningstype.id.uuid },
    )
}

private fun Opplysning<*>.tilOpplysningDTO() =
    OpplysningKlumpDTO(
        id = this.id,
        opplysningTypeId = this.opplysningstype.id.uuid,
        navn = this.opplysningstype.navn,
        verdi =
            when (this.opplysningstype.datatype) {
                // todo: Frontenden burde vite om det er penger og håndtere det med valuta
                Penger -> (this.verdi as Beløp).uavrundet.toString()
                else -> this.verdi.toString()
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
                )
            },
    )

package no.nav.dagpenger.behandling.mediator.api

import mu.withLoggingContext
import no.nav.dagpenger.behandling.api.models.BarnVerdiDTO
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BegrunnelseDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.HjemmelDTO
import no.nav.dagpenger.behandling.api.models.LovkildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsperiodeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsperiodeDTOStatusDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.api.models.VurderingsresultatDTO
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
import no.nav.dagpenger.opplysning.TidslinjeBygger
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.component2
import kotlin.collections.contains

internal fun Behandling.VedtakOpplysninger.tilBehandlingsresultatDTO(ident: String): BehandlingsresultatDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
        val opplysningSet = opplysninger.somListe().toSet()

        val egneId = opplysningSet.map { it.id }

        BehandlingsresultatDTO(
            behandlingId = behandlingId,
            ident = ident,
            vilkår =
                behandlingAv.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Vilkår)
                    .mapNotNull { it.tilVurderingsresultatDTO(opplysningSet) }
                    .sortedBy { it.hjemmel.paragraf.toInt() },
            rettighetsperioder = rettighetsperioder(),
            fastsettelser =
                behandlingAv.forretningsprosess.regelverk
                    .regelsettAvType(RegelsettType.Fastsettelse)
                    .mapNotNull { it.tilVurderingsresultatDTO(opplysningSet) }
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
                    OpplysningerDTO(
                        opplysningTypeId = type.id.uuid,
                        navn = type.navn,
                        perioder = opplysninger.map { opplysning -> opplysning.tilOpplysningsperiodeDTO(egneId) },
                    )
                },
        )
    }

private fun Behandling.VedtakOpplysninger.rettighetsperioder(): List<RettighetsperiodeDTO> {
    val vilkår: List<Opplysningstype<Boolean>> =
        behandlingAv.forretningsprosess.regelverk
            .regelsettAvType(RegelsettType.Vilkår)
            .flatMap { it.utfall }

    val utfall =
        opplysninger
            .somListe()
            .filter { it.opplysningstype in vilkår }
            .filter { it.erRelevant }
            .filterIsInstance<Opplysning<Boolean>>()

    return TidslinjeBygger(utfall)
        .lagPeriode(hvorAlleVilkårErOppfylt())
        .map {
            RettighetsperiodeDTO(
                fraOgMed = it.fraOgMed,
                tilOgMed = it.tilOgMed.tilApiDato(),
                harRett = it.verdi,
            )
        }
}

private fun Regelsett.tilVurderingsresultatDTO(opplysninger: Set<Opplysning<*>>): VurderingsresultatDTO? {
    val produkter: Set<Opplysning<*>> =
        opplysninger
            .filter { opplysning -> opplysning.opplysningstype in produserer }
            .sortedBy { produserer.indexOf(it.opplysningstype) }
            .toSet()

    if (produkter.isEmpty()) return null

    val typerSomFinnes = produkter.map { it.opplysningstype }.toSet()

    val utfall1: List<Opplysningstype<*>> = typerSomFinnes.filter { it in utfall }
    val ønsket: List<Opplysningstype<*>> = typerSomFinnes.filter { it in ønsketInformasjon }

    val utfall =
        opplysninger
            .filter { it.opplysningstype in utfall1 }
            .filterIsInstance<Opplysning<Boolean>>()

    val perioder = TidslinjeBygger(utfall).medLikVerdi()

    return VurderingsresultatDTO(
        navn = hjemmel.kortnavn,
        hjemmel =
            HjemmelDTO(
                kilde = LovkildeDTO(hjemmel.kilde.navn, hjemmel.kilde.kortnavn),
                kapittel = hjemmel.kapittel.toString(),
                paragraf = hjemmel.paragraf.toString(),
                tittel = hjemmel.toString(),
                url = hjemmel.url,
            ),
        perioder =
            perioder.map {
                RettighetsperiodeDTO(
                    fraOgMed = it.fraOgMed,
                    tilOgMed = it.tilOgMed.tilApiDato(),
                    harRett = it.verdi,
                )
            },
        utfall = utfall1.map { it.id.uuid },
        ønsketResultat = ønsket.map { it.id.uuid },
        opplysninger = typerSomFinnes.map { opplysningstype -> opplysningstype.id.uuid },
    )
}

private fun hvorAlleVilkårErOppfylt(): (Collection<Opplysning<Boolean>>) -> Boolean? =
    { påDato -> påDato.isNotEmpty() && påDato.all { it.verdi } }

private fun Opplysning<*>.tilOpplysningsperiodeDTO(egneId: List<UUID>) =
    OpplysningsperiodeDTO(
        id = this.id,
        opprettet = opprettet,
        status =
            when (id in egneId) {
                true -> OpplysningsperiodeDTOStatusDTO.NY
                false -> OpplysningsperiodeDTOStatusDTO.ARVET
            },
        gyldigFraOgMed = this.gyldighetsperiode.fom.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tom.tilApiDato(),
        verdi =
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

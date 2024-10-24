package no.nav.dagpenger.behandling.mediator.api

import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Redigerbar
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.SøknadInnsendtHendelse.Companion.prøvingsdato
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import java.time.LocalDate

internal fun Behandling.tilBehandlingDTO(): BehandlingDTO =
    BehandlingDTO(
        behandlingId = this.behandlingId,
        tilstand = BehandlingDTO.Tilstand.valueOf(tilstand().first.name),
        opplysning =
            this.opplysninger().finnAlle().map { opplysning ->
                opplysning.tilOpplysningDTO()
            },
        kreverTotrinnskontroll = this.kreverTotrinnskontroll(),
    )

internal fun Opplysning<*>.tilOpplysningDTO(): OpplysningDTO =
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
                Tekst -> DataTypeDTO.tekst
            },
        kilde =
            this.kilde?.let {
                val registrert = it.registrert
                when (it) {
                    is Saksbehandlerkilde -> OpplysningskildeDTO("Saksbehandler", ident = it.ident, registrert = registrert)
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
        redigerbar =
            this.kanRedigeres(redigerbareOpplysninger),
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
                TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstid,
                TapAvArbeidsinntektOgArbeidstid.nyArbeidstid,
                prøvingsdato,
            )

        override fun kanRedigere(opplysning: Opplysning<*>): Boolean = redigerbare.contains(opplysning.opplysningstype)
    }

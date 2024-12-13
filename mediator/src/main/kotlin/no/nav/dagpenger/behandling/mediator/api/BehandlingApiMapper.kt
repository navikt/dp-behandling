package no.nav.dagpenger.behandling.mediator.api

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.api.models.AvklaringDTO
import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
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
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Redigerbar
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.ReellArbeidssøker.godkjentDeltidssøker
import no.nav.dagpenger.regel.ReellArbeidssøker.godkjentLokalArbeidssøker
import no.nav.dagpenger.regel.Samordning.sykepengerDagsats
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.Utestengning.utestengt
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

internal fun Behandling.tilBehandlingDTO(): BehandlingDTO =
    withLoggingContext("behandlingId" to this.behandlingId.toString()) {
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
            opplysning =
                this.opplysninger().finnAlle().map { opplysning ->
                    opplysning.tilOpplysningDTO()
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
    val sisteEndring =
        this.endringer.last().takeIf {
            it is Avklaring.Endring.Avklart && it.avklartAv is Saksbehandlerkilde
        } as Avklaring.Endring.Avklart?
    val saksbehandler =
        (sisteEndring?.avklartAv as Saksbehandlerkilde?)?.let {
            SaksbehandlerDTO(it.saksbehandler.ident)
        }
    return AvklaringDTO(
        id = this.id,
        kode = this.kode.kode,
        tittel = this.kode.tittel,
        beskrivelse = this.kode.beskrivelse,
        status =
            when (this.endringer.last()) {
                is Avklaring.Endring.Avbrutt -> AvklaringDTO.Status.Løst
                is Avklaring.Endring.Avklart -> AvklaringDTO.Status.Kvittert
                is Avklaring.Endring.UnderBehandling -> AvklaringDTO.Status.Åpen
            },
        begrunnelse = sisteEndring?.begrunnelse,
        kvittertAv = saksbehandler,
    )
}

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
                sykepengerDagsats,
                godkjentLokalArbeidssøker,
                godkjentDeltidssøker,
                utestengt,
            )

        override fun kanRedigere(opplysning: Opplysning<*>): Boolean = redigerbare.contains(opplysning.opplysningstype)
    }

package no.nav.dagpenger.behandling.mediator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.db.logger
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.behandling.simulering.api.models.BeregningDTO
import no.nav.dagpenger.behandling.simulering.api.models.BeregningDagDTO
import no.nav.dagpenger.behandling.simulering.api.models.BeregningRequestDTO
import no.nav.dagpenger.behandling.simulering.api.models.HttpProblemDTO
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Meldekortprosess
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruktEgenandel
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.ordinærPeriode
import no.nav.dagpenger.regel.fastsetting.Egenandel.egenandel
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.hendelse.tilOpplysninger
import no.nav.dagpenger.uuid.UUIDv7
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration

internal fun Application.simuleringApi() {
    routing {
        swaggerUI(path = "simulering/openapi", swaggerFile = "simulering-api.yaml")
        route("simulering") {
            put("beregning") {
                val beregningRequestDTO = call.receive<BeregningRequestDTO>()
                val opplysninger = simuleringsdata(beregningRequestDTO)
                try {
                    val meldekortprosess = Meldekortprosess()
                    meldekortprosess.kjørUnderOpprettelse(Prosesskontekst(opplysninger))
                    meldekortprosess.kjørRegelkjøringFerdig(Prosesskontekst(opplysninger))
                    val forbruktEgenandel =
                        opplysninger
                            .finnAlle(forbruktEgenandel)
                            .map { it.verdi }
                            .fold(Beløp(0)) { acc, beløp -> acc + beløp }
                    val forbruksdager = opplysninger.finnAlle(Beregning.forbruk)
                    val dagsats = opplysninger.finnOpplysning(dagsatsEtterSamordningMedBarnetillegg).verdi.verdien
                    val fva = opplysninger.finnOpplysning(fastsattVanligArbeidstid).verdi

                    val beregning =
                        BeregningDTO(
                            forbruktEgenandel = forbruktEgenandel.verdien,
                            forbruktKvote = forbruksdager.count { it.verdi },
                            utbetalt =
                                opplysninger
                                    .finnOpplysning(Beregning.utbetalingForPeriode)
                                    .verdi.heleKroner
                                    .toInt(),
                            dager =
                                forbruksdager.map { dag ->
                                    with(opplysninger.forDato(dag.gyldighetsperiode.fraOgMed)) {
                                        BeregningDagDTO(
                                            dato = dag.gyldighetsperiode.fraOgMed,
                                            dagsats = dagsats.toInt(),
                                            utbetalt = finnOpplysning(Beregning.utbetaling).verdi.heleKroner.toInt(),
                                            fastsattVanligArbeidstid = fva,
                                            timerArbeidet =
                                                if (har(Beregning.arbeidstimer)) {
                                                    finnOpplysning(Beregning.arbeidstimer).verdi
                                                } else {
                                                    0.0
                                                },
                                        )
                                    }
                                },
                        )

                    call.respond(HttpStatusCode.OK, beregning)
                } catch (e: Exception) {
                    logger.error(e) { e.message }
                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message =
                            HttpProblemDTO(
                                status = HttpStatusCode.BadRequest.value,
                                title = "Kunne ikke beregne simulering",
                                type = URI("urn:nav:no:dp:simulering:broke"),
                                detail =
                                    (e.message ?: "Ukjent feil ved simulering") + "Opplysninger: ${
                                        opplysninger.somListe().joinToString("\n") {
                                            """
                                            - ${it.opplysningstype.navn} : ${it.verdi} (${it.gyldighetsperiode.fraOgMed} - ${it.gyldighetsperiode.tilOgMed}))
                                            """.trimIndent()
                                        }
                                    }",
                            ),
                    )
                }
            }
        }
    }
}

private fun simuleringsdata(beregningRequestDTO: BeregningRequestDTO): Opplysninger {
    val opplysninger = mutableListOf<Opplysning<*>>()

    val stønadsperiodeFom = beregningRequestDTO.stønadsperiode.fom
    val meldekortFom = beregningRequestDTO.meldekortFom ?: stønadsperiodeFom
    val antDager = beregningRequestDTO.antallMeldekortdager?.toLong() ?: 14
    val antUker = beregningRequestDTO.stønadsperiode.uker
    val meldekortTom = meldekortFom.plusDays(antDager - 1)
    val terskel =
        beregningRequestDTO.terskel.map { terskel ->
            Faktum(
                kravTilArbeidstidsreduksjon,
                terskel.verdi,
                Gyldighetsperiode(terskel.fom ?: stønadsperiodeFom, terskel.tom ?: LocalDate.MAX),
            )
        }

    val sats =
        beregningRequestDTO.dagsats.map { sats ->
            Faktum(
                dagsatsEtterSamordningMedBarnetillegg,
                Beløp(sats.verdi),
                Gyldighetsperiode(sats.fom ?: stønadsperiodeFom, sats.tom ?: LocalDate.MAX),
            )
        }
    opplysninger.add(Faktum(harLøpendeRett, true, Gyldighetsperiode(stønadsperiodeFom, LocalDate.MAX)))
    opplysninger.addAll(terskel)
    opplysninger.add(Faktum(egenandel, Beløp(beregningRequestDTO.egenandel)))
    opplysninger.addAll(sats)
    opplysninger.add(Faktum(fastsattVanligArbeidstid, beregningRequestDTO.fastsattVanligArbeidstid))
    opplysninger.add(Faktum(ordinærPeriode, antUker))
    opplysninger.add(Faktum(antallStønadsdager, antUker * 5))
    opplysninger.add(Faktum(Beregning.meldeperiode, Periode(meldekortFom, meldekortTom)))

    val meldekortdager =
        beregningRequestDTO.dager
            .map { it ->
                Dag(
                    dato = it.dato,
                    aktiviteter =
                        it.aktiviteter.map { aktivitet ->
                            MeldekortAktivitet(
                                type = AktivitetType.valueOf(aktivitet.type.value),
                                timer = aktivitet.timer?.let { timer -> Duration.parseIsoString(timer) },
                            )
                        },
                    meldt = it.meldt,
                )
            }

    val meldekort =
        Meldekort(
            id = UUIDv7.ny(),
            meldingsreferanseId = UUIDv7.ny(),
            ident = "00000000000",
            eksternMeldekortId = MeldekortId("simulering-${UUIDv7.ny()}"),
            fom = meldekortFom,
            tom = meldekortTom,
            kilde = MeldekortKilde("SIMULERING", "00000000000"),
            dager = meldekortdager,
            innsendtTidspunkt = LocalDateTime.now(),
            korrigeringAv = null,
            meldedato = LocalDate.now(),
            kanSendesFra = meldekortTom.minusDays(1),
        )

    val meldkortOpplysning = meldekort.tilOpplysninger(Systemkilde(UUIDv7.ny(), LocalDate.now().atStartOfDay()))
    opplysninger.addAll(meldkortOpplysning)
    return opplysninger.somOpplysninger()
}

package no.nav.dagpenger.behandling.mediator.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.simulering.api.models.BeregningDTO
import no.nav.dagpenger.behandling.simulering.api.models.BeregningDagDTO
import no.nav.dagpenger.behandling.simulering.api.models.BeregningRequestDTO
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Kvotetelling
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon
import no.nav.dagpenger.regel.beregning.Beregning.forbruktEgenandel
import no.nav.dagpenger.regel.beregning.Beregningsperiode
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.ordinærPeriode
import no.nav.dagpenger.regel.fastsetting.Egenandel.egenandel
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.hendelse.tilOpplysninger
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import kotlin.time.Duration

private val logger = KotlinLogging.logger { }

class SimuleringsException(
    string: String,
    throwable: Throwable,
) : RuntimeException(string, throwable)

internal fun Application.simuleringApi() {
    routing {
        swaggerUI(path = "simulering/openapi", swaggerFile = "simulering-api.yaml")
        route("simulering") {
            put("beregning") {
                val beregningRequestDTO = call.receive<BeregningRequestDTO>()
                val (meldekortFom, meldekortTom, opplysninger) = simuleringsdata(beregningRequestDTO)

                val beregningsperiode =
                    try {
                        beregningsperiode(meldekortFom, meldekortTom, opplysninger).also {
                            Kvotetelling().ferdig(opplysninger)
                        }
                    } catch (e: Exception) {
                        throw SimuleringsException("Feil ved beregning av meldekortperiode", e)
                    }

                val forbruktEgenandel = opplysninger.finnAlle(forbruktEgenandel).sumOf { it.verdi }
                val beregning =
                    BeregningDTO(
                        forbruktEgenandel = forbruktEgenandel,
                        forbruktKvote = beregningsperiode.forbruksdager.size,
                        dager =
                            beregningsperiode.forbruksdager.map { dag ->
                                BeregningDagDTO(
                                    dato = dag.dato,
                                    dagsats = dag.sats.verdien.intValueExact(),
                                    forbruktEgenandel = dag.forbruktEgenandel.verdien,
                                    utbetalt = dag.avrundetUtbetaling,
                                    fastsattVanligArbeidstid = dag.fva.timer,
                                    timerArbeidet = dag.timerArbeidet.timer,
                                )
                            },
                    )

                call.respond(HttpStatusCode.OK, beregning)
            }
        }
    }
}

private fun beregningsperiode(
    meldekortFom: LocalDate,
    meldekortTom: LocalDate,
    opplysninger: Opplysninger,
): Beregningsperiode {
    val beregningsperiode =
        BeregningsperiodeFabrikk(
            meldekortFom,
            meldekortTom,
            opplysninger,
        ).lagBeregningsperiode()
    return beregningsperiode
}

private fun simuleringsdata(beregningRequestDTO: BeregningRequestDTO): Triple<LocalDate, LocalDate, Opplysninger> {
    val opplysningsliste = mutableListOf<Opplysning<*>>()
    val stønadsperiodeFom = beregningRequestDTO.stønadsperiode.fom
    val meldekortFom = beregningRequestDTO.meldekortFom ?: stønadsperiodeFom
    val antDager = beregningRequestDTO.antallMeldekortdager?.toLong() ?: 14
    val antUker = beregningRequestDTO.stønadsperiode?.uker ?: 52
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
    opplysningsliste.add(Faktum(harLøpendeRett, true, Gyldighetsperiode(stønadsperiodeFom, LocalDate.MAX)))
    opplysningsliste.addAll(terskel)
    opplysningsliste.add(Faktum(egenandel, Beløp(beregningRequestDTO.egenandel)))
    opplysningsliste.addAll(sats)
    opplysningsliste.add(Faktum(fastsattVanligArbeidstid, beregningRequestDTO.fastsattVanligArbeidstid))
    opplysningsliste.add(Faktum(ordinærPeriode, antUker))
    opplysningsliste.add(Faktum(antallStønadsdager, antUker * 5))

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

    val meldkortOpplysning = meldekortdager.tilOpplysninger(Systemkilde(UUIDv7.ny(), LocalDate.now().atStartOfDay()))
    opplysningsliste.addAll(meldkortOpplysning)
    val opplysninger = opplysningsliste.somOpplysninger()
    return Triple(meldekortFom, meldekortTom, opplysninger)
}

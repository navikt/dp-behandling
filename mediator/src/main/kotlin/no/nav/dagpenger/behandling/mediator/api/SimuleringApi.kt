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
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.simulering.api.models.BeregningDTO
import no.nav.dagpenger.behandling.simulering.api.models.BeregningDagDTO
import no.nav.dagpenger.behandling.simulering.api.models.BeregningRequestDTO
import no.nav.dagpenger.behandling.simulering.api.models.MeldekortDTOTypeDTO
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon
import no.nav.dagpenger.regel.beregning.Beregning.arbeidsdag
import no.nav.dagpenger.regel.beregning.Beregning.arbeidstimer
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.ordinærPeriode
import no.nav.dagpenger.regel.fastsetting.Egenandel.egenandel
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

internal fun Application.simuleringApi() {
    routing {
        swaggerUI(path = "simulering/openapi", swaggerFile = "simulering-api.yaml")
        route("simulering") {
            put("beregning") {
                val req = call.receive<BeregningRequestDTO>()
                val opplysningsliste = mutableListOf<Opplysning<*>>()
                val rettighetstatus = TemporalCollection<Rettighetstatus>()
                val stønadsperiodeFom = req.stonadsperiode.fom
                val meldekortFom = req.meldekortFom ?: stønadsperiodeFom
                val antDager = req.antallMeldekortdager?.toLong() ?: 14
                val antUker = req.stonadsperiode.uker?.toInt() ?: 52
                val meldekortTom = meldekortFom.plusDays(antDager - 1)
                val terskel =
                    req.terskel.map { terskel ->
                        Faktum(
                            kravTilArbeidstidsreduksjon,
                            terskel.verdi,
                            Gyldighetsperiode(terskel.fom ?: stønadsperiodeFom, terskel.tom ?: LocalDate.MAX),
                        )
                    }

                val sats =
                    req.sats.map { sats ->
                        Faktum(
                            dagsatsEtterSamordningMedBarnetillegg,
                            Beløp(sats.verdi),
                            Gyldighetsperiode(sats.fom ?: stønadsperiodeFom, sats.tom ?: LocalDate.MAX),
                        )
                    }
                rettighetstatus.put(
                    stønadsperiodeFom,
                    Rettighetstatus(
                        virkningsdato = stønadsperiodeFom,
                        utfall = true,
                        behandlingId = UUIDv7.ny(),
                    ),
                )
                opplysningsliste.addAll(terskel)
                opplysningsliste.add(Faktum(egenandel, Beløp(req.egenandel)))
                opplysningsliste.addAll(sats)
                opplysningsliste.add(Faktum(fastsattVanligArbeidstid, req.fva))
                opplysningsliste.add(Faktum(ordinærPeriode, antUker))
                opplysningsliste.add(Faktum(antallStønadsdager, antUker * 5))

                val meldekortDager =
                    req.meldekort
                        .mapIndexed { index, dag ->
                            val dagensDato = meldekortFom.plusDays(index.toLong())
                            when (dag.type) {
                                MeldekortDTOTypeDTO.ARBEIDSTIMER ->
                                    listOf(
                                        Faktum(arbeidstimer, dag.verdi, Gyldighetsperiode(dagensDato, dagensDato)),
                                        Faktum(arbeidsdag, true, Gyldighetsperiode(dagensDato, dagensDato)),
                                    )

                                MeldekortDTOTypeDTO.FRAVÆR ->
                                    listOf(
                                        Faktum(arbeidsdag, true, Gyldighetsperiode(dagensDato, dagensDato)),
                                    )

                                MeldekortDTOTypeDTO.SYKDOM ->
                                    listOf(
                                        Faktum(arbeidsdag, true, Gyldighetsperiode(dagensDato, dagensDato)),
                                    )
                            }
                        }.flatten()
                opplysningsliste.addAll(meldekortDager)

                val beregningsperiode =
                    BeregningsperiodeFabrikk(
                        meldekortFom,
                        meldekortTom,
                        opplysningsliste.somOpplysninger(),
                        rettighetstatus,
                    ).lagBeregningsperiode()

                val egenandel = opplysningsliste.find { it.opplysningstype == egenandel }!!.verdi as Beløp
                val forbruktEgenandel = beregningsperiode.forbruksdager.sumOf { it.forbruktEgenandel.verdien }.toDouble()
                val forbrukt = beregningsperiode.forbruksdager.sumOf { it.avrundetUtbetaling }

                val beregning =
                    BeregningDTO(
                        gjenståendeEgenandel = egenandel.verdien.toDouble() - forbruktEgenandel,
                        kvoteForbruk = beregningsperiode.forbruksdager.size,
                        dager =
                            beregningsperiode.forbruksdager.map { dag ->
                                BeregningDagDTO(
                                    dato = dag.dato,
                                    sats = dag.sats.verdien.toDouble(),
                                    forbruktEgenandel = dag.forbruktEgenandel.verdien.toDouble(),
                                    utbetalt = dag.avrundetUtbetaling,
                                    fva = dag.fva.timer,
                                    timerArbeidet = dag.timerArbeidet.timer,
                                )
                            },
                    )

                call.respond(HttpStatusCode.OK, beregning)
            }
        }
    }
}

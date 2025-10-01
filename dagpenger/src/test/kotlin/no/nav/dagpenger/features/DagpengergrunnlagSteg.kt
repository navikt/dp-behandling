package no.nav.dagpenger.features

import io.cucumber.datatable.DataTable
import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.DagpengergrunnlagSteg.Månedsinntekt.Companion.finnSisteAvsluttendeKalenderMåned
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.Virkningstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.uavrundetGrunnlag
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagHvisVerneplikt
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.dagpenger.inntekt.v1.Inntekt as InntektV1

class DagpengergrunnlagSteg : No {
    private val fraDato = 10.mai(2021)
    private val regelsett = RegelverkDagpenger.regelsettFor(uavrundetGrunnlag)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søknadsdato for dagpenger er {dato}") { søknadsdato: LocalDate ->
            opplysninger.leggTil(Faktum(prøvingsdato, søknadsdato)).also { regelkjøring.evaluer() }

            opplysninger.leggTil(Faktum(grunnlagHvisVerneplikt, Beløp(0.0)))
        }
        Gitt("at verneplikt for grunnlag er satt {boolsk}") { verneplikt: Boolean ->
            opplysninger.leggTil(Faktum(oppfyllerKravetTilVerneplikt, verneplikt)).also { regelkjøring.evaluer() }
        }

        Gitt("at inntekt for grunnlag er") { dataTable: DataTable? ->
            val inntektstabell = dataTable!!.asMaps()
            val f = Inntekt(lagInntekt(inntektstabell))
            opplysninger.leggTil(Faktum(Minsteinntekt.inntektFraSkatt, f))
            val rapport = regelkjøring.evaluer()
            println(rapport)
        }

        Så("beregnet uavrundet grunnlag være {string}") { uavrundetGrunnlag: String ->
            opplysninger.finnOpplysning(Dagpengegrunnlag.uavrundetGrunnlag).verdi shouldBe Beløp(BigDecimal(uavrundetGrunnlag))
        }

        Så("beregnet grunnlag være {string}") { grunnlag: String ->
            opplysninger.finnOpplysning(Dagpengegrunnlag.grunnlag).verdi shouldBe Beløp(grunnlag.toBigDecimal())
        }

        Så("uavkortet {string}") { uavkortet: String ->
            listOf(
                opplysninger.finnOpplysning(Dagpengegrunnlag.uavkortet12mnd).verdi,
                opplysninger.finnOpplysning(Dagpengegrunnlag.uavkortet36mnd).verdi,
            ).shouldContain(Beløp(uavkortet.toBigDecimal()))
        }

        Og("vi har ikke avkortet") {
            opplysninger.finnOpplysning(Dagpengegrunnlag.harAvkortet).verdi shouldBe false
        }
        Og("vi har avkortet") {
            opplysninger.finnOpplysning(Dagpengegrunnlag.harAvkortet).verdi shouldBe true
        }

        Og("beregningsregel er {string}") { bruktBeregningsregel: String ->
            opplysninger.finnOpplysning(Dagpengegrunnlag.bruktBeregningsregel).verdi shouldBe bruktBeregningsregel
        }
    }

    private fun lagInntekt(inntekt: MutableList<MutableMap<String, String>>): InntektV1 {
        val inntekter = inntekt.map { Månedsinntekt(it) }
        val perMåned = inntekter.groupBy { it.periode }
        return InntektV1(
            inntektsId = UUID.randomUUID().toString(),
            inntektsListe =
                perMåned.map { (periode, månedsinntekter) ->
                    KlassifisertInntektMåned(
                        årMåned = periode,
                        klassifiserteInntekter =
                            månedsinntekter.map {
                                KlassifisertInntekt(
                                    beløp = it.beløp,
                                    inntektKlasse = it.inntektsklasse,
                                )
                            },
                    )
                },
            sisteAvsluttendeKalenderMåned = inntekter.finnSisteAvsluttendeKalenderMåned(),
        )
    }

    private data class Månedsinntekt(
        val periode: YearMonth,
        val beløp: BigDecimal,
        val inntektsklasse: InntektKlasse,
    ) {
        constructor(verdier: Map<String, String>) : this(
            verdier["Periode"]!!.let { YearMonth.parse(it) },
            verdier["Beløp"]!!.toBigDecimal(),
            verdier["Inntektsklasse"]!!.let { InntektKlasse.valueOf(it) },
        )

        companion object {
            fun List<Månedsinntekt>.finnSisteAvsluttendeKalenderMåned() = this.maxByOrNull { it.periode }!!.periode
        }
    }
}

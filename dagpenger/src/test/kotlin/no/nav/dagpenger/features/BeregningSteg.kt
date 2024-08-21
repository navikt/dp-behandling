package no.nav.dagpenger.features

import io.cucumber.datatable.DataTable
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.beregning.Beregning.arbeidsdag
import no.nav.dagpenger.regel.beregning.Beregning.terskel
import no.nav.dagpenger.regel.beregning.Beregningsperiode
import no.nav.dagpenger.regel.fastsetting.DagpengensStørrelse.sats
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsuker
import java.time.LocalDate

class BeregningSteg : No {
    private val opplysninger = mutableListOf<Opplysning<*>>()
    private lateinit var meldeperiodeFraOgMed: LocalDate

    init {
        Gitt("at terskel er satt til {double}") { terskelVerdi: Double ->
            opplysninger.add(Faktum(terskel, terskelVerdi))
        }
        Gitt("at mottaker har vedtak med") { dataTable: DataTable? ->
            val vedtakstabell = dataTable!!.asMaps()
            opplysninger.addAll(lagVedtak(vedtakstabell))
        }
        Når("meldekort for periode som begynner fra og med {dato} mottas med") { fraOgMed: LocalDate, dataTable: DataTable? ->
            val dager = dataTable!!.asMaps()
            meldeperiodeFraOgMed = fraOgMed
            opplysninger.addAll(lagMeldekort(fraOgMed, dager))
        }
        Så("skal det utbetales {double} kroner") { utbetaling: Double ->
            val opplysninger = Opplysninger(opplysninger)
            val beregning = Beregningsperiode.fraOpplysninger(meldeperiodeFraOgMed, opplysninger)
            beregning.utbetaling shouldBe utbetaling
        }
    }

    private fun lagVedtak(vedtakstabell: List<MutableMap<String, String>>): List<Opplysning<*>> =
        vedtakstabell.map {
            val factory = opplysningFactory(it)
            factory(it)
        }

    private fun lagMeldekort(
        fraOgMed: LocalDate,
        dager: MutableList<MutableMap<String, String>>,
    ): List<Opplysning<*>> {
        require(dager.size == 14) { "Må ha nøyaktig 14 dager" }
        val opplysninger =
            dager.mapIndexed { i, dag ->
                val dato = fraOgMed.plusDays(i.toLong())
                val timer = dag["verdi"]?.toInt() ?: 0
                Faktum(arbeidsdag, timer, Gyldighetsperiode(dato, dato))
            }
        return opplysninger
    }

    private val opplysningFactories: Map<String, (Map<String, String>) -> Opplysning<*>> =
        mapOf(
            "Periode" to { args ->
                Faktum(antallStønadsuker, args["verdi"]!!.toInt())
            },
            "Sats" to { args ->
                Faktum(sats, Beløp(args["verdi"]!!.toInt()))
            },
            "FVA" to { args ->
                Faktum(fastsattVanligArbeidstid, args["verdi"]!!.toDouble())
            },
        )

    private fun opplysningFactory(it: Map<String, String>): (Map<String, String>) -> Opplysning<*> {
        val opplysningstype = it["Opplysning"]!!
        return opplysningFactories[opplysningstype]
            ?: throw IllegalArgumentException("Ukjent opplysningstype: $opplysningstype")
    }
}

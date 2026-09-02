package no.nav.dagpenger.regel.regelverk

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Utbetaling
import no.nav.dagpenger.opplysning.Utbetalingsberegning
import no.nav.dagpenger.opplysning.Ytelsestype
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger

object Utbetalinger : Utbetalingsberegning {
    override fun utbetalinger(opplysninger: LesbarOpplysninger): List<Utbetaling> {
        val meldeperioder = opplysninger.finnAlle(Beregning.meldeperiode)

        val egneId = opplysninger.somListe(LesbarOpplysninger.Filter.Egne).map { it.id }
        val løpendeRett = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
        val satser = opplysninger.finnAlle(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg)
        val dager = opplysninger.finnAlle(Beregning.utbetaling).associateBy { it.gyldighetsperiode.fraOgMed }

        return meldeperioder.flatMap { periode ->
            periode.verdi.mapNotNull { dato ->
                if (løpendeRett.filter { it.verdi }.none { it.gyldighetsperiode.inneholder(dato) }) {
                    return@mapNotNull null
                }

                val dag = dager[dato] ?: throw IllegalStateException("Mangler utbetaling for dag $dato")
                val sats = satser.first { it.gyldighetsperiode.inneholder(dato) }.verdi
                Utbetaling(
                    meldeperiode = periode.verdi.hashCode().toString(),
                    dato = dato,
                    sats = sats.verdien.toInt(),
                    utbetaling = dag.verdi.heleKroner.toInt(),
                    endret = (dag.id in egneId),
                    ytelsestype = Ytelsestype("Ordinær"),
                )
            }
        }
    }
}

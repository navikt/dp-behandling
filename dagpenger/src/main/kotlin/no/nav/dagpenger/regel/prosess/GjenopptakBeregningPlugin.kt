package no.nav.dagpenger.regel.prosess

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger

/**
 * Reberegner de(n) meldeperioden(e) som overlapper en ny/endret rettighetsperiode
 * ved gjenopptak, slik at Beregning.antallStønadsdager/forbruk - og dermed
 * Gjenopptak.sisteGjenståendeDager/harGjenstående/skalGjenopptas - reflekterer
 * gjenopptaket i stedet for å stoppe ved den gamle (stansede) beregningen.
 *
 * Se mediator/src/test/kotlin/no/nav/dagpenger/scenario/GjenopptakTest.kt,
 * testen "tester gjenopptak med innvilgelse etter stans der deler av meldekortet
 * dekker perioden en skal ha rett", for forventet oppførsel.
 *
 * Modellert etter [OmgjøringBeregningPlugin], men reberegner KUN meldeperioder
 * som faktisk overlapper den nye rettighetsperioden - ikke alle meldeperioder slik
 * omgjøring gjør.
 */
class GjenopptakBeregningPlugin(
    private val meldekortBeregningPlugin: MeldekortBeregningPlugin,
) : ProsessPlugin,
    Aktivitetskontekst {
    @WithSpan("GjenopptakBeregningPlugin.regelkjøringFerdig")
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        kontekst.kontekst(this)

        val nyeRettighetsperioder = kontekst.opplysninger.kunEgne.finnAlle(KravPåDagpenger.harLøpendeRett)
        val meldeperioderSomSkalReberegnes =
            kontekst.opplysninger
                .finnAlle(Beregning.meldeperiode)
                .filter { meldeperiode ->
                    nyeRettighetsperioder.any { it.gyldighetsperiode.overlapper(meldeperiode.gyldighetsperiode) }
                }.sortedBy { it.gyldighetsperiode.fraOgMed }

        Span.current().setAttribute("antallMeldeperioder", meldeperioderSomSkalReberegnes.size.toLong())
        val melding =
            "Start re-beregning for ${meldeperioderSomSkalReberegnes.size} meldeperioder ved gjenopptak. Perioder: ${
                meldeperioderSomSkalReberegnes.joinToString {
                    it.gyldighetsperiode
                        .toString()
                }
            }"
        kontekst.info(melding)
        meldeperioderSomSkalReberegnes.forEach { meldeperiode ->
            meldekortBeregningPlugin.beregnForPeriode(kontekst, meldeperiode.verdi)
        }
    }

    override fun toSpesifikkKontekst() = SpesifikkKontekst("GjenopptakBeregningPlugin")
}

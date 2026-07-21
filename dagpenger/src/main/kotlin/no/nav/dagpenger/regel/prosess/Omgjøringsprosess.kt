package no.nav.dagpenger.regel.prosess

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysninger.Companion.sisteEndring
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.prosess.PeriodeOverskrivingsStrategi.Companion.OVERSKRIV_ALLTID
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage.OmgjøringUtenKlageKontroll
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlageValg.SkalOmgjøringUtenKlageVurderesKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regelverk.hendelseTypeOpplysningstype
import java.time.LocalDate

class Omgjøringsprosess : Forretningsprosess(RegelverkDagpenger) {
    private val meldekortBeregningPlugin = MeldekortBeregningPlugin(regelverk.kvoter())

    init {
        registrer(RettighetsperiodePlugin(this.regelverk, OVERSKRIV_ALLTID))
        // TODO: Sjekk at dette faktisk er lurt
        // registrer(PrøvingsdatoPlugin())
        registrer(OmgjøringBeregningPlugin(meldekortBeregningPlugin))
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val førsteDagMedRett = innvilgelsesdato(opplysninger)
        val meldeperioder = opplysninger.finnAlle(Beregning.meldeperiode)

        // Finn den siste beregnede meldeperioden for å sette sluttdato for regelkjøringen
        val sisteMeldeperiode =
            meldeperioder.maxByOrNull { it.verdi.tilOgMed }?.verdi?.tilOgMed
                // Mangler det meldeperioder prøver vi bare vilkår på nytt for innvilgelsesdatoen
                ?: førsteDagMedRett

        val sistehendelseDato = opplysninger.kunEgne.finnAlle(hendelseTypeOpplysningstype).maxOf { it.gyldighetsperiode.tilOgMed }
        val sisteEndring = opplysninger.somListe(Egne).sisteEndring()

        val regelkjøringSluttDato = maxOf(sisteMeldeperiode, sistehendelseDato, sisteEndring)

        return Regelkjøring(
            regelverksdato = førsteDagMedRett,
            prøvingsperiode = Regelkjøring.Periode(start = førsteDagMedRett, endInclusive = regelkjøringSluttDato),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter() = listOf(OmgjøringUtenKlageKontroll, SkalOmgjøringUtenKlageVurderesKontroll)

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
        val meldeperioder = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
        return meldeperioder.minOf { it.gyldighetsperiode.fraOgMed }
    }

    private fun innvilgelsesdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.finnAlle(Søknadstidspunkt.prøvingsdato).minOf { it.verdi }
}

/**
 * Plugin som kjører beregning for alle meldeperioder ved omgjøring.
 */
class OmgjøringBeregningPlugin(
    private val meldekortBeregningPlugin: MeldekortBeregningPlugin,
) : ProsessPlugin,
    Aktivitetskontekst {
    @WithSpan("OmgjøringBeregningPlugin.regelkjøringFerdig")
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        kontekst.kontekst(this)
        val opplysninger = kontekst.opplysninger
        val meldeperioder =
            opplysninger
                .finnAlle(Beregning.meldeperiode)
                .map { it.verdi }
                .sortedBy { it.fraOgMed }

        Span.current().setAttribute("antallMeldeperioder", meldeperioder.size.toLong())

        kontekst.info("Start re-beregning for ${meldeperioder.size} meldeperioder ved omgjøring.")
        meldeperioder.forEach { periode ->
            meldekortBeregningPlugin.beregnForPeriode(kontekst, periode)
        }
    }

    override fun toSpesifikkKontekst() = SpesifikkKontekst("OmgjøringBeregningPlugin")
}

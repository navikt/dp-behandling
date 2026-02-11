package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.PeriodeOverskrivingsStrategi.Companion.OVERSKRIV_ALLTID
import no.nav.dagpenger.regel.beregning.Beregning
import java.time.LocalDate

class Omgjøringsprosess : Forretningsprosess(RegelverkDagpenger) {
    private val meldekortBeregningPlugin = MeldekortBeregningPlugin()
    private val kvotetelling = Kvotetelling()

    init {
        registrer(RettighetsperiodePlugin(this.regelverk, OVERSKRIV_ALLTID))
        registrer(OmgjøringBeregningPlugin(meldekortBeregningPlugin, kvotetelling))
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val førsteDagMedRett = innvilgelsesdato(opplysninger)
        val meldeperioder = opplysninger.finnAlle(Beregning.meldeperiode)

        // Finn den siste beregnede meldeperioden for å sette sluttdato for regelkjøringen
        val sisteMeldeperiode =
            meldeperioder.maxByOrNull { it.verdi.tilOgMed }?.verdi?.tilOgMed
                // Mangler det meldeperioder prøver vi bare vilkår på nytt for innvilgelsesdatoen
                ?: førsteDagMedRett

        return Regelkjøring(
            regelverksdato = førsteDagMedRett,
            prøvingsperiode = Regelkjøring.Periode(start = førsteDagMedRett, endInclusive = sisteMeldeperiode),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter() = emptyList<Kontrollpunkt>()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
        val meldeperioder = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
        return meldeperioder.minOf { it.gyldighetsperiode.fraOgMed }
    }

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }

    private fun innvilgelsesdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.finnAlle(Søknadstidspunkt.prøvingsdato).minOf { it.gyldighetsperiode.fraOgMed }
}

/**
 * Plugin som kjører beregning for alle meldeperioder ved omgjøring.
 */
class OmgjøringBeregningPlugin(
    private val meldekortBeregningPlugin: MeldekortBeregningPlugin,
    private val kvotetelling: Kvotetelling,
) : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        val opplysninger = kontekst.opplysninger
        val meldeperioder =
            opplysninger
                .finnAlle(Beregning.meldeperiode)
                .map { it.verdi }
                .sortedBy { it.fraOgMed }

        // Kjør beregning for hver meldeperiode i kronologisk rekkefølge
        meldeperioder.forEach { periode ->
            meldekortBeregningPlugin.beregnForPeriode(kontekst, periode)
        }

        // Kjør kvotetelling etter at alle perioder er beregnet
        kvotetelling.regelkjøringFerdig(kontekst)
    }
}

package no.nav.dagpenger.regel.prosess
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.prosess.PeriodeOverskrivingsStrategi.Companion.OVERSKRIV_ALLTID
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage.OmgjøringUtenKlageKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regelverk.hendelseTypeOpplysningstype
import java.time.LocalDate

class Omgjøringsprosess : Forretningsprosess(RegelverkDagpenger) {
    private val meldekortBeregningPlugin = MeldekortBeregningPlugin()
    private val kvotetelling = Kvotetelling()

    init {
        registrer(RettighetsperiodePlugin(this.regelverk, OVERSKRIV_ALLTID))
        // TODO: Sjekk at dette faktisk er lurt
        // registrer(PrøvingsdatoPlugin())
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

        val sistehendelseDato = opplysninger.kunEgne.finnAlle(hendelseTypeOpplysningstype).maxOf { it.gyldighetsperiode.tilOgMed }
        val regelkjøringSluttDato = maxOf(sisteMeldeperiode, sistehendelseDato)

        return Regelkjøring(
            regelverksdato = førsteDagMedRett,
            prøvingsperiode = Regelkjøring.Periode(start = førsteDagMedRett, endInclusive = regelkjøringSluttDato),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter() = listOf(OmgjøringUtenKlageKontroll)

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
        val meldeperioder = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
        return meldeperioder.minOf { it.gyldighetsperiode.fraOgMed }
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
) : ProsessPlugin,
    Aktivitetskontekst {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        kontekst.kontekst(this)
        val opplysninger = kontekst.opplysninger
        val meldeperioder =
            opplysninger
                .finnAlle(Beregning.meldeperiode)
                .map { it.verdi }
                .sortedBy { it.fraOgMed }

        // Kjør beregning for hver meldeperiode i kronologisk rekkefølge

        kontekst.info("Start re-beregning for ${meldeperioder.size} meldeperioder ved omgjøring.")
        meldeperioder.forEach { periode ->
            kontekst.info("Reberegner meldeperiode: $periode")
            meldekortBeregningPlugin.beregnForPeriode(kontekst, periode)
        }

        // Kjør kvotetelling etter at alle perioder er beregnet
        kvotetelling.regelkjøringFerdig(kontekst)
    }

    override fun toSpesifikkKontekst() = SpesifikkKontekst("OmgjøringBeregningPlugin")
}

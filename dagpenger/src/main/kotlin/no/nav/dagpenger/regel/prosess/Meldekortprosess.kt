package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import java.time.LocalDate

class Meldekortprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(AlderskravPlugin())
        registrer(RettighetsperiodePlugin(this.regelverk))
        registrer(MeldekortBeregningPlugin(regelverk.kvoter()))
        registrer(TaptArbeidstidStans())
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val meldeperiode = meldeperiode(opplysninger)
        val innvilgelsesdato = innvilgelsesdato(opplysninger).maxBy { it <= meldeperiode.fraOgMed }
        val førsteDagMedRett = maxOf(innvilgelsesdato, meldeperiode.fraOgMed)

        return Regelkjøring(
            regelverksdato = innvilgelsesdato,
            prøvingsperiode = Regelkjøring.Periode(start = førsteDagMedRett, endInclusive = meldeperiode.tilOgMed),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter(): List<IKontrollpunkt> =
        listOf(
            Alderskrav.StansAlderKontroll,
            Beregning.OverTerskelKontroll,
        )

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = meldeperiode(opplysninger).tilOgMed

    private fun innvilgelsesdato(opplysninger: LesbarOpplysninger): List<LocalDate> =
        opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett).filter { it.verdi }.map { it.gyldighetsperiode.fraOgMed }

    private fun meldeperiode(opplysninger: LesbarOpplysninger): Periode = opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).verdi
}

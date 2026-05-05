package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.Kvoteteller
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.TidsbegrensetBortfall
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import java.time.LocalDate

class Meldekortprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(AlderskravPlugin())
        registrer(RettighetsperiodePlugin(this.regelverk))
        registrer(MeldekortBeregningPlugin())
        registrer(stønadsdagKvotetelling())
        registrer(bortfallKvotetelling())
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

fun stønadsdagKvotetelling() =
    Kvoteteller(
        kapasitet = antallStønadsdager,
        forbrukKriterium = forbruk,
        forbruktTeller = Beregning.forbrukt,
        gjenstående = Beregning.gjenståendeDager,
        sisteDagMedForbruk = Beregning.sisteForbruksdag,
        sisteGjenstående = Beregning.sisteGjenståendeDager,
    )
fun bortfallKvotetelling() =
    Kvoteteller(
        kapasitet = TidsbegrensetBortfall.antallBortfallsdager,
        forbrukKriterium = Beregning.erBortfallsdag,
        forbruktTeller = Beregning.forbruktBortfallsdager,
        gjenstående = Beregning.gjenståendeBortfallsdager,
        sisteDagMedForbruk = Beregning.sisteBortfallsdagMedForbruk,
        sisteGjenstående = Beregning.sisteGjenståendeBortfallsdager,
    )

package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsdager
import java.time.LocalDate

class Meldekortprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(MeldekortBeregningPlugin())
        registrer(stønadsdagKvotetelling())
        registrer(bortfallKvotetelling())
        registrer(RettighetsperiodePlugin(this.regelverk))
        registrer(TaptArbeidstidStans())
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val innvilgelsesdato = innvilgelsesdato(opplysninger)
        val meldeperiode = meldeperiode(opplysninger)
        val førsteDagMedRett = maxOf(innvilgelsesdato, meldeperiode.fraOgMed)

        return Regelkjøring(
            regelverksdato = innvilgelsesdato,
            prøvingsperiode = Regelkjøring.Periode(start = førsteDagMedRett, endInclusive = meldeperiode.tilOgMed),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter() = emptyList<Kontrollpunkt>()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = meldeperiode(opplysninger).tilOgMed

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }

    private fun innvilgelsesdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi

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

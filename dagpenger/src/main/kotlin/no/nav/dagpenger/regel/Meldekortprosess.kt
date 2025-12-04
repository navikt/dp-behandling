package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.Beregning.forbrukt
import no.nav.dagpenger.regel.beregning.Beregning.gjenståendePeriode
import no.nav.dagpenger.regel.beregning.Beregning.utbetaling
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsdager
import java.time.LocalDate

class Meldekortprosess :
    Forretningsprosess(RegelverkDagpenger),
    ProsessPlugin {
    init {
        registrer(this)
        registrer(Kvotetelling())
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

    override fun start(opplysninger: Opplysninger) {
        val meldeperiode = meldeperiode(opplysninger)
        val resultat =
            BeregningsperiodeFabrikk(meldeperiode.fraOgMed, meldeperiode.tilOgMed, opplysninger)
                .lagBeregningsperiode()
                .resultat

        val gyldighetsperiode = Gyldighetsperiode(meldeperiode.fraOgMed, meldeperiode.tilOgMed)
        opplysninger.leggTil(Faktum(Beregning.forbruktEgenandel, resultat.forbruktEgenandel, gyldighetsperiode))
        opplysninger.leggTil(Faktum(Beregning.utbetalingForPeriode, resultat.utbetaling, gyldighetsperiode))
        opplysninger.leggTil(Faktum(Beregning.gjenståendeEgenandel, resultat.gjenståendeEgenandel, gyldighetsperiode))
        opplysninger.leggTil(
            Faktum(Beregning.oppfyllerKravTilTaptArbeidstidIPerioden, resultat.oppfyllerKravTilTaptArbeidstid, gyldighetsperiode),
        )

        val forbruksdager = resultat.forbruksdager
        meldeperiode
            .forEach { dato ->
                val forbruksdag = forbruksdager.singleOrNull { it.dag.dato.isEqual(dato) }
                val gyldighetsperiode = Gyldighetsperiode(dato, dato)

                opplysninger.leggTil(Faktum(forbruk, forbruksdag != null, gyldighetsperiode))
                opplysninger.leggTil(Faktum(utbetaling, forbruksdag?.tilUtbetaling ?: Beløp(0), gyldighetsperiode))
            }
    }

    private fun innvilgelsesdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi

    private fun meldeperiode(opplysninger: LesbarOpplysninger): Periode = opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).verdi
}

class Kvotetelling : ProsessPlugin {
    override fun ferdig(opplysninger: Opplysninger) {
        val innvilgetStønadsdager = opplysninger.finnOpplysning(antallStønadsdager).verdi

        val dager =
            opplysninger.kunEgne
                .finnAlle(forbruk)
                .sortedBy { it.gyldighetsperiode.fraOgMed }

        var utgangspunkt =
            opplysninger
                .finnAlle(forbrukt)
                .lastOrNull {
                    it.gyldighetsperiode.fraOgMed.isBefore(dager.first().gyldighetsperiode.fraOgMed)
                }?.verdi ?: 0

        dager.forEach {
            if (it.verdi) utgangspunkt++
            opplysninger.leggTil(Faktum(forbrukt, utgangspunkt, it.gyldighetsperiode))

            val gjenståendeDager = innvilgetStønadsdager - utgangspunkt
            // TODO: Dette må vi bygge inn ett annet sted.
            require(gjenståendeDager >= 0) { "Gjenstående dager kan ikke være negativt. Har $gjenståendeDager dager igjen" }

            opplysninger.leggTil(Faktum(gjenståendePeriode, gjenståendeDager, it.gyldighetsperiode))
        }
    }
}

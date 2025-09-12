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
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.Beregning.forbrukt
import no.nav.dagpenger.regel.beregning.Beregning.gjenstående
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

        // TODO: Vi trenger også en smartere måte å finne stansdato

        return Regelkjøring(
            regelverksdato = innvilgelsesdato,
            prøvingsperiode = Regelkjøring.Periode(start = førsteDagMedRett, endInclusive = meldeperiode.tilOgMed),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter() = emptyList<Kontrollpunkt>()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = false

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
                .beregn()

        opplysninger.leggTil(
            Faktum(
                Beregning.forbruktEgenandel,
                resultat.forbruktEgenandel,
                Gyldighetsperiode(meldeperiode.fraOgMed, meldeperiode.tilOgMed),
            ),
        )
        val forbruksdager = resultat.forbruksdager
        meldeperiode
            .forEach { dato ->
                val forbruksdag = forbruksdager.singleOrNull { it.dag.dato.isEqual(dato) }
                val gyldighetsperiode = Gyldighetsperiode(dato, dato)

                opplysninger.leggTil(Faktum(forbruk, forbruksdag != null, gyldighetsperiode))
                opplysninger.leggTil(Faktum(utbetaling, forbruksdag?.tilUtbetaling ?: 0, gyldighetsperiode))
            }
    }

    private fun innvilgelsesdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi

    private fun meldeperiode(opplysninger: LesbarOpplysninger): Periode = opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).verdi
}

class Kvotetelling : ProsessPlugin {
    override fun ferdig(opplysninger: Opplysninger) {
        val innvilgetStønadsdager = opplysninger.finnOpplysning(antallStønadsdager).verdi

        val dager = opplysninger.kunEgne.finnAlle(forbruk)
        var utgangspunkt =
            opplysninger
                .finnAlle(forbrukt)
                .lastOrNull {
                    it.gyldighetsperiode.fom.isBefore(dager.first().gyldighetsperiode.fom)
                }?.verdi ?: 0

        dager.forEach {
            if (it.verdi) utgangspunkt++
            opplysninger.leggTil(Faktum(forbrukt, utgangspunkt, it.gyldighetsperiode))

            val gjenståendeDager = innvilgetStønadsdager - utgangspunkt
            // TODO: Dette må vi bygge inn ett annet sted.
            require(gjenståendeDager >= 0) { "Gjenstående dager kan ikke være negativt. Har $gjenståendeDager dager igjen" }

            opplysninger.leggTil(Faktum(gjenstående, gjenståendeDager, it.gyldighetsperiode))
        }
    }
}

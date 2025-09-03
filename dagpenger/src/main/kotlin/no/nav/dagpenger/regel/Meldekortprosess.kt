package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Klatteland
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.RegistrertForretningsprosess
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.Beregning.utbetaling
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import java.time.LocalDate

class Meldekortprosess : RegistrertForretningsprosess() {
    override val regelverk = RegelverkDagpenger

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

    override fun regelsett() = regelverk.regelsett

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> = Beregning.regelsett.ønsketInformasjon

    override fun klatten(
        tilstand: Klatteland,
        opplysninger: Opplysninger,
    ) {
        val meldeperiode = meldeperiode(opplysninger)

        when (tilstand) {
            Klatteland.Start -> {
                val fabrikk = BeregningsperiodeFabrikk(meldeperiode.fraOgMed, meldeperiode.tilOgMed, opplysninger)
                val periode = fabrikk.lagBeregningsperiode()
                val forbruksdager = periode.forbruksdager

                meldeperiode
                    .forEach { dato ->
                        val forbruksdag = forbruksdager.singleOrNull { it.dato.isEqual(dato) }
                        val gyldighetsperiode = Gyldighetsperiode(dato, dato)
                        val tilUtbetaling = forbruksdag?.avrundetUtbetaling ?: 0
                        val forbruktEgenandel = forbruksdag?.forbruktEgenandel?.verdien?.toInt() ?: 0

                        // TODO: VI kan ikke gjøre det slik. Vi må finne en annen måte å si ifra på at det er forbruk (eksempel hvis det er sanksjon)
                        val erForbruk = tilUtbetaling > 0 || forbruktEgenandel > 0
                        opplysninger.leggTil(Faktum(forbruk, erForbruk, gyldighetsperiode))
                        opplysninger.leggTil(Faktum(utbetaling, tilUtbetaling, gyldighetsperiode))
                        opplysninger.leggTil(Faktum(Beregning.forbruktEgenandel, forbruktEgenandel, gyldighetsperiode))
                    }
            }

            else -> {}
        }
    }

    private fun innvilgelsesdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi

    private fun meldeperiode(opplysninger: LesbarOpplysninger): Periode = opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).verdi
}

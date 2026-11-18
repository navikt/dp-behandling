package no.nav.dagpenger.regel.prosess

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.RegelverkDagpenger
import java.time.LocalDate

class Stansprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val endringer = endringsperiode(opplysninger)
        val rettighetsperioder = rettighetsperioder(opplysninger)

        // Begrenser kjøringen til perioden der bruker har rett, slik at tidligere perioder ikke vurderes på nytt
        val første = maxOf(endringer.start, rettighetsperioder.minOf { it.fraOgMed })
        val siste = minOf(endringer.endInclusive, rettighetsperioder.maxOf { it.tilOgMed })

        logger.info { "Kjører regler i ${this.javaClass.simpleName} fra $første til $siste" }

        return Regelkjøring(
            regelverksdato = første,
            prøvingsperiode = Regelkjøring.Periode(start = første, endInclusive = siste),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    private fun endringsperiode(opplysninger: Opplysninger): ClosedRange<LocalDate> {
        val startdatoer =
            opplysninger
                .somListe(Egne)
                .filter { it.gyldighetsperiode.harStartdato }
                .map { it.gyldighetsperiode.fraOgMed }
        return startdatoer.min()..startdatoer.max()
    }

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger) =
        opplysninger.somListe(Egne).filter { it.gyldighetsperiode.harStartdato }.minOf { it.gyldighetsperiode.fraOgMed }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}

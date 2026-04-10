package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import java.time.LocalDate

class Stansprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val egne = opplysninger.somListe(Egne).filter { !it.gyldighetsperiode.fraOgMed.isEqual(LocalDate.MIN) }
        val første = egne.minOf { it.gyldighetsperiode.fraOgMed }
        val siste = egne.maxOf { it.gyldighetsperiode.fraOgMed }
        return Regelkjøring(
            regelverksdato = første,
            prøvingsperiode = Regelkjøring.Periode(start = første, endInclusive = siste),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter() = emptyList<Kontrollpunkt>()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger) =
        opplysninger.somListe(Egne).filter { !it.gyldighetsperiode.fraOgMed.isEqual(LocalDate.MIN) }.minOf { it.gyldighetsperiode.fraOgMed }

    override fun ønsketResultat(opplysninger: LesbarOpplysninger) =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }
}

package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.divisjon
import no.nav.dagpenger.opplysning.regel.hvis
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.høyesteAv
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object Dagpengeperiode {
    private val antallStønadsuker = Opplysningstype.somHeltall("Antall stønadsuker")
    private val gjenståendeStønadsdager = Opplysningstype.somHeltall("Antall gjenstående stønadsdager")
    val ordinærPeriode = Opplysningstype.somHeltall("Antall stønadsuker som gis ved ordinære dagpenger")
    private val dagerIUka = Opplysningstype.somHeltall("Antall dager som skal regnes med i hver uke")
    private val terskelFaktor12 = Opplysningstype.somDesimaltall("Terskelfaktor for 12 måneder")
    private val terskelFaktor36 = Opplysningstype.somDesimaltall("Terskelfaktor for 36 måneder")
    private val grunnbeløp = Minsteinntekt.grunnbeløp
    private val terskel12 = Opplysningstype.somBeløp("Terskel for 12 måneder")
    private val terskel36 = Opplysningstype.somBeløp("Terskel for 36 måneder")
    private val inntektSiste12 = Minsteinntekt.inntekt12
    private val inntektSiste36 = Minsteinntekt.inntekt36

    private val inntektSnittSiste36 = Opplysningstype.somBeløp("Snittinntekt siste 36 måneder")
    private val divisor = Opplysningstype.somDesimaltall("Divisior")

    private val langPeriode = Opplysningstype.somHeltall("Lang dagpengeperiode")
    private val kortPeriode = Opplysningstype.somHeltall("Kort dagpengeperiode")
    private val overterskel12 = Opplysningstype.somBoolsk("Over terskel for 12 måneder")
    private val overterskel36 = Opplysningstype.somBoolsk("Over terskel for 36 måneder")
    private val stønadsuker12 = Opplysningstype.somHeltall("Stønadsuker ved siste 12 måneder")
    private val stønadsuker36 = Opplysningstype.somHeltall("Stønadsuker ved siste 36 måneder")

    val regelsett =
        Regelsett("Dagpengeperiode") {
            regel(kortPeriode) { oppslag(prøvingsdato) { 52 } }
            regel(langPeriode) { oppslag(prøvingsdato) { 104 } }
            regel(terskelFaktor12) { oppslag(prøvingsdato) { 2.0 } }
            regel(terskelFaktor36) { oppslag(prøvingsdato) { 2.0 } }
            regel(divisor) { oppslag(prøvingsdato) { 3.0 } }
            regel(terskel12) { multiplikasjon(grunnbeløp, terskelFaktor12) }
            regel(terskel36) { multiplikasjon(grunnbeløp, terskelFaktor36) }
            regel(inntektSnittSiste36) { divisjon(inntektSiste36, divisor) }

            regel(overterskel12) { størreEnnEllerLik(inntektSiste12, terskel12) }
            regel(overterskel36) { størreEnnEllerLik(inntektSnittSiste36, terskel36) }

            regel(stønadsuker12) { hvisSannMedResultat(overterskel12, langPeriode, kortPeriode) }
            regel(stønadsuker36) { hvisSannMedResultat(overterskel36, langPeriode, kortPeriode) }

            regel(antallStønadsuker) { høyesteAv(stønadsuker12, stønadsuker36) }

            regel(ordinærPeriode) { hvis(Minsteinntekt.minsteinntekt, antallStønadsuker, 0) }

            regel(dagerIUka) { oppslag(prøvingsdato) { 5 } }
            regel(gjenståendeStønadsdager) { multiplikasjon(antallStønadsuker, dagerIUka) }
        }

    val ønsketResultat = listOf(ordinærPeriode, gjenståendeStønadsdager)
}

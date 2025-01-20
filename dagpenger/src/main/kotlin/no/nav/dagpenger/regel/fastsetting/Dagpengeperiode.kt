package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningsformål.Mellomsteg
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.regel.divisjon
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.høyesteAv
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.ordinærPeriode
import no.nav.dagpenger.regel.folketrygden

object Dagpengeperiode {
    private val dagerIUka = Opplysningstype.somHeltall("Antall dager som skal regnes med i hver uke", Mellomsteg, aldriSynlig)

    private val kortPeriode = Opplysningstype.somHeltall("Kort dagpengeperiode", Mellomsteg, aldriSynlig)
    private val langPeriode = Opplysningstype.somHeltall("Lang dagpengeperiode", Mellomsteg, aldriSynlig)
    private val terskelFaktor12 = Opplysningstype.somDesimaltall("Terskelfaktor for 12 måneder", Mellomsteg, aldriSynlig)
    private val terskelFaktor36 = Opplysningstype.somDesimaltall("Terskelfaktor for 36 måneder", Mellomsteg, aldriSynlig)
    private val divisor = Opplysningstype.somDesimaltall("Divisior", Mellomsteg, aldriSynlig)

    private val grunnbeløp = Minsteinntekt.grunnbeløp
    private val terskel12 = Opplysningstype.somBeløp("Terskel for 12 måneder", Mellomsteg, aldriSynlig)
    private val terskel36 = Opplysningstype.somBeløp("Terskel for 36 måneder", Mellomsteg, aldriSynlig)
    private val inntektSnittSiste36 = Opplysningstype.somBeløp("Snittinntekt siste 36 måneder")

    private val inntektSiste12 = Minsteinntekt.inntekt12
    private val inntektSiste36 = Minsteinntekt.inntekt36

    private val stønadsuker12 = Opplysningstype.somHeltall("Stønadsuker ved siste 12 måneder", Mellomsteg, aldriSynlig)
    private val stønadsuker36 = Opplysningstype.somHeltall("Stønadsuker ved siste 36 måneder", Mellomsteg, aldriSynlig)

    private val overterskel12 = Opplysningstype.somBoolsk("Over terskel for 12 måneder", Mellomsteg, aldriSynlig)
    private val overterskel36 = Opplysningstype.somBoolsk("Over terskel for 36 måneder", Mellomsteg, aldriSynlig)

    private val antallStønadsuker = Opplysningstype.somHeltall("Antall stønadsuker", Mellomsteg, aldriSynlig)
    private val gjenståendeStønadsdager = Opplysningstype.somHeltall("Antall gjenstående stønadsdager", Mellomsteg, aldriSynlig)

    private val ingenOrdinærPeriode =
        Opplysningstype.somHeltall("Stønadsuker når kravet til minste arbeidsinntekt ikke er oppfylt", Mellomsteg, aldriSynlig)

    val ordinærPeriode = Opplysningstype.somHeltall("Antall stønadsuker som gis ved ordinære dagpenger")

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 15, "Antall stønadsuker (stønadsperiode)", "4-15 Periode"),
            RegelsettType.Fastsettelse,
        ) {
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

            regel(ingenOrdinærPeriode) { oppslag(prøvingsdato) { 0 } }

            regel(ordinærPeriode) { hvisSannMedResultat(Minsteinntekt.minsteinntekt, antallStønadsuker, ingenOrdinærPeriode) }

            regel(dagerIUka) { oppslag(prøvingsdato) { 5 } }
            regel(gjenståendeStønadsdager) { multiplikasjon(antallStønadsuker, dagerIUka) }
        }

    val ønsketResultat = listOf(ordinærPeriode, gjenståendeStønadsdager)
}

package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.beløp
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.divisjon
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.høyesteAv
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.OpplysningsTyper.AntallStønadsukerId
import no.nav.dagpenger.regel.OpplysningsTyper.DagerIUkaId
import no.nav.dagpenger.regel.OpplysningsTyper.DivisiorId
import no.nav.dagpenger.regel.OpplysningsTyper.GjenståendeStønadsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.IngenOrdinærPeriodeId
import no.nav.dagpenger.regel.OpplysningsTyper.InntektSnittSiste36Id
import no.nav.dagpenger.regel.OpplysningsTyper.KortPeriodeId
import no.nav.dagpenger.regel.OpplysningsTyper.LangPeriodeId
import no.nav.dagpenger.regel.OpplysningsTyper.OrdinærPeriodeId
import no.nav.dagpenger.regel.OpplysningsTyper.Overterskel12Id
import no.nav.dagpenger.regel.OpplysningsTyper.Overterskel36Id
import no.nav.dagpenger.regel.OpplysningsTyper.Stønadsuker12Id
import no.nav.dagpenger.regel.OpplysningsTyper.Stønadsuker36Id
import no.nav.dagpenger.regel.OpplysningsTyper.Terskel12Id
import no.nav.dagpenger.regel.OpplysningsTyper.Terskel36Id
import no.nav.dagpenger.regel.OpplysningsTyper.TerskelFaktor12Id
import no.nav.dagpenger.regel.OpplysningsTyper.TerskelFaktor36Id
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.folketrygden
import no.nav.dagpenger.regel.kravPåDagpenger
import no.nav.dagpenger.regel.kravetTilMinsteinntektEllerVerneplikt

object Dagpengeperiode {
    private val dagerIUka = heltall(DagerIUkaId, "Antall dager som skal regnes med i hver uke", synlig = aldriSynlig)

    private val kortPeriode = heltall(KortPeriodeId, "Kort dagpengeperiode", synlig = aldriSynlig)
    private val langPeriode = heltall(LangPeriodeId, "Lang dagpengeperiode", synlig = aldriSynlig)
    private val terskelFaktor12 = desimaltall(TerskelFaktor12Id, "Terskelfaktor for 12 måneder", synlig = aldriSynlig)
    private val terskelFaktor36 = desimaltall(TerskelFaktor36Id, "Terskelfaktor for 36 måneder", synlig = aldriSynlig)
    private val divisor = desimaltall(DivisiorId, "Divisior", synlig = aldriSynlig)

    private val grunnbeløp = Minsteinntekt.grunnbeløp
    private val terskel12 = beløp(Terskel12Id, "Terskel for 12 måneder", synlig = aldriSynlig)
    private val terskel36 = beløp(Terskel36Id, "Terskel for 36 måneder", synlig = aldriSynlig)
    private val inntektSnittSiste36 = beløp(InntektSnittSiste36Id, "Snittinntekt siste 36 måneder", synlig = aldriSynlig)

    private val inntektSiste12 = Minsteinntekt.inntekt12
    private val inntektSiste36 = Minsteinntekt.inntekt36

    private val stønadsuker12 = heltall(Stønadsuker12Id, "Stønadsuker ved siste 12 måneder", synlig = aldriSynlig)
    private val stønadsuker36 = heltall(Stønadsuker36Id, "Stønadsuker ved siste 36 måneder", synlig = aldriSynlig)

    private val overterskel12 = boolsk(Overterskel12Id, "Over terskel for 12 måneder", synlig = aldriSynlig)
    private val overterskel36 = boolsk(Overterskel36Id, "Over terskel for 36 måneder", synlig = aldriSynlig)

    private val antallStønadsuker = heltall(AntallStønadsukerId, "Antall stønadsuker", synlig = aldriSynlig)
    private val gjenståendeStønadsdager = heltall(GjenståendeStønadsdagerId, "Antall gjenstående stønadsdager", synlig = aldriSynlig)

    private val ingenOrdinærPeriode =
        heltall(IngenOrdinærPeriodeId, "Stønadsuker når kravet til minste arbeidsinntekt ikke er oppfylt", synlig = aldriSynlig)

    val ordinærPeriode = heltall(OrdinærPeriodeId, "Antall stønadsuker som gis ved ordinære dagpenger")

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 15, "Antall stønadsuker (stønadsperiode)", "Periode"),
        ) {
            skalKjøres { kravPåDagpenger(it) }

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

            relevantHvis { kravetTilMinsteinntektEllerVerneplikt(it) }

            ønsketResultat(ordinærPeriode, gjenståendeStønadsdager)
        }
}

package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.regel.addisjon
import no.nav.dagpenger.opplysning.regel.avrund
import no.nav.dagpenger.opplysning.regel.divisjon
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.tekstId
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import java.math.BigDecimal
import java.time.LocalDate

object DagpengenesStørrelse {
    private val grunnlag = Dagpengegrunnlag.grunnlag

    // TODO: Hent antall barn fra søknaden. Hvordan skal saksbehandler godkjenne antall barn?
    internal val antallBarn = Opplysningstype.somHeltall("Antall barn")
    private val barnetilleggetsStørrelse = Opplysningstype.somBeløp("Barnetilleggets størrelse")

    /**
     * 1. Hente barn fra søknad
     * 2. Saksbehandler vilkårprøver at en har rett til barnetillegg per barn
     * 3. == antall barn * barnetillegg
     */
    private val dekningsgrad = Opplysningstype.somDesimaltall("Dekningsgrad")
    val dagsatsUtenBarnetillegg = Opplysningstype.somBeløp("Dagsats uten barnetillegg")
    val avrundetDagsUtenBarnetillegg = Opplysningstype.somBeløp("Avrundet dagsats uten barnetillegg")
    val avrundetDagsMedBarnetillegg =
        Opplysningstype.somBeløp(
            "Avrundet dagsats med barnetillegg".tekstId("opplysning.avrundet-dagsats-med-barnetillegg"),
        )
    val barnetillegg = Opplysningstype.somBeløp("Barnetillegg")
    val dagsatsMedBarn = Opplysningstype.somBeløp("Dagsats med barn")
    val ukessats = Opplysningstype.somBeløp("Ukessats".tekstId("opplysning.ukessats"))
    private val maksGrunnlag = Opplysningstype.somBeløp("Maks grunnlag for dagpenger")
    private val antallArbeidsdagerPerÅr = Opplysningstype.somHeltall("Antall arbeidsdager per år")
    private val arbeidsdagerPerUke = Opplysningstype.somHeltall("Antall arbeidsdager per uke")
    private val maksSats = Opplysningstype.somBeløp("Maks dagsats for dagpenger")
    private val nittiProsent = Opplysningstype.somDesimaltall("90% av grunnlag for dagpenger")
    val sats = Opplysningstype.somBeløp("Dagsats for dagpenger med barnetillegg")

    val regelsett =
        Regelsett("§ 4-12. Dagpengenes størrelse\n (Sats)") {
            regel(antallBarn) { oppslag(prøvingsdato) { 0 } } // TODO: Satt til 0 for testing av innvilgelse

            regel(barnetilleggetsStørrelse) { oppslag(prøvingsdato) { Barnetillegg.forDato(it) } }
            regel(dekningsgrad) {
                oppslag(prøvingsdato) { DagpengensStørrelseFaktor.forDato(it) }
            }
            regel(dagsatsUtenBarnetillegg) { multiplikasjon(grunnlag, dekningsgrad) }
            regel(barnetillegg) { multiplikasjon(barnetilleggetsStørrelse, antallBarn) }
            regel(dagsatsMedBarn) { addisjon(dagsatsUtenBarnetillegg, barnetillegg) }

            // Regne ut ukessats
            regel(arbeidsdagerPerUke) { oppslag(prøvingsdato) { 5 } }
            regel(nittiProsent) { oppslag(prøvingsdato) { 0.9 } }
            regel(antallArbeidsdagerPerÅr) { oppslag(prøvingsdato) { 260 } }

            regel(maksGrunnlag) { multiplikasjon(grunnlag, nittiProsent) }
            regel(maksSats) { divisjon(maksGrunnlag, antallArbeidsdagerPerÅr) }
            regel(sats) { minstAv(maksSats, dagsatsMedBarn) }

            regel(avrundetDagsMedBarnetillegg) { avrund(sats) }
            regel(avrundetDagsUtenBarnetillegg) { avrund(dagsatsUtenBarnetillegg) } // Arena trenger denne
            regel(ukessats) { multiplikasjon(avrundetDagsMedBarnetillegg, arbeidsdagerPerUke) }
        }

    val ønsketResultat = listOf(ukessats)
}

private object Barnetillegg {
    private val satser =
        TemporalCollection<BigDecimal>().apply {
            // Defineres her: https://lovdata.no/pro/#document/SF/forskrift/1998-09-16-890/%C2%A77-1
            put(LocalDate.MIN, BigDecimal(17))
            put(LocalDate.of(2023, 2, 1), BigDecimal(35))
            put(LocalDate.of(2024, 1, 1), BigDecimal(36))
        }

    fun forDato(regelverksdato: LocalDate) = Beløp(satser.get(regelverksdato))
}

private object DagpengensStørrelseFaktor {
    private val faktorer =
        TemporalCollection<Double>().apply {
            // Defineres her: https://lovdata.no/lov/1997-02-28-19/§4-12
            put(LocalDate.MIN, 0.0024)
        }

    fun forDato(regelverksdato: LocalDate) = faktorer.get(regelverksdato)
}

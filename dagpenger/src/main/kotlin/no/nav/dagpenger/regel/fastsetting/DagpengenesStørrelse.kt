package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.addisjon
import no.nav.dagpenger.opplysning.regel.antallAv
import no.nav.dagpenger.opplysning.regel.avrund
import no.nav.dagpenger.opplysning.regel.divisjon
import no.nav.dagpenger.opplysning.regel.erUlik
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.opplysning.regel.substraksjonTilNull
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Avklaringspunkter.BarnMåGodkjennes
import no.nav.dagpenger.regel.Behov.Barnetillegg
import no.nav.dagpenger.regel.SamordingUtenforFolketrygden.dagsatsSamordnetUtenforFolketrygden
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import java.math.BigDecimal
import java.time.LocalDate

object DagpengenesStørrelse {
    private val grunnlag = Dagpengegrunnlag.grunnlag

    val barn = Opplysningstype.somBarn("Barn".id(Barnetillegg))
    internal val antallBarn = Opplysningstype.somHeltall("Antall barn som gir rett til barnetillegg")
    internal val barnetilleggetsStørrelse = Opplysningstype.somBeløp("Barnetilleggets størrelse i kroner per dag for hvert barn")

    /**
     * 1. Hente barn fra søknad
     * 2. Saksbehandler vilkårprøver at en har rett til barnetillegg per barn
     * 3. == antall barn * barnetillegg
     */
    private val dekningsgrad = Opplysningstype.somDesimaltall("Faktor for utregning av dagsats etter dagpengegrunnlaget")
    val dagsatsUtenBarnetillegg = Opplysningstype.somBeløp("Dagsats uten barnetillegg før samordning")
    val ukessatMedBarnetillegg = Opplysningstype.somBeløp("Avrundet ukessats med barnetillegg før samordning")
    private val avrundetDagsatsUtenBarnetillegg = Opplysningstype.somBeløp("Avrundet dagsats uten barnetillegg før samordning")
    private val beløpOverMaks =
        Opplysningstype.somBeløp(
            "Andel av dagsats med barnetillegg som overstiger maks andel av dagpengegrunnlaget",
        )
    val dagsatsEtterNittiProsent =
        Opplysningstype.somBeløp(
            "Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget",
        )
    val barnetillegg = Opplysningstype.somBeløp("Sum av barnetillegg")
    private val dagsatsMedBarnetillegg = Opplysningstype.somBeløp("Dagsats med barnetillegg før samordning")
    private val nittiProsent = Opplysningstype.somDesimaltall("90% av grunnlag for dagpenger")
    private val antallArbeidsdagerPerÅr = Opplysningstype.somHeltall("Antall arbeidsdager per år")
    private val maksGrunnlag = Opplysningstype.somBeløp("Maksimalt mulig grunnlag avgrenset til 90% av dagpengegrunnlaget")
    val arbeidsdagerPerUke = Opplysningstype.somHeltall("Antall arbeidsdager per uke")
    private val maksSats = Opplysningstype.somBeløp("Maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget")
    private val avrundetMaksSats = Opplysningstype.somBeløp("Avrundet maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget")
    internal val harBarnetillegg = Opplysningstype.somBoolsk("Har barnetillegg")
    private val samordnetDagsatsMedBarnetillegg = Opplysningstype.somBeløp("Samordnet dagsats med barnetillegg")
    val ukessats = Opplysningstype.somBeløp("Ukessats med barnetillegg etter samordning")
    val dagsatsEtterSamordningMedBarnetillegg = Opplysningstype.somBeløp("Dagsats med barnetillegg etter samordning og 90% regel")
    val harSamordnet = Opplysningstype.somBoolsk("Har samordnet")

    val regelsett =
        Regelsett("§ 4-12. Dagpengenes størrelse\n (Sats)") {
            regel(barn) { innhentMed(søknadIdOpplysningstype) }
            regel(antallBarn) { antallAv(barn) { kvalifiserer } }

            // Regn ut dagsats uten barnetillegg, før samordning
            regel(dekningsgrad) { oppslag(prøvingsdato) { DagpengensStørrelseFaktor.forDato(it) } }
            regel(dagsatsUtenBarnetillegg) { multiplikasjon(grunnlag, dekningsgrad) }

            // Avrunder og sender over til samordning
            regel(avrundetDagsatsUtenBarnetillegg) { avrund(dagsatsUtenBarnetillegg) }

            // Regn ut barnetillegg
            regel(barnetilleggetsStørrelse) { oppslag(prøvingsdato) { BarnetilleggSats.forDato(it) } }
            regel(barnetillegg) { multiplikasjon(barnetilleggetsStørrelse, antallBarn) }

            // Regn ut dagsats med barnetillegg, før maks og samordning
            regel(dagsatsMedBarnetillegg) { addisjon(dagsatsUtenBarnetillegg, barnetillegg) }
            // Regn ut ukessats med barnetillegg som Arena trenger.
            regel(ukessatMedBarnetillegg) { multiplikasjon(dagsatsMedBarnetillegg, arbeidsdagerPerUke) }

            // Regn ut 90% av dagpengegrunnlaget
            regel(nittiProsent) { oppslag(prøvingsdato) { 0.9 } }
            regel(antallArbeidsdagerPerÅr) { oppslag(prøvingsdato) { 260 } }
            regel(maksGrunnlag) { multiplikasjon(grunnlag, nittiProsent) }
            regel(maksSats) { divisjon(maksGrunnlag, antallArbeidsdagerPerÅr) }
            regel(avrundetMaksSats) { avrund(maksSats) }

            // Finn beløp som overstiger maksimal mulig dagsats
            regel(beløpOverMaks) { substraksjonTilNull(dagsatsMedBarnetillegg, avrundetMaksSats) }
            regel(dagsatsEtterNittiProsent) { substraksjonTilNull(avrundetDagsatsUtenBarnetillegg, beløpOverMaks) }

            // Regn ut samordnet dagsats med barnetillegg, begrenset til 90% av dagpengegrunnlaget
            regel(samordnetDagsatsMedBarnetillegg) { addisjon(dagsatsSamordnetUtenforFolketrygden, barnetillegg) }
            regel(dagsatsEtterSamordningMedBarnetillegg) { minstAv(samordnetDagsatsMedBarnetillegg, avrundetMaksSats) }
            regel(harSamordnet) { erUlik(dagsatsEtterNittiProsent, dagsatsSamordnetUtenforFolketrygden) }

            // Regn ut ukessats
            regel(arbeidsdagerPerUke) { oppslag(prøvingsdato) { 5 } }
            regel(ukessats) { multiplikasjon(dagsatsEtterSamordningMedBarnetillegg, arbeidsdagerPerUke) }

            regel(harBarnetillegg) { størreEnnEllerLik(barnetillegg, barnetilleggetsStørrelse) }
        }

    val ønsketResultat = listOf(ukessats, dagsatsSamordnetUtenforFolketrygden, ukessatMedBarnetillegg, harSamordnet)

    val BarnetilleggKontroll =
        Kontrollpunkt(BarnMåGodkjennes) {
            it.har(barn) && it.finnOpplysning(barn).verdi.isNotEmpty()
        }
}

private object BarnetilleggSats {
    private val satser =
        TemporalCollection<BigDecimal>().apply {
            // Defineres her: https://lovdata.no/pro/#document/SF/forskrift/1998-09-16-890/%C2%A77-1
            put(LocalDate.MIN, BigDecimal(17))
            put(LocalDate.of(2023, 2, 1), BigDecimal(35))
            put(LocalDate.of(2024, 1, 1), BigDecimal(36))
            put(LocalDate.of(2025, 1, 1), BigDecimal(37))
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

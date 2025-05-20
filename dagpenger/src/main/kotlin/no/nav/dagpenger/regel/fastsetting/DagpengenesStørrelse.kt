package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål.Legacy
import no.nav.dagpenger.opplysning.Opplysningsformål.Register
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.beløp
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.dsl.fastsettelse
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
import no.nav.dagpenger.regel.OpplysningsTyper.AntallArbeidsdagerPerÅrId
import no.nav.dagpenger.regel.OpplysningsTyper.AntallBarnSomGirRettTilBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.ArbeidsdagerPerUkeId
import no.nav.dagpenger.regel.OpplysningsTyper.AvrundetDagsatsUtenBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.AvrundetMaksSatsId
import no.nav.dagpenger.regel.OpplysningsTyper.AvrundetUkessatsMedBarnetilleggFørSmordningId
import no.nav.dagpenger.regel.OpplysningsTyper.BarnId
import no.nav.dagpenger.regel.OpplysningsTyper.BarnetillegDekningsgradId
import no.nav.dagpenger.regel.OpplysningsTyper.BarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.BarnetilleggetsStørrelsePerDagId
import no.nav.dagpenger.regel.OpplysningsTyper.DagsatsEtterNittiProsentId
import no.nav.dagpenger.regel.OpplysningsTyper.DagsatsEtterSamordningMedBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.DagsatsMedBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.DagsatsUtenBarnetilleggFørSamordningId
import no.nav.dagpenger.regel.OpplysningsTyper.HarBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.HarSamordnetId
import no.nav.dagpenger.regel.OpplysningsTyper.MaksGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.MaksSatsId
import no.nav.dagpenger.regel.OpplysningsTyper.NittiProsentId
import no.nav.dagpenger.regel.OpplysningsTyper.SamordnetDagsatsMedBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.UkessatsId
import no.nav.dagpenger.regel.OpplysningsTyper.beløpOverMaksId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.dagsatsSamordnetUtenforFolketrygden
import no.nav.dagpenger.regel.folketrygden
import no.nav.dagpenger.regel.kravPåDagpenger
import java.math.BigDecimal
import java.time.LocalDate

object DagpengenesStørrelse {
    val barn = Opplysningstype.barn(BarnId, "Barn", Register, behovId = Barnetillegg)
    internal val antallBarn = heltall(AntallBarnSomGirRettTilBarnetilleggId, "Antall barn som gir rett til barnetillegg")
    internal val barnetilleggetsStørrelse =
        beløp(BarnetilleggetsStørrelsePerDagId, "Barnetilleggets størrelse i kroner per dag for hvert barn", synlig = aldriSynlig)

    /**
     * 1. Hente barn fra søknad
     * 2. Saksbehandler vilkårprøver at en har rett til barnetillegg per barn
     * 3. == antall barn * barnetillegg
     */
    private val dekningsgrad =
        desimaltall(BarnetillegDekningsgradId, "Faktor for utregning av dagsats etter dagpengegrunnlaget", synlig = aldriSynlig)
    val dagsatsUtenBarnetillegg =
        beløp(DagsatsUtenBarnetilleggFørSamordningId, "Dagsats uten barnetillegg før samordning", synlig = aldriSynlig)
    val ukesatsMedBarnetillegg =
        beløp(AvrundetUkessatsMedBarnetilleggFørSmordningId, "Avrundet ukessats med barnetillegg før samordning", Legacy, aldriSynlig)
    private val avrundetDagsatsUtenBarnetillegg =
        beløp(AvrundetDagsatsUtenBarnetilleggId, "Dagsats uten barnetillegg før samordning")
    private val beløpOverMaks =
        beløp(
            beløpOverMaksId,
            "Andel av dagsats med barnetillegg som overstiger maks andel av dagpengegrunnlaget",
            synlig = aldriSynlig,
        )
    val dagsatsEtterNittiProsent =
        beløp(
            DagsatsEtterNittiProsentId,
            "Andel av dagsats uten barnetillegg avkortet til maks andel av dagpengegrunnlaget",
            synlig = aldriSynlig,
        )
    val barnetillegg = beløp(BarnetilleggId, "Sum av barnetillegg", synlig = aldriSynlig)
    private val avrundetDagsatsMedBarnetillegg =
        beløp(DagsatsMedBarnetilleggId, "Dagsats med barnetillegg før samordning", synlig = aldriSynlig)
    private val nittiProsent = desimaltall(NittiProsentId, "90% av grunnlag for dagpenger", synlig = aldriSynlig)
    private val antallArbeidsdagerPerÅr = heltall(AntallArbeidsdagerPerÅrId, "Antall arbeidsdager per år", synlig = aldriSynlig)
    private val maksGrunnlag =
        beløp(MaksGrunnlagId, "Maksimalt mulig grunnlag avgrenset til 90% av dagpengegrunnlaget", synlig = aldriSynlig)
    val arbeidsdagerPerUke = heltall(ArbeidsdagerPerUkeId, "Antall arbeidsdager per uke", synlig = aldriSynlig)
    private val maksSats =
        beløp(MaksSatsId, "Maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget", synlig = aldriSynlig)
    private val avrundetMaksSats =
        beløp(AvrundetMaksSatsId, "Avrundet maksimal mulig dagsats avgrenset til 90% av dagpengegrunnlaget", synlig = aldriSynlig)
    internal val harBarnetillegg = boolsk(HarBarnetilleggId, "Har barnetillegg", synlig = aldriSynlig)
    private val samordnetDagsatsMedBarnetillegg = beløp(SamordnetDagsatsMedBarnetilleggId, "Samordnet dagsats med barnetillegg")
    val ukessats = beløp(UkessatsId, "Ukessats med barnetillegg etter samordning", Legacy, aldriSynlig)
    val dagsatsEtterSamordningMedBarnetillegg =
        beløp(DagsatsEtterSamordningMedBarnetilleggId, "Dagsats med barnetillegg etter samordning og 90 % regel")
    val harSamordnet = boolsk(HarSamordnetId, "Har samordnet")

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 12, "Dagpengenes størrelse", "Sats og barnetillegg"),
        ) {
            skalVurderes { kravPåDagpenger(it) }

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
            regel(avrundetDagsatsMedBarnetillegg) { addisjon(avrundetDagsatsUtenBarnetillegg, barnetillegg) }

            // Regn ut ukessats med barnetillegg som Arena trenger.
            regel(ukesatsMedBarnetillegg) { multiplikasjon(avrundetDagsatsMedBarnetillegg, arbeidsdagerPerUke) }

            // Regn ut 90% av dagpengegrunnlaget
            regel(nittiProsent) { oppslag(prøvingsdato) { 0.9 } }
            regel(antallArbeidsdagerPerÅr) { oppslag(prøvingsdato) { 260 } }
            regel(maksGrunnlag) { multiplikasjon(grunnlag, nittiProsent) }
            regel(maksSats) { divisjon(maksGrunnlag, antallArbeidsdagerPerÅr) }
            regel(avrundetMaksSats) { avrund(maksSats) }

            // Finn beløp som overstiger maksimal mulig dagsats
            regel(beløpOverMaks) { substraksjonTilNull(avrundetDagsatsMedBarnetillegg, avrundetMaksSats) }
            regel(dagsatsEtterNittiProsent) { substraksjonTilNull(avrundetDagsatsUtenBarnetillegg, beløpOverMaks) }

            // Regn ut samordnet dagsats med barnetillegg, begrenset til 90% av dagpengegrunnlaget
            regel(samordnetDagsatsMedBarnetillegg) { addisjon(dagsatsSamordnetUtenforFolketrygden, barnetillegg) }
            regel(dagsatsEtterSamordningMedBarnetillegg) { minstAv(samordnetDagsatsMedBarnetillegg, avrundetMaksSats) }
            regel(harSamordnet) { erUlik(dagsatsEtterNittiProsent, dagsatsSamordnetUtenforFolketrygden) }

            // Regn ut ukessats
            regel(arbeidsdagerPerUke) { oppslag(prøvingsdato) { 5 } }
            regel(ukessats) { multiplikasjon(dagsatsEtterSamordningMedBarnetillegg, arbeidsdagerPerUke) }

            regel(harBarnetillegg) { størreEnnEllerLik(barnetillegg, barnetilleggetsStørrelse) }

            avklaring(BarnMåGodkjennes)

            påvirkerResultat { kravPåDagpenger(it) }

            ønsketResultat(ukessats, dagsatsSamordnetUtenforFolketrygden, ukesatsMedBarnetillegg, harSamordnet)
        }

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

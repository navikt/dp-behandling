package no.nav.dagpenger.regel.regelsett.beregning

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.høyesteAv
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.regel.tomRegel
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstimerId
import no.nav.dagpenger.regel.OpplysningsTyper.erSanksjonsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.forbrukId
import no.nav.dagpenger.regel.OpplysningsTyper.forbruktBortfallsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.forbruktEgenandelId
import no.nav.dagpenger.regel.OpplysningsTyper.forbruktSanksjonsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.forbrukteDagerId
import no.nav.dagpenger.regel.OpplysningsTyper.gjenståendeBortfallsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.gjenståendeDagerId
import no.nav.dagpenger.regel.OpplysningsTyper.gjenståendeEgenandelId
import no.nav.dagpenger.regel.OpplysningsTyper.gjenståendeSanksjonsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.maksAntallPerioderMedIkkeTaptArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.meldedatoId
import no.nav.dagpenger.regel.OpplysningsTyper.meldeperiodeId
import no.nav.dagpenger.regel.OpplysningsTyper.meldtId
import no.nav.dagpenger.regel.OpplysningsTyper.prosentfaktorId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteBortfallsdagMedForbrukId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteDagMedForbrukId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteGjenståendeBortfallsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteGjenståendeDagerId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteGjenståendeSanksjonsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteSanksjonsdagMedForbrukId
import no.nav.dagpenger.regel.OpplysningsTyper.sumArbeidstimerId
import no.nav.dagpenger.regel.OpplysningsTyper.sumFvaId
import no.nav.dagpenger.regel.OpplysningsTyper.taptArbeidIPeriodenId
import no.nav.dagpenger.regel.OpplysningsTyper.terskelId
import no.nav.dagpenger.regel.OpplysningsTyper.trekkVedForsenMeldingId
import no.nav.dagpenger.regel.OpplysningsTyper.utbetalingForPeriodeId
import no.nav.dagpenger.regel.OpplysningsTyper.utbetalingId
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import java.time.LocalDate

object Beregning {
    val meldeperiode = Opplysningstype.periode(meldeperiodeId, "Meldeperiode")
    val arbeidsdag = Opplysningstype.boolsk(arbeidsdagId, "Arbeidsdag")
    val arbeidstimer = Opplysningstype.desimaltall(arbeidstimerId, "Arbeidstimer på en arbeidsdag", enhet = Enhet.Timer)

    val forbruk = Opplysningstype.boolsk(forbrukId, "Dag som fører til forbruk av dagpengeperiode")
    val meldt = Opplysningstype.boolsk(meldtId, "Har meldt seg via meldekort")

    val forbruktEgenandel = Opplysningstype.beløp(forbruktEgenandelId, "Forbrukt egenandel")
    val gjenståendeEgenandel = Opplysningstype.beløp(gjenståendeEgenandelId, "Gjenstående egenandel")

    val utbetaling = Opplysningstype.beløp(utbetalingId, "Penger som skal utbetales")
    val utbetalingForPeriode = Opplysningstype.beløp(utbetalingForPeriodeId, "Penger som skal utbetales for perioden")

    val forbrukt = Opplysningstype.heltall(forbrukteDagerId, "Antall dager som er forbrukt", enhet = Enhet.Dager)
    val gjenståendeDager = Opplysningstype.heltall(gjenståendeDagerId, "Antall dager som gjenstår", enhet = Enhet.Dager)

    val sisteForbruksdag = Opplysningstype.dato(sisteDagMedForbrukId, "Siste forbruksdato")
    val sisteGjenståendeDager = Opplysningstype.heltall(sisteGjenståendeDagerId, "Siste antall dager som gjenstår", enhet = Enhet.Dager)

    val erSanksjonsdag = Opplysningstype.boolsk(erSanksjonsdagId, "Dag med sanksjon av dagpenger")

    val forbruktSanksjonsdager =
        Opplysningstype.heltall(
            forbruktSanksjonsdagerId,
            "Antall dager med sanksjon som er forbrukt",
            enhet = Enhet.Dager,
        )
    val gjenståendeSanksjonsdager =
        Opplysningstype.heltall(
            gjenståendeSanksjonsdagerId,
            "Antall dager med sanksjon som gjenstår",
            enhet = Enhet.Dager,
        )
    val sisteSanksjonsdagMedForbruk = Opplysningstype.dato(sisteSanksjonsdagMedForbrukId, "Siste dag med forbruk av sanksjon")
    val sisteGjenståendeSanksjonsdager =
        Opplysningstype.heltall(sisteGjenståendeSanksjonsdagerId, "Siste antall dager med sanksjon som gjenstår", enhet = Enhet.Dager)

    val forbruktBortfallsdager =
        Opplysningstype.heltall(
            forbruktBortfallsdagerId,
            "Antall bortfallsdager som er forbrukt",
            enhet = Enhet.Dager,
        )
    val gjenståendeBortfallsdager =
        Opplysningstype.heltall(
            gjenståendeBortfallsdagerId,
            "Antall bortfallsdager som gjenstår",
            enhet = Enhet.Dager,
        )
    val sisteBortfallsdagMedForbruk = Opplysningstype.dato(sisteBortfallsdagMedForbrukId, "Siste dag med forbruk av bortfall")
    val sisteGjenståendeBortfallsdager =
        Opplysningstype.heltall(sisteGjenståendeBortfallsdagerId, "Siste antall bortfallsdager som gjenstår", enhet = Enhet.Dager)

    val meldedato = Opplysningstype.dato(meldedatoId, "Meldedato")
    val meldtITide = Opplysningstype.boolsk(trekkVedForsenMeldingId, "Har meldt seg i tide")

    val maksAntallPerioderMedIkkeTaptArbeidstid =
        Opplysningstype.heltall(
            maksAntallPerioderMedIkkeTaptArbeidstidId,
            "Maks antall perioder en kan ha påfølgende tap av arbeidstid",
            synlig = aldriSynlig,
        )

    val sumFva = Opplysningstype.desimaltall(sumFvaId, "Sum av fastsatt vanlig arbeidstid", enhet = Enhet.Timer, synlig = aldriSynlig)
    val sumArbeidstimer =
        Opplysningstype.desimaltall(
            sumArbeidstimerId,
            "Sum av arbeidede timer",
            enhet = Enhet.Timer,
            synlig = aldriSynlig,
        )
    val prosentfaktor = Opplysningstype.desimaltall(prosentfaktorId, "Prosentfaktor", synlig = aldriSynlig)

    val oppfyllerKravTilTaptArbeidstidIPerioden =
        Opplysningstype.boolsk(
            taptArbeidIPeriodenId,
            "Oppfyller kravet til tapt arbeidstid i perioden",
        )

    // TODO: Er dette noe annet enn krav til tap?
    val terskel =
        Opplysningstype.desimaltall(terskelId, "Terskel for hvor mye arbeid som kan utføres samtidig med dagpenger", enhet = Enhet.Prosent)

    val regelsett =
        fastsettelse(tomHjemmel("Meldekortberegning")) {
            skalVurderes { it.har(meldeperiode) }
            påvirkerResultat { it.har(meldeperiode) }

            regel(meldeperiode) { tomRegel }
            regel(arbeidsdag) { tomRegel }
            regel(arbeidstimer) { tomRegel }
            regel(forbruk) { tomRegel }
            regel(meldt) { tomRegel }
            regel(forbruktEgenandel) { tomRegel }
            regel(utbetaling) { tomRegel }
            regel(utbetalingForPeriode) { tomRegel }

            // Skal denne være her? Per dag?
            regel(terskel) { tomRegel }

            regel(forbrukt) { tomRegel }
            regel(gjenståendeDager) { tomRegel }

            regel(sisteForbruksdag) { tomRegel }
            // TODO: Lag en egen verdiAv()-regel?
            regel(sisteGjenståendeDager) { høyesteAv(antallStønadsdager) }
            regel(maksAntallPerioderMedIkkeTaptArbeidstid) { somUtgangspunkt(3) }

            // Bortfall (per-meldeperiode)
            regel(erSanksjonsdag) { tomRegel }
            regel(forbruktSanksjonsdager) { tomRegel }
            regel(gjenståendeSanksjonsdager) { tomRegel }
            regel(sisteSanksjonsdagMedForbruk) { tomRegel }
            regel(sisteGjenståendeSanksjonsdager) { tomRegel }
            regel(forbruktBortfallsdager) { tomRegel }
            regel(gjenståendeBortfallsdager) { tomRegel }
            regel(sisteBortfallsdagMedForbruk) { tomRegel }
            regel(sisteGjenståendeBortfallsdager) { tomRegel }

            regel(gjenståendeEgenandel) { tomRegel }
            regel(oppfyllerKravTilTaptArbeidstidIPerioden) { tomRegel }
            regel(meldtITide) { tomRegel }
            regel(meldedato) { tomRegel }

            regel(sumFva) { tomRegel }
            regel(sumArbeidstimer) { tomRegel }
            regel(prosentfaktor) { tomRegel }

            ønsketResultat(
                arbeidsdag,
                arbeidstimer,
                erSanksjonsdag,
                forbruk,
                forbrukt,
                forbruktSanksjonsdager,
                forbruktBortfallsdager,
                forbruktEgenandel,
                gjenståendeBortfallsdager,
                gjenståendeSanksjonsdager,
                gjenståendeEgenandel,
                gjenståendeDager,
                maksAntallPerioderMedIkkeTaptArbeidstid,
                meldedato,
                meldeperiode,
                meldt,
                meldtITide,
                oppfyllerKravTilTaptArbeidstidIPerioden,
                prosentfaktor,
                sumArbeidstimer,
                sumFva,
                terskel,
                utbetaling,
                utbetalingForPeriode,
            )
        }

    val OverTerskelKontroll =
        Kontrollpunkt(Avklaringspunkter.JobbetOverTerskel) {
            it.har(
                TapAvArbeidsinntektOgArbeidstid.kravTilTaptArbeidstid,
            ) &&
                !it.oppfyller(TapAvArbeidsinntektOgArbeidstid.kravTilTaptArbeidstid)
        }
}

internal object TerskelTrekkForSenMelding {
    private val antallDager =
        TemporalCollection<Int>().apply {
            put(LocalDate.MIN, 8)
        }

    fun forDato(regelverksdato: LocalDate) = antallDager.get(regelverksdato)
}

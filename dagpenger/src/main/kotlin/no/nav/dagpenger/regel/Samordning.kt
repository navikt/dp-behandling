package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningssjekk
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.addisjon
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.opplysning.regel.substraksjonTilNull
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Behov.Foreldrepenger
import no.nav.dagpenger.regel.Behov.Omsorgspenger
import no.nav.dagpenger.regel.Behov.Opplæringspenger
import no.nav.dagpenger.regel.Behov.Pleiepenger
import no.nav.dagpenger.regel.Behov.Svangerskapspenger
import no.nav.dagpenger.regel.Behov.Sykepenger
import no.nav.dagpenger.regel.Behov.Uføre
import no.nav.dagpenger.regel.OpplysningsTyper.antallTimerArbeidstidenSkalSamordnesMotId
import no.nav.dagpenger.regel.OpplysningsTyper.foreldrepengerDagsatsId
import no.nav.dagpenger.regel.OpplysningsTyper.foreldrepengerId
import no.nav.dagpenger.regel.OpplysningsTyper.omsorgspengerDagsatsId
import no.nav.dagpenger.regel.OpplysningsTyper.omsorgspengerId
import no.nav.dagpenger.regel.OpplysningsTyper.opplæringspengerDagsatsId
import no.nav.dagpenger.regel.OpplysningsTyper.opplæringspengerId
import no.nav.dagpenger.regel.OpplysningsTyper.pleiepengerDagsatsId
import no.nav.dagpenger.regel.OpplysningsTyper.pleiepengerId
import no.nav.dagpenger.regel.OpplysningsTyper.samordnetDagsatsErNegativEller0Id
import no.nav.dagpenger.regel.OpplysningsTyper.samordnetDagsatsUtenBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.samordnetFastsattArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.skalSamordnesId
import no.nav.dagpenger.regel.OpplysningsTyper.skalUføreSamordnesId
import no.nav.dagpenger.regel.OpplysningsTyper.sumAndreYtelserId
import no.nav.dagpenger.regel.OpplysningsTyper.sumHvisUføreIkkeSkalSamordnesId
import no.nav.dagpenger.regel.OpplysningsTyper.svangerskapspengerDagsatsId
import no.nav.dagpenger.regel.OpplysningsTyper.svangerskapspengerId
import no.nav.dagpenger.regel.OpplysningsTyper.sykepengerDagsatsId
import no.nav.dagpenger.regel.OpplysningsTyper.sykepengerId
import no.nav.dagpenger.regel.OpplysningsTyper.uføreBeløpSomSkalSamordnesId
import no.nav.dagpenger.regel.OpplysningsTyper.uføreDagsatsId
import no.nav.dagpenger.regel.OpplysningsTyper.uføreId
import no.nav.dagpenger.regel.OpplysningsTyper.utfallEtterSamordningId
import no.nav.dagpenger.regel.Samordning.skalSamordnes
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstid
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterNittiProsent
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.harBarnetillegg

private val visesHvisSamordning: Opplysningssjekk = { it.erSann(skalSamordnes) }

/**
 * § 4-25.Samordning med reduserte ytelser fra folketrygden, eller redusert avtalefestet pensjon
 */
object Samordning {
    val sykepenger = Opplysningstype.boolsk(sykepengerId, "Sykepenger etter lovens kapittel 8", behovId = Sykepenger)
    val pleiepenger = Opplysningstype.boolsk(pleiepengerId, "Pleiepenger etter lovens kapittel 9", behovId = Pleiepenger)
    val omsorgspenger = Opplysningstype.boolsk(omsorgspengerId, "Omsorgspenger etter lovens kapittel 9", behovId = Omsorgspenger)
    val opplæringspenger =
        Opplysningstype.boolsk(
            opplæringspengerId,
            "Opplæringspenger etter lovens kapittel 9",
            behovId = Opplæringspenger,
        )
    val uføre = Opplysningstype.boolsk(uføreId, "Uføretrygd etter lovens kapittel 12", behovId = Uføre)
    val skalUføreSamordnes =
        Opplysningstype.boolsk(
            skalUføreSamordnesId,
            "Uføretrygden er gitt med virkningstidspunkt i inneværende år eller innenfor de to siste kalenderår",
            synlig = {
                it.erSann(uføre)
            },
        )
    val uføreBeløpSomSkalSamordnes =
        Opplysningstype.beløp(
            uføreBeløpSomSkalSamordnesId,
            "Uførebeløp som skal samordnes",
            synlig = {
                it.erSann(skalUføreSamordnes)
            },
        )
    val sumHvisUføreIkkeSkalSamordnes =
        Opplysningstype.beløp(
            sumHvisUføreIkkeSkalSamordnesId,
            "Sum hvis Uføre ikke skal samordnes",
            synlig = {
                it.erSann(skalUføreSamordnes)
            },
        )
    val foreldrepenger = Opplysningstype.boolsk(foreldrepengerId, "Foreldrepenger etter lovens kapittel 14", behovId = Foreldrepenger)
    val svangerskapspenger =
        Opplysningstype.boolsk(
            svangerskapspengerId,
            "Svangerskapspenger etter lovens kapittel 14",
            behovId = Svangerskapspenger,
        )

    val sykepengerDagsats = Opplysningstype.beløp(sykepengerDagsatsId, "Sykepenger dagsats", synlig = { it.erSann(sykepenger) })
    val pleiepengerDagsats = Opplysningstype.beløp(pleiepengerDagsatsId, "Pleiepenger dagsats", synlig = { it.erSann(pleiepenger) })
    val omsorgspengerDagsats = Opplysningstype.beløp(omsorgspengerDagsatsId, "Omsorgspenger dagsats", synlig = { it.erSann(omsorgspenger) })
    val opplæringspengerDagsats =
        Opplysningstype.beløp(opplæringspengerDagsatsId, "Opplæringspenger dagsats", synlig = {
            it.erSann(opplæringspenger)
        })
    val uføreDagsats =
        Opplysningstype.beløp(
            uføreDagsatsId,
            "Uføre dagsats",
            synlig = { it.erSann(uføre) && it.erSann(skalUføreSamordnes) },
        )
    val foreldrepengerDagsats =
        Opplysningstype.beløp(
            foreldrepengerDagsatsId,
            "Foreldrepenger dagsats",
            synlig = { it.erSann(foreldrepenger) },
        )
    val svangerskapspengerDagsats =
        Opplysningstype.beløp(svangerskapspengerDagsatsId, "Svangerskapspenger dagsats", synlig = {
            it.erSann(svangerskapspenger)
        })

    private val sumAndreYtelser = Opplysningstype.beløp(sumAndreYtelserId, "Sum andre ytelser", synlig = visesHvisSamordning)
    internal val skalSamordnes =
        Opplysningstype.boolsk(skalSamordnesId, "Medlem har reduserte ytelser fra folketrygden (Samordning)", synlig = aldriSynlig)

    internal val samordnetDagsats =
        Opplysningstype.beløp(
            samordnetDagsatsUtenBarnetilleggId,
            "Samordnet dagsats uten barnetillegg",
            synlig = visesHvisSamordning,
        )
    private val kanUtbetale =
        Opplysningstype.boolsk(
            samordnetDagsatsErNegativEller0Id,
            "Samordnet dagsats er større enn 0",
            synlig = visesHvisSamordning,
        )
    val samordnetArbeidstid =
        Opplysningstype.desimaltall(
            antallTimerArbeidstidenSkalSamordnesMotId,
            "Antall timer arbeidstiden skal samordnes mot",
            synlig = visesHvisSamordning,
        )
    val samordnetBeregnetArbeidstid =
        Opplysningstype.desimaltall(samordnetFastsattArbeidstidId, "Samordnet fastsatt arbeidstid", synlig = visesHvisSamordning)
    val utfallEtterSamordning =
        Opplysningstype.boolsk(
            utfallEtterSamordningId,
            "Utfall etter samordning",
            synlig = visesHvisSamordning,
        )

    // Fulle dagpenger minus en/flere av reduserte ytelsene man mottar per samme dag (regnestykket)
    // avrundetDagsUtenBarnetillegg - sykepenger - pleiepenger - omsorgspenger - opplæringspenger - uføre - foreldrepenger - svangerskapspenger

    val regelsett =
        vilkår(
            folketrygden.hjemmel(
                kapittel = 4,
                paragraf = 25,
                tittel = "Samordning med reduserte ytelser fra folketrygden, eller redusert avtalefestet pensjon",
                kortnavn = "Samordning reduserte ytelser",
            ),
        ) {
            skalVurderes { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            regel(sykepenger) { innhentMed(prøvingsdato) }
            regel(pleiepenger) { innhentMed(prøvingsdato) }
            regel(omsorgspenger) { innhentMed(prøvingsdato) }
            regel(opplæringspenger) { innhentMed(prøvingsdato) }
            regel(foreldrepenger) { innhentMed(prøvingsdato) }
            regel(svangerskapspenger) { innhentMed(prøvingsdato) }

            // TODO: Hent uførestrygd og barnepenger fra pesys
            regel(uføre) { oppslag(prøvingsdato) { false } }
            regel(skalUføreSamordnes) { erSann(uføre) }

            regel(samordnetArbeidstid) { oppslag(prøvingsdato) { 0.0 } }
            regel(samordnetBeregnetArbeidstid) { substraksjonTilNull(beregnetArbeidstid, samordnetArbeidstid) }

            regel(sykepengerDagsats) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(pleiepengerDagsats) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(omsorgspengerDagsats) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(opplæringspengerDagsats) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(uføreDagsats) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(svangerskapspengerDagsats) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(foreldrepengerDagsats) { oppslag(prøvingsdato) { Beløp(0.0) } }

            regel(sumHvisUføreIkkeSkalSamordnes) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(uføreBeløpSomSkalSamordnes) {
                hvisSannMedResultat(
                    skalUføreSamordnes,
                    uføreDagsats,
                    sumHvisUføreIkkeSkalSamordnes,
                )
            }
            regel(sumAndreYtelser) {
                addisjon(
                    sykepengerDagsats,
                    pleiepengerDagsats,
                    omsorgspengerDagsats,
                    opplæringspengerDagsats,
                    uføreBeløpSomSkalSamordnes,
                    foreldrepengerDagsats,
                    svangerskapspengerDagsats,
                )
            }

            regel(samordnetDagsats) { substraksjonTilNull(dagsatsEtterNittiProsent, sumAndreYtelser) }
            regel(kanUtbetale) { størreEnnEllerLik(dagsatsEtterNittiProsent, sumAndreYtelser) }

            utfall(utfallEtterSamordning) { enAv(kanUtbetale, harBarnetillegg) }

            regel(skalSamordnes) {
                enAv(
                    sykepenger,
                    pleiepenger,
                    omsorgspenger,
                    opplæringspenger,
                    skalUføreSamordnes,
                    foreldrepenger,
                    svangerskapspenger,
                )
            }

            påvirkerResultat {
                it.har(skalSamordnes) && it.finnOpplysning(skalSamordnes).verdi
            }

            ønsketResultat(samordnetDagsats, skalSamordnes, utfallEtterSamordning)

            avklaring(Avklaringspunkter.Samordning)
        }

    val SkalSamordnes =
        Kontrollpunkt(Avklaringspunkter.Samordning) {
            it.har(skalSamordnes) && it.finnOpplysning(skalSamordnes).verdi
        }
}

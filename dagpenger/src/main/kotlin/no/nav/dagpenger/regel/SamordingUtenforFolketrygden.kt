package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.regel.divisjon
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.høyesteAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.inntekt.sumAv
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.substraksjonTilNull
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Behov.AndreØkonomiskeYtelser
import no.nav.dagpenger.regel.Behov.OppgittAndreYtelserUtenforNav
import no.nav.dagpenger.regel.OpplysningsTyper.andreØkonomiskeYtelserId
import no.nav.dagpenger.regel.OpplysningsTyper.beløpEtterlønnId
import no.nav.dagpenger.regel.OpplysningsTyper.beløpFraOffentligTjenestepensjonsordningId
import no.nav.dagpenger.regel.OpplysningsTyper.beløpGarantilottId
import no.nav.dagpenger.regel.OpplysningsTyper.beløpOffentligPensjonsordningId
import no.nav.dagpenger.regel.OpplysningsTyper.beløpVartpengerId
import no.nav.dagpenger.regel.OpplysningsTyper.beløpVentelønnId
import no.nav.dagpenger.regel.OpplysningsTyper.dagsatsSamordnetUtenforFolketrygdenId
import no.nav.dagpenger.regel.OpplysningsTyper.minsteMuligeUkessatsSomKanBrukesId
import no.nav.dagpenger.regel.OpplysningsTyper.mottarEtterlønnId
import no.nav.dagpenger.regel.OpplysningsTyper.mottarGarantilottId
import no.nav.dagpenger.regel.OpplysningsTyper.mottarPensjonFraEnOffentligTjenestepensjonsordningId
import no.nav.dagpenger.regel.OpplysningsTyper.mottarVartpengerId
import no.nav.dagpenger.regel.OpplysningsTyper.mottarVentelønnId
import no.nav.dagpenger.regel.OpplysningsTyper.nedreGrenseForSamordningId
import no.nav.dagpenger.regel.OpplysningsTyper.oppgittAndreYtelserUtenforNavId
import no.nav.dagpenger.regel.OpplysningsTyper.redusertUførepensjonId
import no.nav.dagpenger.regel.OpplysningsTyper.samordnetUkessatsMedFolketrygdenId
import no.nav.dagpenger.regel.OpplysningsTyper.samordnetUkessatsUtenBarnetilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.samordnetUkessatsUtenforFolketrygdenId
import no.nav.dagpenger.regel.OpplysningsTyper.skalSamordnesMedYtelserUtenforFolketrygdenId
import no.nav.dagpenger.regel.OpplysningsTyper.sumYtelserUtenforFolketrygdenId
import no.nav.dagpenger.regel.OpplysningsTyper.terskelVedSamordningId
import no.nav.dagpenger.regel.Samordning.samordnetDagsats
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.arbeidsdagerPerUke

object SamordingUtenforFolketrygden {
    val andreYtelser =
        Opplysningstype.boolsk(
            oppgittAndreYtelserUtenforNavId,
            "Oppgitt andre ytelser utenfor NAV i søknaden",
            behovId = OppgittAndreYtelserUtenforNav,
        )

    private val pensjonFraOffentligTjenestepensjonsordning =
        Opplysningstype.boolsk(
            mottarPensjonFraEnOffentligTjenestepensjonsordningId,
            "Mottar pensjon fra en offentlig tjenestepensjonsordning",
        )
    private val mottarRedusertUførepensjon =
        Opplysningstype.boolsk(
            redusertUførepensjonId,
            "Mottar redusert uførepensjon fra offentlig pensjonsordning",
        )
    private val vartpenger = Opplysningstype.boolsk(mottarVartpengerId, "Mottar vartpenger")
    private val ventelønn = Opplysningstype.boolsk(mottarVentelønnId, "Mottar ventelønn")
    private val etterlønn = Opplysningstype.boolsk(mottarEtterlønnId, "Mottar etterlønn")
    private val garantilottGFF = Opplysningstype.boolsk(mottarGarantilottId, "Mottar garantilott fra Garantikassen for fiskere.")

    val pensjonFraOffentligTjenestepensjonsordningBeløp =
        Opplysningstype.beløp(
            id = beløpFraOffentligTjenestepensjonsordningId,
            beskrivelse = "Pensjon fra en offentlig tjenestepensjonsordning beløp",
            synlig = { it.erSann(pensjonFraOffentligTjenestepensjonsordning) },
        )
    val redusertUførepensjonBeløp =
        Opplysningstype.beløp(beløpOffentligPensjonsordningId, "Uførepensjon fra offentlig pensjonsordning beløp", synlig = {
            it.erSann(mottarRedusertUførepensjon)
        })
    val vartpengerBeløp =
        Opplysningstype.beløp(beløpVartpengerId, "Vartpenger beløp", synlig = { it.erSann(vartpenger) })
    val ventelønnBeløp =
        Opplysningstype.beløp(beløpVentelønnId, "Ventelønn beløp", synlig = { it.erSann(ventelønn) })
    val etterlønnBeløp =
        Opplysningstype.beløp(beløpEtterlønnId, "Etterlønn beløp", synlig = { it.erSann(etterlønn) })
    val garantilottGFFBeløp =
        Opplysningstype.beløp(beløpGarantilottId, "Garantilott fra Garantikassen for fiskere beløp", synlig = { it.erSann(garantilottGFF) })

    val andreØkonomiskeYtelser =
        Opplysningstype.boolsk(
            id = andreØkonomiskeYtelserId,
            beskrivelse = "Mottar andre økonomiske ytelser fra arbeidsgiver eller tidligere arbeidsgiver enn lønn",
            behovId = AndreØkonomiskeYtelser,
        )

    private val terskelVedSamordning =
        Opplysningstype.desimaltall(
            id = terskelVedSamordningId,
            beskrivelse = "Hvor mange prosent av G skal brukes som terskel ved samordning",
            synlig = aldriSynlig,
        )
    val nedreGrenseForSamordning =
        Opplysningstype.beløp(
            id = nedreGrenseForSamordningId,
            beskrivelse = "Beløp tilsvarende nedre terskel av G",
            synlig = aldriSynlig,
        )
    val skalSamordnesUtenforFolketrygden =
        Opplysningstype.boolsk(
            skalSamordnesMedYtelserUtenforFolketrygdenId,
            "Skal samordnes med ytelser utenfor folketrygden",
        )

    val sumAvYtelserUtenforFolketrygden =
        Opplysningstype.beløp(
            id = sumYtelserUtenforFolketrygdenId,
            beskrivelse = "Sum av ytelser utenfor folketrygden",
            synlig = { it.erSann(skalSamordnesUtenforFolketrygden) },
        )
    val samordnetUkessatsUtenBarnetillegg =
        Opplysningstype.beløp(
            id = samordnetUkessatsUtenBarnetilleggId,
            beskrivelse = "Samordnet ukessats uten barnetillegg",
            synlig = { it.erSann(skalSamordnesUtenforFolketrygden) },
        )
    private val minsteMuligeUkessats =
        Opplysningstype.beløp(
            id = minsteMuligeUkessatsSomKanBrukesId,
            beskrivelse = "Minste mulige ukessats som som kan brukes",
            synlig = aldriSynlig,
        )
    private val samordnetUkessatsUtenforFolketrygden =
        Opplysningstype.beløp(
            id = samordnetUkessatsUtenforFolketrygdenId,
            beskrivelse = "Ukessats trukket ned for ytelser utenfor folketrygden",
            synlig = aldriSynlig,
        )
    val samordnetUkessats = Opplysningstype.beløp(samordnetUkessatsMedFolketrygdenId, "Samordnet ukessats med ytelser utenfor folketrygden")
    val dagsatsSamordnetUtenforFolketrygden =
        Opplysningstype.beløp(
            dagsatsSamordnetUtenforFolketrygdenId,
            "Dagsats uten barnetillegg samordnet",
        )

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 26, "Samordning med ytelser utenfor folketrygden", "Samordning utenfor folketrygden"),
            RegelsettType.Fastsettelse,
        ) {
            regel(andreYtelser) { innhentes }

            regel(pensjonFraOffentligTjenestepensjonsordning) { oppslag(prøvingsdato) { false } }
            regel(mottarRedusertUførepensjon) { oppslag(prøvingsdato) { false } }
            regel(vartpenger) { oppslag(prøvingsdato) { false } }
            regel(ventelønn) { oppslag(prøvingsdato) { false } }
            regel(etterlønn) { oppslag(prøvingsdato) { false } }
            regel(garantilottGFF) { oppslag(prøvingsdato) { false } }

            regel(andreØkonomiskeYtelser) { innhentes }

            regel(pensjonFraOffentligTjenestepensjonsordningBeløp) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(redusertUførepensjonBeløp) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(vartpengerBeløp) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(ventelønnBeløp) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(etterlønnBeløp) { oppslag(prøvingsdato) { Beløp(0.0) } }
            regel(garantilottGFFBeløp) { oppslag(prøvingsdato) { Beløp(0.0) } }

            regel(sumAvYtelserUtenforFolketrygden) {
                sumAv(
                    pensjonFraOffentligTjenestepensjonsordningBeløp,
                    redusertUførepensjonBeløp,
                    vartpengerBeløp,
                    ventelønnBeløp,
                    etterlønnBeløp,
                    garantilottGFFBeløp,
                )
            }

            regel(terskelVedSamordning) { oppslag(prøvingsdato) { 0.03 } }
            regel(nedreGrenseForSamordning) { multiplikasjon(grunnbeløpForDagpengeGrunnlag, terskelVedSamordning) }

            regel(samordnetUkessatsUtenBarnetillegg) { multiplikasjon(samordnetDagsats, arbeidsdagerPerUke) }

            regel(minsteMuligeUkessats) { minstAv(samordnetUkessatsUtenBarnetillegg, nedreGrenseForSamordning) }
            regel(samordnetUkessatsUtenforFolketrygden) {
                substraksjonTilNull(samordnetUkessatsUtenBarnetillegg, sumAvYtelserUtenforFolketrygden)
            }
            regel(samordnetUkessats) { høyesteAv(minsteMuligeUkessats, samordnetUkessatsUtenforFolketrygden) }

            regel(dagsatsSamordnetUtenforFolketrygden) { divisjon(samordnetUkessats, arbeidsdagerPerUke) }

            regel(skalSamordnesUtenforFolketrygden) {
                enAv(
                    andreYtelser,
                    pensjonFraOffentligTjenestepensjonsordning,
                    mottarRedusertUførepensjon,
                    vartpenger,
                    ventelønn,
                    etterlønn,
                    garantilottGFF,
                    andreØkonomiskeYtelser,
                )
            }

            relevantHvis { kravetTilAlderOgMinsteinntektErOppfylt(it) }
        }

    val ønsketResultat = listOf(skalSamordnesUtenforFolketrygden, dagsatsSamordnetUtenforFolketrygden)

    val YtelserUtenforFolketrygdenKontroll =
        Kontrollpunkt(sjekker = Avklaringspunkter.YtelserUtenforFolketrygden) { opplysninger ->
            opplysninger.har(skalSamordnesUtenforFolketrygden) && opplysninger.finnOpplysning(skalSamordnesUtenforFolketrygden).verdi
        }
}

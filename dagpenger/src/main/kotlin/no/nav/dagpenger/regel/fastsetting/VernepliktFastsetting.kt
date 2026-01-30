package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningssjekk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.beløp
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper.AntallGVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagForVernepliktErGunstigstId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagHvisVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.VernepliktFastsattVanligArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.VernepliktGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.VernepliktPeriodeId
import no.nav.dagpenger.regel.Rettighetstype.skalVernepliktVurderes
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.folketrygden

internal val synligOmVerneplikt: Opplysningssjekk = {
    it.erSann(oppfyllerKravetTilVerneplikt) && it.erSann(grunnlagForVernepliktErGunstigst)
}

object VernepliktFastsetting {
    private val antallG =
        desimaltall(AntallGVernepliktId, "Antall G som gis som grunnlag ved verneplikt", synlig = aldriSynlig, enhet = Enhet.G)
    internal val vernepliktGrunnlag = beløp(VernepliktGrunnlagId, "Grunnlag som gis ved verneplikt", synlig = synligOmVerneplikt)
    val vernepliktPeriode = heltall(VernepliktPeriodeId, "Periode som gis ved verneplikt", synlig = synligOmVerneplikt, enhet = Enhet.Uker)
    internal val vernepliktFastsattVanligArbeidstid =
        desimaltall(
            VernepliktFastsattVanligArbeidstidId,
            "Fastsatt vanlig arbeidstid for verneplikt",
            synlig = synligOmVerneplikt,
            enhet = Enhet.Timer,
        )
    internal val grunnlagHvisVerneplikt =
        beløp(GrunnlagHvisVernepliktId, "Grunnlag for verneplikt hvis kravet er oppfylt", synlig = aldriSynlig)

    val grunnlagForVernepliktErGunstigst =
        boolsk(
            GrunnlagForVernepliktErGunstigstId,
            "Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget",
            synlig = synligOmVerneplikt,
        )

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 19, "Dagpenger etter avtjent verneplikt", "Dagpenger ved verneplikt"),
        ) {
            skalVurderes { it.erSann(skalVernepliktVurderes) }

            regel(antallG) { oppslag(prøvingsdato) { 3.0 } }
            regel(vernepliktGrunnlag) { multiplikasjon(grunnbeløpForDagpengeGrunnlag, antallG) }
            regel(vernepliktPeriode) { oppslag(prøvingsdato) { 26 } }
            regel(vernepliktFastsattVanligArbeidstid) { oppslag(prøvingsdato) { 37.5 } }

            ønsketResultat(
                vernepliktGrunnlag,
                vernepliktPeriode,
                vernepliktFastsattVanligArbeidstid,
            )

            påvirkerResultat {
                it.erSann(oppfyllerKravetTilVerneplikt) && it.erSann(grunnlagForVernepliktErGunstigst)
            }
        }
}

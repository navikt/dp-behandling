package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningssjekk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.beløp
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.regel.størreEnn
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.OpplysningsTyper.AntallGVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagForVernepliktErGunstigstId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagHvisVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.GrunnlagUtenVernepliktId
import no.nav.dagpenger.regel.OpplysningsTyper.VernepliktFastsattVanligArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.VernepliktGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.VernepliktPeriodeId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.folketrygden
import no.nav.dagpenger.regel.kravPåDagpenger

private val synligOmVerneplikt: Opplysningssjekk = {
    it.erSann(oppfyllerKravetTilVerneplikt) && it.erSann(grunnlagForVernepliktErGunstigst)
}

object VernepliktFastsetting {
    private val antallG = desimaltall(AntallGVernepliktId, "Antall G som gis som grunnlag ved verneplikt", synlig = aldriSynlig)
    internal val vernepliktGrunnlag = beløp(VernepliktGrunnlagId, "Grunnlag for gis ved verneplikt", synlig = synligOmVerneplikt)
    val vernepliktPeriode = heltall(VernepliktPeriodeId, "Periode som gis ved verneplikt", synlig = synligOmVerneplikt)
    internal val vernepliktFastsattVanligArbeidstid =
        desimaltall(VernepliktFastsattVanligArbeidstidId, "Fastsatt vanlig arbeidstid for verneplikt", synlig = synligOmVerneplikt)
    internal val grunnlagHvisVerneplikt =
        beløp(GrunnlagHvisVernepliktId, "Grunnlag for verneplikt hvis kravet er oppfylt", synlig = aldriSynlig)
    internal val grunnlagUtenVerneplikt =
        beløp(GrunnlagUtenVernepliktId, "Grunnlag for verneplikt hvis kravet ikke er oppfylt", synlig = aldriSynlig)

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
            skalVurderes { kravPåDagpenger(it) }

            regel(antallG) { oppslag(prøvingsdato) { 3.0 } }
            regel(vernepliktGrunnlag) { multiplikasjon(grunnbeløpForDagpengeGrunnlag, antallG) }
            regel(vernepliktPeriode) { oppslag(prøvingsdato) { 26 } }
            regel(vernepliktFastsattVanligArbeidstid) { oppslag(prøvingsdato) { 37.5 } }

            regel(grunnlagUtenVerneplikt) { somUtgangspunkt(Beløp(0)) }

            // Setter grunnlag avhengig av om bruker oppfyller kravet til verneplikt (0G eller 3G)
            regel(grunnlagHvisVerneplikt) { hvisSannMedResultat(oppfyllerKravetTilVerneplikt, vernepliktGrunnlag, grunnlagUtenVerneplikt) }

            // Kriteriet om vi skal bruke grunnlag og FVA fra verneplikt eller dagpengegrunnlag
            regel(grunnlagForVernepliktErGunstigst) { størreEnn(grunnlagHvisVerneplikt, dagpengegrunnlag) }

            påvirkerResultat {
                it.erSann(oppfyllerKravetTilVerneplikt) && it.erSann(grunnlagForVernepliktErGunstigst)
            }

            ønsketResultat(
                vernepliktGrunnlag,
                vernepliktPeriode,
                vernepliktFastsattVanligArbeidstid,
                grunnlagForVernepliktErGunstigst,
            )
        }
}

package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.hvis
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnn
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag

object VernepliktFastsetting {
    private val antallG = Opplysningstype.somDesimaltall("Antall G som gis som grunnlag ved verneplikt")
    internal val vernepliktGrunnlag = Opplysningstype.somBeløp("Grunnlag for gis ved verneplikt")
    val vernepliktPeriode = Opplysningstype.somHeltall("Periode som gis ved verneplikt")
    internal val vernepliktFastsattVanligArbeidstid = Opplysningstype.somDesimaltall("Fastsatt vanlig arbeidstid for verneplikt")
    internal val grunnlagHvisVerneplikt = Opplysningstype.somBeløp("Grunnlag for verneplikt hvis kravet er oppfylt")
    val grunnlagForVernepliktErGunstigst = Opplysningstype.somBoolsk("Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget")

    val regelsett =
        Regelsett("VernepliktFastsetting") {
            regel(antallG) { oppslag(prøvingsdato) { 3.0 } }
            regel(vernepliktGrunnlag) { multiplikasjon(grunnbeløpForDagpengeGrunnlag, antallG) }
            regel(vernepliktPeriode) { oppslag(prøvingsdato) { 26 } }
            regel(vernepliktFastsattVanligArbeidstid) { oppslag(prøvingsdato) { 37.5 } }

            // Setter grunnlag avhengig av om bruker oppfyller kravet til verneplikt (0G eller 3G)
            regel(grunnlagHvisVerneplikt) { hvis(oppfyllerKravetTilVerneplikt, vernepliktGrunnlag, Beløp(0)) }

            // Kriteriet om vi skal bruke grunnlag og FVA fra verneplikt eller dagpengegrunnlag
            regel(grunnlagForVernepliktErGunstigst) { størreEnn(vernepliktGrunnlag, dagpengegrunnlag) }
        }

    val ønsketResultat = listOf(vernepliktGrunnlag, vernepliktPeriode, vernepliktFastsattVanligArbeidstid)
}

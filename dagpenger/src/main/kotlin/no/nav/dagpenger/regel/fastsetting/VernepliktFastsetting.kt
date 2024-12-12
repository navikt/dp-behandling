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
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.vernepliktGrunnlag

object VernepliktFastsetting {
    private val antallG = Opplysningstype.somDesimaltall("Antall G som gis som grunnlag ved verneplikt")
    internal val vernepliktGrunnlag = Opplysningstype.somBeløp("Grunnlag for gis ved verneplikt")
    internal val vernepliktPeriode = Opplysningstype.somHeltall("Periode som gis ved verneplikt")
    internal val vernepliktFastsattVanligArbeidstid = Opplysningstype.somDesimaltall("Fastsatt vanlig arbeidstid for verneplikt")
    internal val faktiskArbeidstid = Opplysningstype.somDesimaltall("Fastsatt vanlig arbeidstid for verneplikt")
    internal val fajlskdjfgrunnlagsomfaktisksalbrukesomduharlov = Opplysningstype.somBeløp("Fastsatt vanlig arbeidstid for verneplikt")
    internal val vernepliktGrunnlagErKulest = Opplysningstype.somBoolsk("Fastsatt vanlig arbeidstid for verneplikt")

    val regelsett =
        Regelsett("VernepliktFastsetting") {
            regel(antallG) { oppslag(prøvingsdato) { 3.0 } }
            regel(vernepliktGrunnlag) { multiplikasjon(grunnbeløpForDagpengeGrunnlag, antallG) }
            regel(vernepliktPeriode) { oppslag(prøvingsdato) { 26 } }
            regel(vernepliktFastsattVanligArbeidstid) { oppslag(prøvingsdato) { 37.5 } }

            regel(fajlskdjfgrunnlagsomfaktisksalbrukesomduharlov) { hvis(oppfyllerKravetTilVerneplikt, vernepliktGrunnlag, Beløp(0)) }
            regel(vernepliktGrunnlagErKulest) { størreEnn(er = vernepliktGrunnlag, størreEnn = dagpengegrunnlag) }
            regel(fajlskdjfgrunnlagsomfaktisksalbrukesomduharlov) { hvis(oppfyllerKravetTilVerneplikt, vernepliktGrunnlag, Beløp(0)) }

            regel(faktiskArbeidstid) { hvis(vernepliktGrunnlagErKulest, vernepliktFastsattVanligArbeidstid, 0.0) }
        }

    val ønsketResultat = listOf(vernepliktGrunnlag, vernepliktPeriode, vernepliktFastsattVanligArbeidstid)
}

package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.prosentTerskel
import no.nav.dagpenger.regel.Behov.HarTaptArbeid
import no.nav.dagpenger.regel.Behov.KravPåLønn
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadstidspunkt

object TapAvArbeidsinntektOgArbeidstid {
    internal val tapAvArbeid = Opplysningstype.somBoolsk("Har tapt arbeid".id(HarTaptArbeid))
    internal val kravPåLønn = Opplysningstype.somBoolsk("Krav på lønn fra tidligere arbeidsgiver".id(KravPåLønn))
    private val ikkeKravPåLønn = Opplysningstype.somBoolsk("Ikke krav på lønn fra tidligere arbeidsgiver")
    internal val tapAvArbeidsinntekt = Opplysningstype.somBoolsk("Krav til tap av arbeidsinntekt")

    private val kravTilArbeidstidsreduksjon = Opplysningstype.somDesimaltall("Krav til prosentvis tap av arbeidstid")
    private val beregningsregel6mnd = Opplysningstype.somBoolsk("Beregningsregel: Arbeidstid siste 6 måneder")
    private val beregningsregel12mnd = Opplysningstype.somBoolsk("Beregningsregel: Arbeidstid siste 12 måneder")
    private val beregningsregel36mnd = Opplysningstype.somBoolsk("Beregeningsregel: Arbeidstid siste 36 måneder")
    internal val beregnetArbeidstid = Opplysningstype.somDesimaltall("Beregnet vanlig arbeidstid per uke før tap")
    private val maksimalVanligArbeidstid = Opplysningstype.somDesimaltall("Maksimal vanlig arbeidstid")
    private val fastsattVanligArbeidstid = Opplysningstype.somDesimaltall("Fastsatt arbeidstid per uke før tap")
    internal val nyArbeidstid = Opplysningstype.somDesimaltall("Ny arbeidstid per uke")
    internal val kravTilTaptArbeidstid: Opplysningstype<Boolean> = Opplysningstype.somBoolsk("Tap av arbeidstid er minst terskel")

    val kravTilTapAvArbeidsinntektOgArbeidstid = Opplysningstype.somBoolsk("Krav til tap av arbeidsinntekt og arbeidstid")

    val regelsett =
        Regelsett("Krav til tap av arbeidsinntekt og arbeidstid") {
            regel(tapAvArbeid) { innhentMed() }
            regel(kravPåLønn) { innhentMed() }
            regel(ikkeKravPåLønn) { ikke(kravPåLønn) }
            regel(tapAvArbeidsinntekt) { alle(tapAvArbeid, ikkeKravPåLønn) }

            regel(kravTilArbeidstidsreduksjon) { oppslag(søknadstidspunkt) { 50.0 } } // Perm og sånt har andre terskelverdier

            // TODO: Kun en av disse må være sann. Enforces med Avklaring (i framtiden)
            regel(beregningsregel6mnd) { oppslag(søknadstidspunkt) { false } }
            regel(beregningsregel12mnd) { oppslag(søknadstidspunkt) { false } }
            regel(beregningsregel36mnd) { oppslag(søknadstidspunkt) { false } }

            // TODO: Bør hentes fra noe
            regel(beregnetArbeidstid) { oppslag(søknadstidspunkt) { 0.0 } }
            regel(nyArbeidstid) { oppslag(søknadstidspunkt) { 0.0 } }
            regel(maksimalVanligArbeidstid) { oppslag(søknadstidspunkt) { 40.0 } }
            regel(fastsattVanligArbeidstid) { minstAv(beregnetArbeidstid, maksimalVanligArbeidstid) }

            regel(kravTilTaptArbeidstid) { prosentTerskel(nyArbeidstid, fastsattVanligArbeidstid, kravTilArbeidstidsreduksjon) }

            regel(kravTilTapAvArbeidsinntektOgArbeidstid) {
                alle(tapAvArbeidsinntekt, kravTilTaptArbeidstid)
            }
        }
}

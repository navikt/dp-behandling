package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.erIkkeSann
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadstidspunkt

object Utdanning {
    internal val tarUtdanning = Opplysningstype.somBoolsk("Tar utdanning eller opplæring?")
    internal val godkjentUnntakForUtdanning = Opplysningstype.somBoolsk("Godkjent unntak for utdanning eller opplæring?")
    private val svartJaPåUtdanning = Opplysningstype.somBoolsk("Har svart ja på spørsmål om utdanning eller opplæring")
    private val svartNeiPåUtdanning = Opplysningstype.somBoolsk("Har svart nei på spørsmål om utdanning eller opplæring")
    private val oppfyllerKravetPåUnntak = Opplysningstype.somBoolsk("Oppfyller kravet på unntak for utdanning eller opplæring")

    // § 4-3.(deltakelse i arbeidsmarkedstiltak)
    private val deltakelseIArbeidsmarkedstiltak = Opplysningstype.somBoolsk("Deltar i arbeidsmarkedstiltak")

    // § 4-3a.(opplæring for innvandrere)
    private val opplæringForInnvandrere = Opplysningstype.somBoolsk("Deltar i opplæring for innvandrere")

    // § 4-3b.(grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter)
    private val grunnskoleopplæring =
        Opplysningstype.somBoolsk("Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter")

    // § 4-3c.(høyere yrkesfaglig utdanning)
    private val høyereYrkesfagligUtdanning = Opplysningstype.somBoolsk("Deltar i høyere yrkesfaglig utdanning")

    // § 4-3d.(høyere utdanning)
    private val høyereUtdanning = Opplysningstype.somBoolsk("Deltar i høyere utdanning")

    // § 4-3e.(deltakelse på kurs mv
    private val deltakelsePåKurs = Opplysningstype.somBoolsk("Deltar på kurs mv")

    val kravTilUtdanning = Opplysningstype.somBoolsk("Krav til utdanning eller opplæring")

    val regelsett =
        Regelsett("§ 4-6 første og andre avsnitt, Utdanning") {
            regel(tarUtdanning) {
                innhentMed()
            }
            regel(deltakelseIArbeidsmarkedstiltak) {
                oppslag(søknadstidspunkt) { false }
            }
            regel(opplæringForInnvandrere) {
                oppslag(søknadstidspunkt) { false }
            }
            regel(grunnskoleopplæring) {
                oppslag(søknadstidspunkt) { false }
            }
            regel(høyereYrkesfagligUtdanning) {
                oppslag(søknadstidspunkt) { false }
            }
            regel(høyereUtdanning) {
                oppslag(søknadstidspunkt) { false }
            }
            regel(deltakelsePåKurs) {
                oppslag(søknadstidspunkt) { false }
            }

            regel(godkjentUnntakForUtdanning) {
                alle(
                    deltakelseIArbeidsmarkedstiltak,
                    opplæringForInnvandrere,
                    grunnskoleopplæring,
                    høyereYrkesfagligUtdanning,
                    høyereUtdanning,
                    deltakelsePåKurs,
                )
            }
            regel(svartJaPåUtdanning) {
                erSann(tarUtdanning)
            }
            regel(svartNeiPåUtdanning) {
                erIkkeSann(tarUtdanning)
            }
            regel(oppfyllerKravetPåUnntak) {
                alle(svartJaPåUtdanning, godkjentUnntakForUtdanning)
            }
            regel(kravTilUtdanning) {
                enAv(oppfyllerKravetPåUnntak, svartNeiPåUtdanning)
            }
        }
}

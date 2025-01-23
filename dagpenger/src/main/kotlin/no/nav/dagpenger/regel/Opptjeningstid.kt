package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.regel.dato.førsteArbeidsdag
import no.nav.dagpenger.opplysning.regel.dato.sisteAvsluttendeKalenderMåned
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Behov.SisteAvsluttendeKalenderMåned
import no.nav.dagpenger.regel.OpplysningEtellerannet.ArbeidsgiversRapporteringsfristId
import no.nav.dagpenger.regel.OpplysningEtellerannet.PliktigRapporteringsfristId
import no.nav.dagpenger.regel.OpplysningEtellerannet.SisteAvsluttendeKalenderMånedId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import java.time.LocalDate

/**
 * Kapittel 3A. Søknadstidspunkt, opptjeningstid mv. -
 *
 * https://lovdata.no/dokument/SF/forskrift/1998-09-16-890/KAPITTEL_4#%C2%A73a-2
 */

object Opptjeningstid {
    // https://lovdata.no/dokument/NL/lov/2012-06-22-43/%C2%A74#%C2%A74
    private val pliktigRapporteringsfrist = dato(PliktigRapporteringsfristId, "Lovpålagt rapporteringsfrist for A-ordningen")
    val justertRapporteringsfrist = dato(ArbeidsgiversRapporteringsfristId, "Arbeidsgivers rapporteringsfrist")
    val sisteAvsluttendendeKalenderMåned =
        dato(SisteAvsluttendeKalenderMånedId, beskrivelse = "Siste avsluttende kalendermåned", behovId = SisteAvsluttendeKalenderMåned)

    val regelsett =
        Regelsett(
            aOpplynsingsLoven.hjemmel(1, 2, "Frist for levering av opplysninger", "Opptjeningsperiode"),
            RegelsettType.Fastsettelse,
        ) {
            regel(pliktigRapporteringsfrist) { oppslag(prøvingsdato) { Aordningen.rapporteringsfrist(it) } }
            regel(justertRapporteringsfrist) { førsteArbeidsdag(pliktigRapporteringsfrist) }
            regel(sisteAvsluttendendeKalenderMåned) { sisteAvsluttendeKalenderMåned(prøvingsdato, justertRapporteringsfrist) }
        }
}

private object Aordningen {
    fun rapporteringsfrist(dato: LocalDate): LocalDate = LocalDate.of(dato.year, dato.month, 5)
}

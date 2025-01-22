package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.regel.OpplysningEtellerannet.avtjentVernepliktId
import no.nav.dagpenger.regel.OpplysningEtellerannet.oppfyllerKravetTilVernepliktId
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype

object Verneplikt {
    val avtjentVerneplikt =
        Opplysningstype.som(
            avtjentVernepliktId,
            "Avtjent verneplikt",
            formål = Opplysningsformål.Bruker,
            behovId = Behov.Verneplikt,
        )
    val oppfyllerKravetTilVerneplikt =
        Opplysningstype.som(
            oppfyllerKravetTilVernepliktId,
            "Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste",
        )

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 19, "Dagpenger etter avtjent verneplikt", "4-19 Verneplikt"),
        ) {
            regel(avtjentVerneplikt) { innhentMed(søknadIdOpplysningstype) }
            utfall(oppfyllerKravetTilVerneplikt) { erSann(avtjentVerneplikt) }

            avklaring(Avklaringspunkter.Verneplikt)

            relevantHvis {
                val a = it.har(avtjentVerneplikt) && it.finnOpplysning(avtjentVerneplikt).verdi
                val b = it.har(oppfyllerKravetTilVerneplikt) && it.finnOpplysning(oppfyllerKravetTilVerneplikt).verdi

                a || b
            }
        }

    val VernepliktKontroll =
        Kontrollpunkt(sjekker = Avklaringspunkter.Verneplikt) { opplysninger ->
            opplysninger.har(avtjentVerneplikt) && opplysninger.finnOpplysning(avtjentVerneplikt).verdi
        }
}

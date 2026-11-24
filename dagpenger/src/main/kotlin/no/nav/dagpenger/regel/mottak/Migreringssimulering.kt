package no.nav.dagpenger.regel.mottak

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.RegelkjøringLoopException
import no.nav.dagpenger.opplysning.Regelkjøringsrapport
import no.nav.dagpenger.opplysning.regel.Regel

internal fun Forretningsprosess.simulerMigrering(opplysninger: Opplysninger): Regelkjøringsrapport {
    val kjørteRegler = mutableSetOf<Regel<*>>()
    val kontekst = Prosesskontekst(opplysninger)
    repeat(11) {
        kontekst.startNyRunde()
        val rapport = regelkjøring(opplysninger).evaluer()
        kjørteRegler.addAll(rapport.kjørteRegler)
        kjørEtterRegelkjøring(kontekst)
        if (rapport.erFerdig()) {
            kjørRegelkjøringFerdig(kontekst)
        }
        if (!kontekst.kreverRekjøring) {
            return rapport.copy(kjørteRegler = kjørteRegler.toSet())
        }
    }
    throw RegelkjøringLoopException(
        "Regler og plugins blir ikke enige under simulering av migrering. " +
            "Rekjøringer per årsak: ${kontekst.rekjøringerPerÅrsak}",
    )
}

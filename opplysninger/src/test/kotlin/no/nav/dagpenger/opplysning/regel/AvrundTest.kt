package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.TestOpplysningstyper.beløpA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.beløpB
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.mai
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.opplysning.verdier.Beløp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvrundTest {
    private val opplysninger = Opplysninger()
    private val regelsett =
        fastsettelse(tomHjemmel("Test avrunding")) {
            regel(beløpA) { innhentes }
            regel(beløpB) { avrund(beløpA) }
        }

    private val regelkjøring =
        Regelkjøring(
            1.mai,
            opplysninger,
            regelsett,
        )

    @Test
    fun `sjekk avrund oppover`() {
        opplysninger.leggTil(Faktum(beløpA, Beløp(8488.725)))
        regelkjøring.evaluer()
        val avrundet = opplysninger.finnOpplysning(beløpB)
        assertEquals(Beløp(8489), avrundet.verdi)
    }

    @Test
    fun `sjekk avrund nedover`() {
        opplysninger.leggTil(Faktum(beløpA, Beløp(8488.425)))
        regelkjøring.evaluer()
        val avrundet = opplysninger.finnOpplysning(beløpB)
        assertEquals(Beløp(8488), avrundet.verdi)
    }
}

package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.TestOpplysningstyper.beløpA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.beløpB
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskA
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.mai
import no.nav.dagpenger.opplysning.verdier.Beløp
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StørreEnnEllerLikTest {
    private val opplysninger = Opplysninger()
    private val regelkjøring =
        Regelkjøring(
            1.mai,
            opplysninger,
            vilkår("regelsett") {
                regel(beløpA) { innhentes }
                regel(beløpB) { innhentes }
                regel(boolskA) { størreEnnEllerLik(beløpA, beløpB) }
            },
        )

    @Test
    fun `større enn`() {
        opplysninger.leggTil(Faktum(beløpA, Beløp(2.0.toBigDecimal()))).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(beløpB, Beløp(1.0.toBigDecimal()))).also { regelkjøring.evaluer() }
        val utledet = opplysninger.finnOpplysning(boolskA)
        assertTrue(utledet.verdi)
    }

    @Test
    fun `ikke større enn`() {
        opplysninger.leggTil(Faktum(beløpA, Beløp(1.0.toBigDecimal()))).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(beløpB, Beløp(2.0.toBigDecimal()))).also { regelkjøring.evaluer() }
        val utledet = opplysninger.finnOpplysning(boolskA)
        assertFalse(utledet.verdi)
    }
}

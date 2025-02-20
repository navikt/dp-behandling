package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.TestOpplysningstyper.andel
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.terskel
import no.nav.dagpenger.opplysning.TestOpplysningstyper.total
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.mai
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SjekkAvTerskelTest {
    private val opplysninger = Opplysninger()
    private val regelkjøring =
        Regelkjøring(
            1.mai,
            opplysninger,
            vilkår("regelsett") {
                regel(andel) { innhentes }
                regel(total) { innhentes }
                regel(terskel) { innhentes }
                regel(boolskA) { prosentTerskel(andel, total, terskel) }
            },
        )

    @Test
    fun `større enn`() {
        opplysninger.leggTil(Faktum(andel, 24.0)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(total, 40.0)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(terskel, 40.0)).also { regelkjøring.evaluer() }
        val utledet = opplysninger.finnOpplysning(boolskA)
        assertTrue(utledet.verdi)
    }

    @Test
    fun `større endn`() {
        opplysninger.leggTil(Faktum(andel, 30.0)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(total, 37.5)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(terskel, 50.0)).also { regelkjøring.evaluer() }
        val utledet = opplysninger.finnOpplysning(boolskA)
        assertFalse(utledet.verdi)
    }
}

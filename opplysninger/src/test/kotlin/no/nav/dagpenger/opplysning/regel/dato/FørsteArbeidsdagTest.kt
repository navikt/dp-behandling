package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato1
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato2
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.mai
import no.nav.dagpenger.opplysning.regel.innhentes
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FørsteArbeidsdagTest {
    private val opplysninger = Opplysninger()
    private val regelkjøring =
        Regelkjøring(
            1.mai,
            opplysninger,
            vilkår("finn første arbeidsdag for en dato") {
                regel(dato1) { innhentes }
                regel(dato2) { førsteArbeidsdag(dato1) }
            },
        )

    @Test
    fun `Fredag 5 juli  2019 er en arbeidsag`() {
        opplysninger.leggTil(Hypotese(dato1, LocalDate.of(2019, 7, 5))).also { regelkjøring.evaluer() }
        val utledet = opplysninger.finnOpplysning(dato2)
        assertEquals(LocalDate.of(2019, 7, 5), utledet.verdi)
    }

    @Test
    fun `Fredag 5 mai  2019 er en søndag og dermed blir første arbeidsdag 6 mai`() {
        opplysninger.leggTil(Hypotese(dato1, LocalDate.of(2019, 5, 5))).also { regelkjøring.evaluer() }
        val utledet = opplysninger.finnOpplysning(dato2)
        assertEquals(LocalDate.of(2019, 5, 6), utledet.verdi)
    }

    @Test
    fun `Første arbeidsdag etter 5 oktober 2024 er mandag 7 oktober 2024`() {
        opplysninger.leggTil(Hypotese(dato1, LocalDate.of(2024, 10, 5))).also { regelkjøring.evaluer() }
        val utledet = opplysninger.finnOpplysning(dato2)
        assertEquals(LocalDate.of(2024, 10, 7), utledet.verdi)
    }
}

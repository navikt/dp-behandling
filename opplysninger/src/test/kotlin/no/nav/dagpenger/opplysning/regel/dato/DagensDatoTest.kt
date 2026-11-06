package no.nav.dagpenger.opplysning.regel.dato

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato1
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.mai
import java.time.LocalDate
import kotlin.test.Test

class DagensDatoTest {
    private val opplysninger = Opplysninger()
    private val regelkjøring =
        Regelkjøring(
            1.mai,
            opplysninger,
            vilkår("finn dagens dato") {
                regel(dato1) { finnDagensDato }
            },
        )

    @Test
    fun `setter dagens dato når den mangler`() {
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(dato1).verdi shouldBe LocalDate.now()
    }

    @Test
    fun `oppdaterer dagens dato når lagret dato er utdatert`() {
        opplysninger.leggTil(Faktum(dato1, LocalDate.now().minusDays(3)))

        regelkjøring.evaluer()

        opplysninger.forDato(1.mai).finnOpplysning(dato1).verdi shouldBe LocalDate.now()
    }

    @Test
    fun `kjører ikke på nytt når lagret dato er dagens dato`() {
        opplysninger.leggTil(Faktum(dato1, LocalDate.now()))

        regelkjøring.evaluer().kjørteRegler shouldBe emptySet()
    }
}

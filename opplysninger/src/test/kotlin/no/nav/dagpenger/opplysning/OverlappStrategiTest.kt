package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Opplysningstype.Id
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OverlappStrategiTest {
    private fun type(strategi: OverlappStrategi) = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "dato", overlappStrategi = strategi)

    private fun perioder(strategi: OverlappStrategi): List<Pair<LocalDate, LocalDate>> {
        val type = type(strategi)
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(type, 1.januar, Gyldighetsperiode(1.januar, 20.januar)))
        opplysninger.leggTil(Faktum(type, 2.januar, Gyldighetsperiode(5.januar, 10.januar)))
        return opplysninger.finnAlle(type).map { it.gyldighetsperiode.fraOgMed to it.gyldighetsperiode.tilOgMed }
    }

    @Test
    fun `ERSTATT_FRA_FOM beholder bare venstre rest`() {
        perioder(OverlappStrategi.ERSTATT_FRA_FOM) shouldBe
            listOf(1.januar to 4.januar, 5.januar to 10.januar)
    }

    @Test
    fun `BEVAR_HØYRE beholder resten til høyre`() {
        perioder(OverlappStrategi.BEVAR_HØYRE) shouldBe
            listOf(1.januar to 4.januar, 5.januar to 10.januar, 11.januar to 20.januar)
    }

    @Test
    fun `KUN_SISTE gir bare siste egne opplysning`() {
        perioder(OverlappStrategi.KUN_SISTE) shouldBe listOf(5.januar to 10.januar)
    }
}

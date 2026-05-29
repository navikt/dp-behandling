package no.nav.dagpenger

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.grunnbelop.Regel
import no.nav.dagpenger.grunnbelop.forDato
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Denne endrer seg hvis vi får en ny G fra dp-grunnbelop - forventet at den feiler da.
// Oppdater til ny verdi som gjelder når en oppdatere dp-grunnbelop biblioteket
class GrunnbeløpSanityTest {
    @Test
    fun `Sanity check for å sjekke at vi har fått ny G`() {
        getGrunnbeløpForRegel(Regel.Grunnlag)
            .forDato(
                dato = LocalDate.now().plusYears(1),
                gjeldendeDato = LocalDate.now().plusYears(1),
            ).verdi shouldBe 136549.toBigDecimal()

        getGrunnbeløpForRegel(Regel.Minsteinntekt)
            .forDato(
                dato = LocalDate.now().plusYears(1),
                gjeldendeDato = LocalDate.now().plusYears(1),
            ).verdi shouldBe 136549.toBigDecimal()
    }
}

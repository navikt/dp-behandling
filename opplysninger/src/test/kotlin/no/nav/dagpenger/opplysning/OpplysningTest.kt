package no.nav.dagpenger.opplysning

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpplysningTest {
    @Test
    fun `Har opplysningstype`() {
        val opplysning = Faktum(Opplysningstype.somDato("Fødselsdato"), LocalDate.now())
        assertTrue(opplysning.er(Opplysningstype.somDato("Fødselsdato")))
    }
}

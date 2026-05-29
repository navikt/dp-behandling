package no.nav.dagpenger.opplysning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class OpplysningstypeRegisterTest {
    @Test
    fun `skal registrere opplysningstyper`() {
        val id = Opplysningstype.Id(UUID.randomUUID(), Dato)
        val opplysningstype = Opplysningstype.dato(id, "test")

        val register = OpplysningstypeRegister(listOf(opplysningstype))
        register[id] shouldBe opplysningstype
    }

    @Test
    fun `skal ikke registrere duplikate ider`() {
        val id = Opplysningstype.Id(UUID.randomUUID(), Dato)
        val opplysningstype1 = Opplysningstype.dato(id, "test1")
        val opplysningstype2 = Opplysningstype.dato(id, "test2")

        shouldThrow<IllegalStateException> {
            OpplysningstypeRegister(listOf(opplysningstype1, opplysningstype2))
        }
    }

    @Test
    fun `skal ikke registrere duplikate ider mot historisk`() {
        val id = Opplysningstype.Id(UUID.randomUUID(), Dato)
        val opplysningstype1 = Opplysningstype.dato(id, "test")

        shouldThrow<IllegalStateException> {
            OpplysningstypeRegister(listOf(opplysningstype1), listOf(id))
        }
    }
}

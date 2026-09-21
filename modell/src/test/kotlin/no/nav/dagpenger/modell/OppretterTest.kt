package no.nav.dagpenger.modell

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OppretterTest {
    @Test
    fun `parser støttede opprettertyper`() {
        Oppretter.fra("Saksbehandler", "Z123456") shouldBe
            Oppretter(Oppretter.Type.Saksbehandler, "Z123456")
        Oppretter.fra("System", "dp-klient") shouldBe
            Oppretter(Oppretter.Type.System, "dp-klient")
    }

    @Test
    fun `ukjent opprettertype feiler eksplisitt`() {
        shouldThrow<IllegalArgumentException> {
            Oppretter.fra("Ukjent", "ident")
        }
    }
}

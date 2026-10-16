package no.nav.dagpenger.opplysning

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AktørTest {
    @Test
    fun `aktører har identitet`() {
        Saksbehandler("Z123456").ident shouldBe "Z123456"
        Systemaktør("dp-klient").ident shouldBe "dp-klient"
        Systemaktør.dpSak shouldBe Systemaktør("dp-sak")
    }

    @Test
    fun `avviser tom aktørident`() {
        shouldThrowWithMessage<IllegalArgumentException>("Saksbehandlerident kan ikke være tom") {
            Saksbehandler(" ")
        }
        shouldThrowWithMessage<IllegalArgumentException>("Systemident kan ikke være tom") {
            Systemaktør("")
        }
    }
}

package no.nav.dagpenger.behandling.modell

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import org.junit.jupiter.api.Test

internal class IdentTest {
    @Test
    fun `personidentifikator består av 11 siffer`() {
        shouldNotThrowAny { "12345678901".tilPersonIdentfikator() }
        shouldThrow<IllegalArgumentException> { "123".tilPersonIdentfikator() }
        shouldThrow<IllegalArgumentException> { "ident".tilPersonIdentfikator() }
    }

    @Test
    fun ` likhet `() {
        val personIdent = "12345678901".tilPersonIdentfikator()
        personIdent shouldBe personIdent
        personIdent.hashCode() shouldBe personIdent.hashCode()
        personIdent shouldBe "12345678901".tilPersonIdentfikator()
        personIdent.hashCode() shouldBe "12345678901".tilPersonIdentfikator().hashCode()
        personIdent shouldNotBe "22345678901".tilPersonIdentfikator()
        "22345678901".tilPersonIdentfikator() shouldNotBe personIdent
        personIdent shouldNotBe Any()
        personIdent shouldNotBe null
    }
}

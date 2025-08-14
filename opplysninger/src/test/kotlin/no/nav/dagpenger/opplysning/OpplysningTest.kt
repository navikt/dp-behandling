package no.nav.dagpenger.opplysning

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato1
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato2
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltall
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltallSomTimer
import no.nav.dagpenger.opplysning.TestOpplysningstyper.ulid
import no.nav.dagpenger.opplysning.verdier.Ulid
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OpplysningTest {
    @Test
    fun `Har opplysningstype`() {
        val opplysning = Faktum(dato1, LocalDate.now())
        assertTrue(opplysning.er(dato1))
    }

    @Test
    fun `Opplysningstype med enhet som validerer`() {
        val opplysning = Faktum(desimaltallSomTimer, 2.0)
        opplysning.somEnhet() shouldBe Timer(2.0)

        shouldThrowAny { Faktum(desimaltallSomTimer, 2.1) }
        shouldThrowAny { Faktum(desimaltallSomTimer, -1.0) }
        shouldNotThrowAny { Faktum(desimaltallSomTimer, 2.5) }
    }

    @Test
    fun `opplysning har uuid versjon 7 id`() {
        val opplysning = Faktum(dato1, LocalDate.now())
        shouldNotThrowAny { UUID.fromString(opplysning.id.toString()) }
        opplysning.id.version() shouldBe 7
    }

    @Test
    fun `opplysning har opprettet dato`() {
        val opplysning = Faktum(dato1, LocalDate.now())
        assertTrue(opplysning.opprettet.isBefore(LocalDateTime.now()))
    }

    @Test
    fun `er redigerbar`() {
        val opplysning1 = Faktum(dato1, LocalDate.now())
        val opplysning2 = Faktum(dato2, LocalDate.now())
        val opplysning3 = Faktum(desimaltall, 2.0, utledetAv = Utledning("regel", listOf(opplysning1, opplysning2)))
        val opplysning4 = Faktum(ulid, Ulid("01F9KZ3YX4QZJZQVQZJZQVQVQZ"))
        val erstattet = opplysning4.lagForkortet(Faktum(ulid, Ulid("01F9KZ3YX4QZJZQVQZJZQVQVQZ")))

        opplysning1.kanRedigeres(RedigerbarPerOpplysningstype) shouldBe true
        opplysning2.kanRedigeres(RedigerbarPerOpplysningstype) shouldBe false
        opplysning2.kanRedigeres { true } shouldBe true

        // Kan redigere opplysning som er utledet
        opplysning3.kanRedigeres { true } shouldBe true

        // Kan ikke redigere opplysningstype ULID
        opplysning4.kanRedigeres { true } shouldBe false

        // Kan ikke redigere erstattet opplysning
        erstattet.kanRedigeres { true } shouldBe false
    }

    private object RedigerbarPerOpplysningstype : Redigerbar {
        private val redigerbare = setOf(dato1)

        override fun kanRedigere(opplysningstype: Opplysningstype<*>): Boolean = redigerbare.contains(opplysningstype)
    }
}

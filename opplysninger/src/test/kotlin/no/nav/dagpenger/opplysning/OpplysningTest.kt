package no.nav.dagpenger.opplysning

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Opplysningstype.Id
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.TestOpplysningstyper.beløpA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato1
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato2
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltall
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltallSomTimer
import no.nav.dagpenger.opplysning.TestOpplysningstyper.heltallA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.ulid
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.Ulid
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.uuid.UUIDv7
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
    fun `Opplysningstype med enhet `() {
        val opplysning = Faktum(desimaltallSomTimer, 2.0)
        opplysning.opplysningstype.enhet shouldBe Enhet.Timer
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
        val erstattet = Faktum(ulid, Ulid("01F9KZ3YX4QZJZQVQZJZQVQVQZ"))
        opplysning4.erstatter(erstattet)

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

    private val gyldighetsperiode = Gyldighetsperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))

    @Test
    fun `erLik - boolsk med lik verdi`() {
        val x = Faktum(a, true, gyldighetsperiode)
        val y = Faktum(a, true, gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - boolsk med ulik verdi`() {
        val x = Faktum(a, true, gyldighetsperiode)
        val y = Faktum(a, false, gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - dato med lik verdi`() {
        val dato = LocalDate.of(2024, 6, 15)
        val x = Faktum(dato1, dato, gyldighetsperiode)
        val y = Faktum(dato1, dato, gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - dato med ulik verdi`() {
        val x = Faktum(dato1, LocalDate.of(2024, 1, 1), gyldighetsperiode)
        val y = Faktum(dato1, LocalDate.of(2024, 1, 2), gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - heltall med lik verdi`() {
        val x = Faktum(heltallA, 42, gyldighetsperiode)
        val y = Faktum(heltallA, 42, gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - heltall med ulik verdi`() {
        val x = Faktum(heltallA, 42, gyldighetsperiode)
        val y = Faktum(heltallA, 43, gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - desimaltall med lik verdi`() {
        val x = Faktum(desimaltall, 3.14, gyldighetsperiode)
        val y = Faktum(desimaltall, 3.14, gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - desimaltall med ulik verdi`() {
        val x = Faktum(desimaltall, 3.14, gyldighetsperiode)
        val y = Faktum(desimaltall, 2.71, gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - tekst med lik verdi`() {
        val type = Opplysningstype.tekst(Id(UUIDv7.ny(), Tekst), "tekst")
        val x = Faktum(type, "hello", gyldighetsperiode)
        val y = Faktum(type, "hello", gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - tekst med ulik verdi`() {
        val type = Opplysningstype.tekst(Id(UUIDv7.ny(), Tekst), "tekst")
        val x = Faktum(type, "hello", gyldighetsperiode)
        val y = Faktum(type, "world", gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - beløp med lik verdi`() {
        val x = Faktum(beløpA, Beløp(1000.0), gyldighetsperiode)
        val y = Faktum(beløpA, Beløp(1000.0), gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - beløp med ulik verdi`() {
        val x = Faktum(beløpA, Beløp(1000.0), gyldighetsperiode)
        val y = Faktum(beløpA, Beløp(2000.0), gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - ulid med lik verdi`() {
        val verdi = Ulid("01ARZ3NDEKTSV4RRFFQ69G5FAV")
        val x = Faktum(ulid, verdi, gyldighetsperiode)
        val y = Faktum(ulid, verdi, gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - ulid med ulik verdi`() {
        val x = Faktum(ulid, Ulid("01ARZ3NDEKTSV4RRFFQ69G5FAV"), gyldighetsperiode)
        val y = Faktum(ulid, Ulid("01BRZ3NDEKTSV4RRFFQ69G5FAV"), gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - periode med lik verdi`() {
        val type = Opplysningstype.periode(Id(UUIDv7.ny(), PeriodeDataType), "periode")
        val verdi = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30))
        val x = Faktum(type, verdi, gyldighetsperiode)
        val y = Faktum(type, verdi, gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - periode med ulik verdi`() {
        val type = Opplysningstype.periode(Id(UUIDv7.ny(), PeriodeDataType), "periode")
        val x = Faktum(type, Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)), gyldighetsperiode)
        val y = Faktum(type, Periode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 6, 30)), gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - barn med lik verdi`() {
        val type = Opplysningstype.barn(Id(UUIDv7.ny(), BarnDatatype), "barn")
        val verdi = BarnListe(barn = listOf(Barn(LocalDate.of(2020, 1, 1), "Per", "Hansen", "NO", true)))
        val x = Faktum(type, verdi, gyldighetsperiode)
        val y = Faktum(type, verdi, gyldighetsperiode)
        x.erLik(y) shouldBe true
    }

    @Test
    fun `erLik - barn med ulik verdi`() {
        val type = Opplysningstype.barn(Id(UUIDv7.ny(), BarnDatatype), "barn")
        val x = Faktum(type, BarnListe(barn = listOf(Barn(LocalDate.of(2020, 1, 1), "Per", "Hansen", "NO", true))), gyldighetsperiode)
        val y = Faktum(type, BarnListe(barn = listOf(Barn(LocalDate.of(2021, 1, 1), "Kari", "Olsen", "NO", false))), gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - ulik gyldighetsperiode`() {
        val x = Faktum(a, true, Gyldighetsperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)))
        val y = Faktum(a, true, Gyldighetsperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
        x.erLik(y) shouldBe false
    }

    @Test
    fun `erLik - ulik opplysningstype`() {
        val typeA = Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "typeA")
        val typeB = Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "typeB")
        val x = Faktum(typeA, true, gyldighetsperiode)
        val y = Faktum(typeB, true, gyldighetsperiode)
        x.erLik(y) shouldBe false
    }

    private object RedigerbarPerOpplysningstype : Redigerbar {
        private val redigerbare = setOf(dato1)

        override fun kanRedigere(opplysningstype: Opplysningstype<*>): Boolean = redigerbare.contains(opplysningstype)
    }
}

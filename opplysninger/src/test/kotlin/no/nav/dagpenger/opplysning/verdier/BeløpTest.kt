package no.nav.dagpenger.opplysning.verdier

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BeløpTest {
    @Test
    fun blupr() {
        val b = Money.of(BigDecimal("10.00000000000000000006"), "NOK")
        println(b.toString())
    }

    @Test
    fun `likhet`() {
        val beløp = Beløp(100.0)
        beløp shouldBe Beløp(100.0)
        beløp shouldBe beløp
        beløp shouldNotBe Beløp(200.0)
        beløp shouldNotBe Beløp(100.1)
        beløp shouldNotBe Any()
    }

    @Test
    fun `kan addere og multiplisere`() {
        val beløp = Beløp(100.0)
        val beløp2 = Beløp(200.0)
        val beløp3 = Beløp(300.0)
        beløp + beløp2 shouldBe beløp3
        beløp * 3.0 shouldBe beløp3
    }

    @Test
    fun `kan dele`() {
        val beløp = Beløp(100.0)
        val beløp2 = Beløp(200.0)
        beløp2 / 2.0 shouldBe beløp

        Beløp(100.0) / 3.0 shouldBe Beløp("NOK 33.33333333333333333333")
        Beløp(100000.0) / 3.0 shouldBe Beløp("NOK 33333.33333333333333333333")
        Beløp("NOK 33.33333333333333333333") / Beløp("NOK 33.33333333333333333333") shouldBe Beløp("NOK 1")
    }

    @Test
    fun `kan gi resultat i kroner og ører`() {
        val beløp1 = Beløp(100.0)
        val beløp2 = Beløp(200.0)
        val beløp3 = beløp1 + beløp2 * 2.0
        val beløp4 = beløp3 / 3.0

        beløp4 shouldBe Beløp("NOK 166.66666666666666666667")
        beløp4.uavrundet.doubleValueExact() shouldBe 166.66666666666666
        beløp4.avrundet.doubleValueExact() shouldBe 167.0
        beløp4.avrundet.numberValue(BigDecimal::class.java) shouldBe BigDecimal("167")
        beløp4.avrundet.intValueExact() shouldBe 167

        beløp4.heleKroner.doubleValueExact() shouldBe 167.0
        beløp4.heleKroner.intValueExact() shouldBe 167
    }

    @Test
    fun `test avrundingsmetoder `() {
        val beløp = Beløp("NOK 33.555")
        beløp.avrundet.toInt() shouldBe 34
        beløp.heleKroner.toInt() shouldBe 34
        beløp.avrundetNedover.toInt() shouldBe 33
    }
}

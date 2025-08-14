package no.nav.dagpenger.opplysning.verdier.enhet

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.tilTimer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.timer
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TimerTest {
    @Test
    fun `likhet test`() {
        val timer = Timer(2.5)
        timer shouldBe timer
        timer shouldBe Timer(2.5)
        2.5.timer shouldBe timer
        timer.hashCode() shouldBe Timer(2.5).hashCode()
        timer shouldBe Timer(2.5)
        timer shouldBe 150.minutes.tilTimer
        timer shouldNotBe 100.minutes.tilTimer
        timer shouldNotBe Any()
        timer shouldNotBe null

        2.5.hours.tilTimer shouldBe Timer(2.5)

        (2.5.hours + 30.minutes).tilTimer shouldBe (2.5.timer + 0.5.timer)
    }
}

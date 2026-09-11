package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RettighetsperiodeTest {
    @Test
    fun `sammenligner rettighetsperioder på fraOgMed`() {
        val tidlig = Rettighetsperiode(1.januar, 31.januar, harRett = true, endret = false)
        val sen = Rettighetsperiode(1.februar, LocalDate.MAX, harRett = false, endret = true)

        (tidlig < sen) shouldBe true
        (sen > tidlig) shouldBe true
        tidlig.compareTo(tidlig) shouldBe 0
    }

    @Test
    fun `sorterer rettighetsperioder kronologisk uavhengig av opprinnelig rekkefølge`() {
        val først = Rettighetsperiode(1.januar, 28.februar, harRett = true, endret = false)
        val andre = Rettighetsperiode(1.mars, 30.april, harRett = false, endret = true)
        val tredje = Rettighetsperiode(1.mai, LocalDate.MAX, harRett = true, endret = true)

        val usortert = listOf(tredje, først, andre)

        usortert.sorted().shouldContainExactly(først, andre, tredje)
    }

    @Test
    fun `beholder rekkefølgen for perioder med lik fraOgMed`() {
        val original = Rettighetsperiode(1.januar, 31.januar, harRett = true, endret = false, opphevetStans = false)
        val omgjort = Rettighetsperiode(1.januar, 31.januar, harRett = false, endret = true, opphevetStans = true)

        listOf(omgjort, original).sorted().shouldContainExactly(omgjort, original)
    }
}

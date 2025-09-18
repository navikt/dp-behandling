package no.nav.dagpenger.opplysning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RettighetsperiodeTest {
    @Test
    fun `perioder er unike i set`() {
        val periode1 = Rettighetsperiode(1.januar, 31.januar, true)
        val periode2 = Rettighetsperiode(1.februar, 28.februar, true)
        val periode3 = Rettighetsperiode(1.januar, 31.januar, true) // Samme som periode1

        val perioderSet = setOf(periode1, periode2, periode3)

        assertEquals(2, perioderSet.size) // Bør være 2 fordi periode1 og periode3 er like
        assertTrue(perioderSet.contains(periode1))
        assertTrue(perioderSet.contains(periode2))
    }
}

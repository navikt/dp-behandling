package no.nav.dagpenger.opplysning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

internal class TemporalCollectionTest {
    private lateinit var satser: TemporalCollection<BigDecimal>
    private val lavSats = BigDecimal(5)
    private val høySats = BigDecimal(10)

    @BeforeEach
    fun setUp() {
        satser = TemporalCollection()
        satser.put(1.mars, lavSats)
        satser.put(4.juli, høySats)
    }

    @Test
    fun `får riktig sats til riktig dato`() {
        assertThrows<IllegalArgumentException> {
            satser.get(1.januar)
        }
        assertEquals(lavSats, satser.get(1.mars))
        assertEquals(lavSats, satser.get(1.juli))
        assertEquals(høySats, satser.get(4.juli))
        assertEquals(høySats, satser.get(15.juli))
    }

    @Test
    fun `Satt på samme dato`() {
        satser.put(7.juli, lavSats)
        satser.put(7.juli, BigDecimal(20))

        assertEquals(BigDecimal(20), satser.get(7.juli))
    }

    @Test
    fun `Skal gi riktig verdi uansett innsettingsrekkefølge`() {
        val temporal = TemporalCollection<String>()

        temporal.put(LocalDate.parse("2025-05-20"), "R1")
        temporal.put(LocalDate.parse("2024-03-10"), "R2") // ut av rekkefølge
        temporal.put(LocalDate.parse("2025-09-30"), "R3")

        val result = temporal.get(LocalDate.parse("2025-05-21"))

        assertEquals("R1", result) // fungerer som forventet
    }

    @Test
    fun `get kaster exception hvis ingen data er gamle nok`() {
        val temporal = TemporalCollection<String>()
        temporal.put(LocalDate.parse("2025-05-20"), "R1")

        assertThrows<IllegalArgumentException> {
            temporal.get(LocalDate.parse("2020-01-01"))
        }
    }
}

package no.nav.dagpenger.aktivitetslogg

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AktivitetsloggTest {
    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var person: TestKontekst

    @BeforeEach
    fun setUp() {
        person = TestKontekst("Person")
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `info should add an Aktivitet Info to the log`() {
        aktivitetslogg.info("This is an info message")
        assertEquals(1, aktivitetslogg.aktivitetsteller())
        assertTrue(aktivitetslogg.behov().isEmpty())
    }

    @Test
    fun `inneholder original melding`() {
        val infomelding = "info message"
        aktivitetslogg.info(infomelding)
        assertInfo(infomelding)
    }

    @Test
    fun `severe oppdaget og kaster exception`() {
        val melding = "Severe error"
        org.junit.jupiter.api.assertThrows<Aktivitetslogg.AktivitetException> { aktivitetslogg.severe(melding) }
        // assertTrue(aktivitetslogg.hasErrors())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertSevere(melding)
    }

    @Test
    fun `Melding sendt til forelder`() {
        val hendelse = TestHendelse(
            "Hendelse",
            aktivitetslogg.barn(),
        )
        "info message".also {
            hendelse.info(it)
            assertInfo(it, hendelse.logg)
            assertInfo(it, aktivitetslogg)
        }
    }

    @Test
    fun `Melding sendt fra barnebarn til forelder`() {
        val hendelse = TestHendelse(
            "Hendelse",
            aktivitetslogg.barn(),
        )
        hendelse.kontekst(person)
        val arbeidsgiver =
            TestKontekst("Melding")
        hendelse.kontekst(arbeidsgiver)
        val vedtaksperiode =
            TestKontekst("Soknad")
        hendelse.kontekst(vedtaksperiode)
        "info message".also {
            hendelse.info(it)
            assertInfo(it, hendelse.logg)
            assertInfo(it, aktivitetslogg)
        }
    }

    @Test
    fun `Vis bare arbeidsgiveraktivitet`() {
        val hendelse1 = TestHendelse(
            "Hendelse1",
            aktivitetslogg.barn(),
        )
        hendelse1.kontekst(person)
        val arbeidsgiver1 =
            TestKontekst("Arbeidsgiver 1")
        hendelse1.kontekst(arbeidsgiver1)
        val vedtaksperiode1 =
            TestKontekst("Vedtaksperiode 1")
        hendelse1.kontekst(vedtaksperiode1)
        hendelse1.info("info message")
        hendelse1.info("annen info message")
        val hendelse2 = TestHendelse(
            "Hendelse2",
            aktivitetslogg.barn(),
        )
        hendelse2.kontekst(person)
        val arbeidsgiver2 =
            TestKontekst("Arbeidsgiver 2")
        hendelse2.kontekst(arbeidsgiver2)
        val vedtaksperiode2 =
            TestKontekst("Vedtaksperiode 2")
        hendelse2.kontekst(vedtaksperiode2)
        hendelse2.info("info message")
        assertEquals(3, aktivitetslogg.aktivitetsteller())
        assertEquals(2, aktivitetslogg.logg(vedtaksperiode1).aktivitetsteller())
        assertEquals(1, aktivitetslogg.logg(arbeidsgiver2).aktivitetsteller())
    }

    @Test
    fun `Behov kan ha detaljer`() {
        val hendelse1 = TestHendelse(
            "Hendelse1",
            aktivitetslogg.barn(),
        )
        hendelse1.kontekst(person)
        val param1 = "value"
        val param2 = LocalDate.now()
        hendelse1.behov(
            type = TestBehov.Test,
            melding = "Behov om test",
            detaljer = mapOf(
                "param1" to param1,
                "param2" to param2,
            ),
        )

        assertEquals(1, aktivitetslogg.behov().size)
        assertEquals(1, aktivitetslogg.behov().first().kontekst().size)
        assertEquals(2, aktivitetslogg.behov().first().detaljer().size)
        assertEquals("Person", aktivitetslogg.behov().first().kontekst()["Person"])
        assertEquals(param1, aktivitetslogg.behov().first().detaljer()["param1"])
        assertEquals(param2, aktivitetslogg.behov().first().detaljer()["param2"])
    }
    private fun assertSevere(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(
            object : AktivitetsloggVisitor {
                override fun visitSevere(
                    kontekster: List<SpesifikkKontekst>,
                    aktivitet: Aktivitet.Severe,
                    melding: String,
                    tidsstempel: String,
                ) {
                    visitorCalled = true
                    assertEquals(message, melding)
                }
            },
        )
        assertTrue(visitorCalled)
    }

    private fun assertInfo(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(
            object : AktivitetsloggVisitor {
                override fun visitInfo(
                    id: UUID,
                    kontekster: List<SpesifikkKontekst>,
                    aktivitet: Aktivitet.Info,
                    melding: String,
                    tidsstempel: String,
                ) {
                    visitorCalled = true
                    assertEquals(message, melding)
                }
            },
        )
        assertTrue(visitorCalled)
    }

    private enum class TestBehov : Aktivitet.Behov.Behovtype {
        Test,
    }

    private class TestKontekst(
        private val melding: String,
    ) : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(melding, mapOf(melding to melding))
    }

    private class TestHendelse(
        private val melding: String,
        internal val logg: Aktivitetslogg,
    ) : Aktivitetskontekst, IAktivitetslogg by logg {
        init {
            logg.kontekst(this)
        }

        override fun toSpesifikkKontekst() = SpesifikkKontekst("TestHendelse")
        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }
    }
}

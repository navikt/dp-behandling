package no.nav.dagpenger.behandling.mediator
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.aktivitetslogg.aktivitet.Behov
import org.junit.jupiter.api.Test
import java.util.UUID

class BehovMediatorTest {
    private val rapid = TestRapid()
    private val mediator = BehovMediator()

    @Test
    fun `grupperer behov og avhengigheter`() {
        val hendelse = TestHendelse()

        hendelse.behov(
            TestBehov.TestBehov1,
            "test",
            mapOf(
                "test1" to "test1",
                "felles" to "felles",
                "@utledetAv" to listOf("test1", "felles"),
            ),
        )
        hendelse.behov(
            TestBehov.TestBehov2,
            "test",
            mapOf(
                "test2" to "test2",
                "felles" to "felles",
                "@utledetAv" to listOf("test2", "felles"),
            ),
        )

        mediator.håndter(rapid, hendelse)

        with(rapid.inspektør.message(0)) {
            this["@behov"].map { it.asText() } shouldContainExactly listOf("TestBehov1", "TestBehov2")

            this["TestBehov1"].size() shouldBe 2
            this["TestBehov1"]["test1"].asText() shouldBe "test1"
            this["TestBehov1"]["felles"].asText() shouldBe "felles"

            this["TestBehov2"].size() shouldBe 2
            this["TestBehov2"]["test2"].asText() shouldBe "test2"
            this["TestBehov2"]["felles"].asText() shouldBe "felles"

            this["@utledetAv"]["TestBehov1"].map { it.asText() }.shouldContainExactly("test1", "felles")
            this["@utledetAv"]["TestBehov2"].map { it.asText() }.shouldContainExactly("test2", "felles")
        }
    }

    private enum class TestBehov : Behov.Behovtype {
        TestBehov1,
        TestBehov2,
    }

    private class TestHendelse(
        private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    ) : AktivitetsloggHendelse,
        IAktivitetslogg by aktivitetslogg {
        init {
            aktivitetslogg.kontekst(this)
        }

        override fun toSpesifikkKontekst() = SpesifikkKontekst("test", mapOf("kontekst" to "kontekst"))

        override fun ident() = "123123"

        override fun meldingsreferanseId() = UUID.randomUUID()
    }
}

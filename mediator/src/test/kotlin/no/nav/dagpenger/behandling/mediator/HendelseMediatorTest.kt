package no.nav.dagpenger.behandling.mediator

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggObserver
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.aktivitetslogg.aktivitet.Hendelse
import no.nav.dagpenger.behandling.mediator.repository.PostgresUnitOfWork
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.uuid.UUIDv7
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class HendelseMediatorTest {
    private val rapid = TestRapid()

    @Test
    fun foobar() {
        val mediator = HendelseMediator(Outbox())
        val søknadId = UUIDv7.ny()
        val gjelderDato = LocalDate.now()
        val hendelse = SøknadInnsendtHendelse(UUIDv7.ny(), "ident", søknadId, gjelderDato, 1, LocalDateTime.now())
        val observer = TestObserver()
        hendelse.registrer(observer)
        hendelse.kontekst(hendelse)
        hendelse.kontekst(Testkontekst())

        hendelse.hendelse(FooBar.FOO, "foo")
        hendelse.hendelse(FooBar.BAR, "bar", mapOf("detaljA" to "verdiA", "detaljB" to "verdiB", "kontekstA" to "bar"))

        mediator.håndter(hendelse, PostgresUnitOfWork.transaction())
        rapid.inspektør.size shouldBe 2

        with(rapid.inspektør.message(0)) {
            this["@event_name"].asText() shouldBe "FOO"
            this["ident"].asText() shouldBe "ident"
            this["søknadId"].asUUID() shouldBe søknadId
            this["gjelderDato"].asLocalDate() shouldBe gjelderDato
            this["detaljA"].shouldBeNull()
            this["detaljB"].shouldBeNull()
            this["kontekstA"].asText() shouldBe "verdiA"
            this["kontekstB"].asText() shouldBe "verdiB"
        }
        with(rapid.inspektør.message(1)) {
            this["@event_name"].asText() shouldBe "BAR"
            this["ident"].asText() shouldBe "ident"
            this["søknadId"].asUUID() shouldBe søknadId
            this["gjelderDato"].asLocalDate() shouldBe gjelderDato
            this["detaljA"].asText() shouldBe "verdiA"
            this["detaljB"].asText() shouldBe "verdiB"
            this["kontekstA"].asText() shouldBe "verdiA"
            this["kontekstB"].asText() shouldBe "verdiB"
        }

        observer.lyttetPåFoo shouldBe true
        observer.verdiA shouldBe "verdiA"
    }

    private class TestObserver : AktivitetsloggObserver {
        var lyttetPåFoo: Boolean = false
        lateinit var verdiA: String

        override fun hendelse(
            id: UUID,
            label: Char,
            type: Hendelse.Hendelsetype,
            melding: String,
            kontekster: List<SpesifikkKontekst>,
            tidsstempel: LocalDateTime,
        ) {
            when (type) {
                FooBar.FOO -> tellA()
                FooBar.BAR -> tellB(kontekster.filterIsInstance<Testkontekst.TestKontekst>().single())
            }
        }

        private fun tellA() {
            lyttetPåFoo = true
        }

        private fun tellB(testKontekst: Testkontekst.TestKontekst) {
            verdiA = testKontekst.kontekstA
        }
    }

    private class Testkontekst : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = TestKontekst("verdiA", "verdiB")

        data class TestKontekst(
            val kontekstA: String,
            val kontekstB: String,
        ) : SpesifikkKontekst("egenKontekst") {
            override val kontekstMap = mapOf("kontekstA" to kontekstA, "kontekstB" to kontekstB)
        }
    }

    private enum class FooBar : Hendelse.Hendelsetype {
        FOO,
        BAR,
    }
}

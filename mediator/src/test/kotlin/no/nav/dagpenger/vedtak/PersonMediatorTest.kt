package no.nav.dagpenger.vedtak

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.vedtak.iverksetting.mediator.IverksettingMediator
import no.nav.dagpenger.vedtak.mediator.HendelseMediator
import no.nav.dagpenger.vedtak.mediator.Meldingsfabrikk
import no.nav.dagpenger.vedtak.mediator.Meldingsfabrikk.dagpengerAvslåttJson
import no.nav.dagpenger.vedtak.mediator.Meldingsfabrikk.dagpengerInnvilgetJson
import no.nav.dagpenger.vedtak.mediator.Meldingsfabrikk.rapporteringInnsendtHendelse
import no.nav.dagpenger.vedtak.mediator.Meldingsfabrikk.rapporteringPåPersonUtenRammevetak
import no.nav.dagpenger.vedtak.mediator.PersonMediator
import no.nav.dagpenger.vedtak.mediator.persistens.InMemoryMeldingRepository
import no.nav.dagpenger.vedtak.mediator.persistens.InMemoryPersonRepository
import no.nav.dagpenger.vedtak.mediator.vedtak.VedtakFattetKafkaObserver
import no.nav.dagpenger.vedtak.modell.PersonObserver
import no.nav.dagpenger.vedtak.modell.vedtak.VedtakObserver
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.Duration

internal class PersonMediatorTest {

    private val testRapid = TestRapid()
    private val ident = "11109233444"
    private val testObservatør = TestObservatør()
    private val personRepository = InMemoryPersonRepository()

    init {
        HendelseMediator(
            rapidsConnection = testRapid,
            hendelseRepository = InMemoryMeldingRepository(),
            personMediator = PersonMediator(
                personRepository = personRepository,
                personObservers = listOf(VedtakFattetKafkaObserver(testRapid), testObservatør),
            ),
            iverksettingMediator = IverksettingMediator(mockk(), mockk()),
        )
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
        personRepository.reset()
    }

    @Test
    fun `Innvilgelse av dagpenger hendelse fører til vedtak fattet event`() {
        testRapid.sendTestMessage(dagpengerInnvilgetJson(ident = ident))
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(testRapid.inspektør.size - 1).also {
            it["@event_name"].asText() shouldBe "vedtak_fattet"
        }
        testObservatør.vedtak.shouldNotBeEmpty()
    }

    @Test
    fun `Avslag av dagpenger hendelse fører til vedtak fattet event`() {
        testRapid.sendTestMessage(dagpengerAvslåttJson(ident = ident))
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(testRapid.inspektør.size - 1).also {
            it["@event_name"].asText() shouldBe "vedtak_fattet"
        }
        testObservatør.vedtak.shouldNotBeEmpty()
    }

    @Test
    fun `Rapporteringshendelse fører til et løpende vedtak fattet`() {
        val rettighetFraDato = 29 mai 2023
        testRapid.sendTestMessage(
            dagpengerInnvilgetJson(
                ident = ident,
                virkningsdato = rettighetFraDato,
                dagsats = 1000.0,
                fastsattVanligArbeidstid = 8,
            ),
        )

        testRapid.sendTestMessage(
            rapporteringInnsendtHendelse(
                ident = ident,
                fom = rettighetFraDato,
                tom = rettighetFraDato.plusDays(13),
                tidArbeidetPerArbeidsdag = Duration.parseIsoString("PT0S"),
            ),
        )

        testRapid.inspektør.size shouldBe 2

        testObservatør.vedtak.size shouldBe 1
        val rammevedtakJson = testRapid.inspektør.message(testRapid.inspektør.size - 2)
        rammevedtakJson["@event_name"].asText() shouldBe "vedtak_fattet"

        testObservatør.løpendeVedtak.size shouldBe 1
        val løpendeVedtakJson = testRapid.inspektør.message(testRapid.inspektør.size - 1)
        løpendeVedtakJson["@event_name"].asText() shouldBe "vedtak_fattet"
        løpendeVedtakJson["utbetalingsdager"].size() shouldBe 10
        løpendeVedtakJson["utbetalingsdager"].map { utbetalingsdagJson ->
            utbetalingsdagJson["beløp"].asDouble() shouldBe 1000.0
        }
    }

    @Test
    @Disabled
    fun `person finnes i PDL med annen ident`() {
        val endretFnr = "12345678912"
        testRapid.sendTestMessage(
            dagpengerInnvilgetJson(ident = ident),
        )

        testRapid.sendTestMessage(
            Meldingsfabrikk.rapporteringInnsendtJson(endretFnr),
        )
    }

    @Test
    @Disabled("Vi må finne hva vi skal gjøre rapportering som ikke har rammevedtak")
    fun `Dersom person vi prøver å rapportere for ikke har noe rammevedtak så da lager vi ikke et vedtak`() {
        val rettighetFraDato = 29 mai 2023
        testRapid.sendTestMessage(
            dagpengerInnvilgetJson(
                ident = ident,
                virkningsdato = rettighetFraDato,
                dagsats = 1000.0,
                fastsattVanligArbeidstid = 8,
            ),
        )

        testRapid.sendTestMessage(
            rapporteringPåPersonUtenRammevetak(),
        )

        testObservatør.vedtak.size shouldBe 1
        val rammevedtakJson = testRapid.inspektør.message(testRapid.inspektør.size - 1)
        rammevedtakJson["@event_name"].asText() shouldBe "vedtak_fattet"
    }

    private class TestObservatør : PersonObserver {

        val vedtak = mutableListOf<VedtakObserver.VedtakFattet>()
        val løpendeVedtak = mutableListOf<VedtakObserver.UtbetalingsvedtakFattet>()

        override fun vedtakFattet(ident: String, vedtakFattet: VedtakObserver.VedtakFattet) {
            vedtak.add(vedtakFattet)
        }

        override fun utbetalingsvedtakFattet(
            ident: String,
            utbetalingsvedtakFattet: VedtakObserver.UtbetalingsvedtakFattet,
        ) {
            løpendeVedtak.add(utbetalingsvedtakFattet)
        }
    }
}

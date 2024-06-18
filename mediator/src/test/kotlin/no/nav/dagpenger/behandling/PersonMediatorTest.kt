package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.getunleash.FakeUnleash
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.mediator.BehovMediator
import no.nav.dagpenger.behandling.mediator.HendelseMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.PersonMediator
import no.nav.dagpenger.behandling.mediator.melding.PostgresHendelseRepository
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.behandling.modell.BehandlingBehov.AvklaringManuellBehandling
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.regel.Behov.HelseTilAlleTyperJobb
import no.nav.dagpenger.regel.Behov.InntektId
import no.nav.dagpenger.regel.Behov.KanJobbeDeltid
import no.nav.dagpenger.regel.Behov.KanJobbeHvorSomHelst
import no.nav.dagpenger.regel.Behov.Lønnsgaranti
import no.nav.dagpenger.regel.Behov.OpptjeningsperiodeFraOgMed
import no.nav.dagpenger.regel.Behov.Ordinær
import no.nav.dagpenger.regel.Behov.Permittert
import no.nav.dagpenger.regel.Behov.PermittertFiskeforedling
import no.nav.dagpenger.regel.Behov.RegistrertSomArbeidssøker
import no.nav.dagpenger.regel.Behov.SisteAvsluttendeKalenderMåned
import no.nav.dagpenger.regel.Behov.TarUtdanningEllerOpplæring
import no.nav.dagpenger.regel.Behov.Verneplikt
import no.nav.dagpenger.regel.Behov.VilligTilÅBytteYrke
import no.nav.dagpenger.regel.SøknadInnsendtRegelsett
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period

internal class PersonMediatorTest {
    private val rapid = TestRapid()
    private val ident = "11109233444"

    private val personRepository = PersonRepositoryPostgres(BehandlingRepositoryPostgres(OpplysningerRepositoryPostgres()))

    private val unleash =
        FakeUnleash().also {
            it.enable("bruk-soknad-orkestrator")
        }
    private val personMediator =
        PersonMediator(
            personRepository = personRepository,
            aktivitetsloggMediator = mockk(relaxed = true),
            behovMediator = BehovMediator(rapid, unleash),
            hendelseMediator = HendelseMediator(rapid),
            observatører = emptySet(),
        )

    init {
        MessageMediator(
            rapidsConnection = rapid,
            personMediator = personMediator,
            hendelseRepository = PostgresHendelseRepository(),
            SøknadInnsendtRegelsett.regelsett.flatMap { it.produserer() }.toSet(),
        )
    }

    @BeforeEach
    fun setUp() {
        rapid.reset()
    }

    @Test
    fun `søknad med for lite inntekt skal automatisk avslås`() =
        withMigratedDb {
            val testPerson =
                TestPerson(
                    ident,
                    rapid,
                    søknadstidspunkt = 6.mai(2021),
                )
            løsBehandlingFramTilFerdig(testPerson)

            /**
             * Avklarer om den krever manuell behandling
             */
            rapid.harBehov(AvklaringManuellBehandling.name)
            testPerson.løsBehov(AvklaringManuellBehandling.name, false)

            personRepository.hent(ident.tilPersonIdentfikator()).also {
                it.shouldNotBeNull()
                it.behandlinger().size shouldBe 1
                it.behandlinger().flatMap { behandling -> behandling.opplysninger().finnAlle() }.size shouldBe 58
            }
            rapid.harHendelse("vedtak_fattet") {
                medBoolsk("utfall") shouldBe false
                medTekst("fagsakId").shouldBeNull()
                medTekst("søknadId") shouldBe testPerson.søknadId

                medOpplysning<Int>("fagsakId") shouldBe 123
                medOpplysning<Boolean>("Ordinær") shouldBe false
            }

            rapid.inspektør.size shouldBe 11
        }

    @Test
    fun `søknad som slår ut på manuelle behandling må føre til forslag til vedtak`() =
        withMigratedDb {
            val testPerson =
                TestPerson(
                    ident,
                    rapid,
                    søknadstidspunkt = 6.mai(2021),
                )

            løsBehandlingFramTilFerdig(testPerson)

            /**
             * Avklarer om den krever manuell behandling
             */
            testPerson.løsBehov(
                AvklaringManuellBehandling.name,
                true,
                data = mapOf("vurderinger" to listOf(mapOf("type" to "type", "utfall" to "Manuell", "begrunnelse" to "begrunnelse"))),
            )

            rapid.harHendelse("forslag_til_vedtak") {
                println(this)
                medTekst("søknadId") shouldBe testPerson.søknadId
                with(medNode("avklaringer")) {
                    this.size() shouldBe 1
                    val avklaring = this.first()
                    avklaring["type"].asText() shouldBe "type"
                    avklaring["begrunnelse"].asText() shouldBe "begrunnelse"
                    avklaring["utfall"].asText() shouldBe "Manuell"
                }
            }

            rapid.inspektør.size shouldBe 11
        }

    @Test
    fun `e2e av søknad som blir avbrutt `() =
        withMigratedDb {
            val testPerson =
                TestPerson(
                    ident,
                    rapid,
                    søknadstidspunkt = 6.mai(2021),
                )
            testPerson.sendSøknad()
            rapid.harHendelse("behandling_opprettet", offset = 2)

            /**
             * Avbryter behandlingen før svar på manuell behandling (feks skjerming av person)
             */
            testPerson.avbrytBehandling()

            rapid.harHendelse("behandling_avbrutt") {
                medTekst("søknadId") shouldBe testPerson.søknadId
            }

            /**
             * Avklarer om den krever manuell behandling kommer etter avbrutt behandling
             */
            testPerson.løsBehov(AvklaringManuellBehandling.name, false)

            rapid.inspektør.size shouldBe 3
        }

    @Test
    fun `søker i overgangen til ny rapporteringsfrist`() =
        withMigratedDb {
            val testPerson =
                TestPerson(
                    ident,
                    rapid,
                    søknadstidspunkt = 5.juni(2024),
                    innsendt = 5.juni(2024).atTime(12, 0),
                )
            løsBehandlingFramTilFerdig(testPerson)

            rapid.harHendelse("behandling_avbrutt") {
                medTekst("søknadId") shouldBe testPerson.søknadId
            }
        }

    @Test
    fun `søker etter overgang til ny rapporteringsfrist`() =
        withMigratedDb {
            val testPerson =
                TestPerson(
                    ident,
                    rapid,
                    søknadstidspunkt = 7.juni(2024),
                    innsendt = 7.juni(2024).atTime(12, 0),
                )
            løsBehandlingFramTilFerdig(testPerson)

            rapid.harHendelse("behov")
        }

    private fun løsBehandlingFramTilFerdig(testPerson: TestPerson) {
        testPerson.sendSøknad()
        rapid.harHendelse("behandling_opprettet", offset = 2)

        /**
         * Fastsetter søknadstidspunkt
         */
        rapid.harBehov("Søknadstidspunkt") {
            medTekst("søknad_uuid") shouldNotBe testPerson.søknadId
            medNode("InnsendtSøknadsId")["urn"].asText() shouldBe "urn:soknad:${testPerson.søknadId}"
        }
        rapid.harFelt {
            medBoolsk("bruk-søknad-orkestrator") shouldBe true
        }

        rapid.harBehov("Fødselsdato", "Søknadstidspunkt", "ØnskerDagpengerFraDato")
        testPerson.løsBehov("Fødselsdato", "Søknadstidspunkt", "ØnskerDagpengerFraDato")

        /**
         * Fastsetter opptjeningsperiode og inntekt. Pt brukes opptjeningsperiode generert fra dp-inntekt
         */
        rapid.harBehov(InntektId) {
            medDato("Virkningsdato") shouldBe testPerson.søknadstidspunkt
            /**
             * TODO: Vi må ta vekk opptjeningsperiode fra dp-inntekt og skive om måten dp-inntekt lagrer inntekt på beregningsdato
             * medDato(OpptjeningsperiodeFraOgMed) shouldBe 1.april(2018)
             * opptjeningsperiodeEr(måneder = 36)
             */
        }
        testPerson.løsBehov(InntektId)

        /**
         * Sjekker kravene til inntekt
         */
        rapid.harBehov("InntektSiste12Mnd") { medTekst("InntektId") shouldBe testPerson.inntektId }
        rapid.harBehov("InntektSiste36Mnd") { medTekst("InntektId") shouldBe testPerson.inntektId }

        testPerson.løsBehov("InntektSiste12Mnd", "InntektSiste36Mnd")

        /**
         * Sjekker om mulig verneplikt
         */
        rapid.harBehov(Verneplikt)
        testPerson.løsBehov(Verneplikt)

        /**
         * Sjekker kravene til reell arbeidssøker
         */
        rapid.harBehov(KanJobbeDeltid, KanJobbeHvorSomHelst, HelseTilAlleTyperJobb, VilligTilÅBytteYrke)
        testPerson.løsBehov(KanJobbeDeltid, KanJobbeHvorSomHelst, HelseTilAlleTyperJobb, VilligTilÅBytteYrke)

        /**
         * Sjekker kravet til registrering som arbeidssøker
         */
        rapid.harBehov(RegistrertSomArbeidssøker)
        testPerson.løsBehov(RegistrertSomArbeidssøker)

        /**
         * Innhenter rettighetstype
         */
        rapid.harBehov(Ordinær, Permittert, Lønnsgaranti, PermittertFiskeforedling)
        testPerson.løsBehov(Ordinær, Permittert, Lønnsgaranti, PermittertFiskeforedling)

        /**
         * Innhenter tar utdanning eller opplæring
         */
        rapid.harBehov(TarUtdanningEllerOpplæring)
        testPerson.løsBehov(TarUtdanningEllerOpplæring)
    }

    private fun Meldingsinnhold.opptjeningsperiodeEr(måneder: Int) {
        val periode = Period.between(medDato(OpptjeningsperiodeFraOgMed), medDato(SisteAvsluttendeKalenderMåned)) + Period.ofMonths(1)
        withClue("Opptjeningsperiode skal være 3 år") { periode.toTotalMonths() shouldBe måneder }
    }
}

private fun TestRapid.harHendelse(
    navn: String,
    offset: Int = 1,
    block: Meldingsinnhold.() -> Unit = {},
) {
    val message = inspektør.message(inspektør.size - offset)
    withClue("Siste melding på rapiden skal inneholde hendelse: $navn") {
        message["@event_name"].asText() shouldBe navn
    }
    Meldingsinnhold(message).apply { block() }
}

private fun TestRapid.harBehov(
    vararg behov: String,
    melding: Int = 1,
) {
    withClue("Siste melding på rapiden skal inneholde behov: ${behov.toList()}") {
        inspektør.message(inspektør.size - melding)["@behov"].map { it.asText() } shouldContainAll behov.toList()
    }
}

private fun TestRapid.harBehov(
    behov: String,
    block: Meldingsinnhold.() -> Unit,
) {
    harBehov(behov)
    Meldingsinnhold(inspektør.message(inspektør.size - 1)[behov]).apply { block() }
}

private fun TestRapid.harFelt(block: Meldingsinnhold.() -> Unit) {
    Meldingsinnhold(inspektør.message(inspektør.size - 1)).apply { block() }
}

private class Meldingsinnhold(
    private val message: JsonNode,
) {
    fun medNode(navn: String) = message.get(navn)

    fun medTekst(navn: String) = message.get(navn)?.asText()

    fun medDato(navn: String) = message.get(navn)?.asLocalDate()

    fun medBoolsk(navn: String) = message.get(navn)?.asBoolean()

    fun medOpplysning(navn: String) = message.get("opplysninger").single { it["opplysningstype"]["id"].asText() == navn }

    inline fun <reified T> medOpplysning(navn: String): T =
        when (T::class) {
            Boolean::class -> medOpplysning(navn)["verdi"].asBoolean() as T
            String::class -> medOpplysning(navn)["verdi"].asText() as T
            LocalDate::class -> medOpplysning(navn)["verdi"].asLocalDate() as T
            Int::class -> medOpplysning(navn)["verdi"].asInt() as T
            else -> throw IllegalArgumentException("Ukjent type")
        }
}

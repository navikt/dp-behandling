package no.nav.dagpenger.behandling.mediator.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.test.TestContext
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.mockk.slot
import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningstypeDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlersVurderingerDTO
import no.nav.dagpenger.behandling.konfigurasjon.Configuration
import no.nav.dagpenger.behandling.mediator.api.TestApplication.autentisert
import no.nav.dagpenger.behandling.mediator.api.TestApplication.testAzureAdToken
import no.nav.dagpenger.behandling.mediator.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.scenario.SimulertDagpengerSystem
import no.nav.dagpenger.behandling.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.regel.ReellArbeidssøker
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class BehandlingApiTest {
    @Test
    fun `ikke autentiserte kall returnerer 401`() {
        medSikretBehandlingApi {
            val response =
                it.client.post("/behandling") {
                    setBody("""{"ident":"${person.ident}"}""")
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `kall uten saksbehandlingsADgruppe i claims returnerer 401`() {
        medSikretBehandlingApi { testContext ->
            testContext
                .autentisert(
                    endepunkt = "/behandling",
                    body = """{"ident":"${person.ident}"}""",
                    token = testAzureAdToken(ADGrupper = emptyList(), navIdent = "123"),
                ).status shouldBe HttpStatusCode.Unauthorized

            testContext
                .autentisert(
                    endepunkt = "/behandling",
                    body = """{"ident":"${person.ident}"}""",
                    token = testAzureAdToken(ADGrupper = listOf("ikke-saksbehandler"), navIdent = "123"),
                ).status shouldBe HttpStatusCode.Unauthorized

            testContext
                .autentisert(
                    endepunkt = "/behandling",
                    body = """{"ident":"${person.ident}"}""",
                    token = testAzureAdToken(ADGrupper = listOf("dagpenger-saksbehandler"), navIdent = "123"),
                ).status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `hent opplysningstyper`() {
        medSikretBehandlingApi { testContext ->
            val response = testContext.autentisert(httpMethod = HttpMethod.Get, "/opplysningstyper")
            response.status shouldBe HttpStatusCode.OK
            val opplysningstyper =
                shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), object : TypeReference<List<OpplysningstypeDTO>>() {}) }
            opplysningstyper.shouldNotBeEmpty()
        }
    }

    @Test
    @Disabled("testen er avhengig av at hendelsemediator ikke er mocket - er del jobb.")
    fun `opprett behandling på en gitt person`() {
        medSikretBehandlingApi { testContext ->
            val response = testContext.autentisert(endepunkt = "/person/behandling", body = """{"ident":"${person.ident}"}""")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()
        }
    }

    @Test
    fun `hent behandlinger gitt person`() {
        medSikretBehandlingApi { testContext ->
            val response = testContext.autentisert(endepunkt = "/behandling", body = """{"ident":"${person.ident}"}""")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe "[]"
            auditlogg.aktivitet shouldContainExactly listOf("les")
        }
    }

    @Test
    fun `gir 404 hvis person ikke eksisterer`() {
        medSikretBehandlingApi { testContext ->
            val response = testContext.autentisert(endepunkt = "/behandling", body = """{"ident":"09876543311"}""")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `hent behandling gitt behandlingId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger()
            behovsløsere.løsTilForslag()

            val response = testContext.autentisert(httpMethod = HttpMethod.Get, endepunkt = "/behandling/${person.behandlingId}")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()

            val behandlingDto = shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), BehandlingDTO::class.java) }
            behandlingDto.behandlingId shouldBe person.behandlingId
            behandlingDto.vilkår.shouldNotBeEmpty()
            behandlingDto.opplysninger.all { it.redigerbar } shouldBe false
            behandlingDto.avklaringer.shouldNotBeEmpty()

            with(behandlingDto.behandletHendelse) {
                shouldNotBeNull()
                type shouldBe HendelseDTOTypeDTO.SØKNAD
            }
            with(behandlingDto.vilkår.single { it.navn == "Minsteinntekt" }) {
                avklaringer shouldHaveSize 1
                avklaringer.any { it.kode == "InntektNesteKalendermåned" } shouldBe true
            }

            behandlingDto.avklaringer shouldHaveSize 5
            auditlogg.aktivitet shouldContainExactly listOf("les")
        }
    }

    @Test
    @Disabled("Scenariotest må få støtte for å endre opplysninger og sånt")
    fun `hent saksbehandlers vurderinger for en gitt behandlingId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger()
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()

            val response = testContext.autentisert(HttpMethod.Get, "/behandling/${person.behandlingId}/vurderinger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()

            val behandlingDto =
                shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), SaksbehandlersVurderingerDTO::class.java) }
            behandlingDto.behandlingId shouldBe person.behandlingId

            behandlingDto.regelsett.shouldBeEmpty()
            behandlingDto.avklaringer.shouldNotBeEmpty()
            behandlingDto.opplysninger.shouldBeEmpty()

            with(behandlingDto.regelsett.single { it.navn == "Søknadstidspunkt" }) {
                avklaringer.shouldBeEmpty()
                opplysningIder?.shouldHaveSize(1)
            }

            with(behandlingDto.avklaringer.single { it.kode == "tittel 2" }) {
                avklartAv.shouldBeInstanceOf<SaksbehandlerDTO>()
            }

            behandlingDto.opplysninger shouldHaveSize 2
            behandlingDto.opplysninger.all { it.kilde?.type?.value == "Saksbehandler" } shouldBe true
        }
    }

    @Test
    fun `lagrer saksbehandlers begrunnelse for en gitt kildeId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger()
            behovsløsere.løsTilForslag()

            val kildeId = saksbehandler.endreOpplysning(ReellArbeidssøker.erArbeidsfør, false)

            testContext.autentisert(
                httpMethod = HttpMethod.Put,
                endepunkt = "/behandling/${person.behandlingId}/vurderinger/$kildeId",
                body = """{ "begrunnelse":"tekst" }""",
            )

            val response =
                testContext.autentisert(httpMethod = HttpMethod.Get, endepunkt = "/behandling/${person.behandlingId}/vurderinger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()
            val behandlingDto =
                shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), SaksbehandlersVurderingerDTO::class.java) }
            behandlingDto.behandlingId shouldBe person.behandlingId

            behandlingDto.regelsett.shouldNotBeEmpty()
            behandlingDto.avklaringer.shouldBeEmpty()
            behandlingDto.opplysninger.shouldNotBeEmpty()

            with(behandlingDto.regelsett.single { it.navn == "Reell arbeidssøker" }) {
                avklaringer.shouldBeEmpty()
                opplysningIder.shouldHaveSize(1)
            }

            behandlingDto.opplysninger shouldHaveSize 1
            behandlingDto.opplysninger.all { it.kilde?.type?.value == "Saksbehandler" } shouldBe true
        }
    }

    @Test
    fun `avbryt behandling gitt behandlingId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger()
            behovsløsere.løsTilForslag()

            val behandlingId = person.behandlingId
            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/$behandlingId/avbryt",
                    body = """{"ident":"${person.ident}"}""",
                )
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText().shouldBeEmpty()

            // TODO: Assert ny tilstand
        }
    }

    @Test
    fun `rekjør behandling med gitt behandlingId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger()
            behovsløsere.løsTilForslag()

            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/${person.behandlingId}/rekjor",
                    body = """{"ident":"${person.ident}"}""",
                )
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText().shouldBeEmpty()

            // TODO: Assertions
        }
    }

    @Test
    fun `test overgangene for behandling mellom saksbehandler og beslutter`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger()
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()

            val behandlingId = person.behandlingId
            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/$behandlingId/godkjenn",
                    body = """{"ident":"${person.ident}"}""",
                )
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText().shouldBeEmpty()

            // TODO: Assert godkjenning
            this.rapidInspektør
            // Send tilbake til saksbehandler
            testContext
                .autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/$behandlingId/send-tilbake",
                    body = """{"ident":"${person.ident}"}""",
                ).status shouldBe HttpStatusCode.Created

            // TODO: Assert at den er tilbake til godkjenning

            // Godkjenn igjen
            testContext
                .autentisert(
                    HttpMethod.Post,
                    "/behandling/$behandlingId/godkjenn",
                    """{"ident":"${person.ident}"}""",
                ).status shouldBe HttpStatusCode.Created

            // TODO: Assert at den er til beslutter igjen

            // Beslutt
            testContext
                .autentisert(
                    HttpMethod.Post,
                    "/behandling/$behandlingId/beslutt",
                    """{"ident":"${person.ident}"}""",
                    token =
                        testAzureAdToken(
                            ADGrupper = listOf(Configuration.properties[Configuration.Grupper.saksbehandler]),
                            navIdent = "555",
                        ),
                ).status shouldBe HttpStatusCode.Created

            // TODO: Assert vedtak
        }
    }

    @Test
    @Disabled("Oppdater til å fungere med ScenarioTesto")
    fun `kan endre alle typer opplysninger som er redigerbare`() {
        medSikretBehandlingApi { testContext ->
            /*val behandlingId = person.behandlingId
            val opplysninger =
                listOf(
                    Pair(TestOpplysningstyper.beløpA, "100"),
                    Pair(TestOpplysningstyper.dato, """"${LocalDate.of(2020, 1, 1)}""""),
                    Pair(TestOpplysningstyper.heltall, "100"),
                    Pair(TestOpplysningstyper.desimal, "100.12"),
                    Pair(TestOpplysningstyper.boolsk, "false"),
                    Pair(
                        TestOpplysningstyper.barn,
                        """

                        [
                            {
                                "fødselsdato": "${LocalDate.now().minusYears(2)}",
                                "fornavnOgMellomnavn": "Navnesen",
                                "etternavn": "Navnsen",
                                "statsborgerskap": "NOR",
                                "kvalifiserer": true
                            }
                        ]

                        """.replace("\n", "").trimIndent().escapeIfNeeded(),
                    ),
                ).map { (opplysning, verdi) ->
                    Pair(
                        verdi,
                        person
                            .behandlinger()
                            .first()
                            .opplysninger()
                            .finnOpplysning(opplysning),
                    )
                }
            opplysninger.forEach { opplysning: Pair<Any, Opplysning<*>> ->
                testContext
                    .autentisert(
                        httpMethod = HttpMethod.Put,
                        endepunkt = "/behandling/$behandlingId/opplysning/${opplysning.second.id}",
                        // language=JSON
                        body = """{"begrunnelse":"tekst", "verdi": ${opplysning.first} }""",
                    ).status shouldBe HttpStatusCode.OK

             */
        }
    }

    @Test
    fun `saksbehandler kan kvittere ut avklaring`() {
        medSikretBehandlingApi { testContext ->
            val kvitteringHendelse = slot<AvklaringKvittertHendelse>()
            /*
                        val behandlingId = person.behandlingId
                        val avklaring =

                            person
                                .behandlinger()
                                .first()
                                .aktiveAvklaringer()
                                .first()
                        val response =
                            testContext.autentisert(
                                httpMethod = HttpMethod.Put,
                                endepunkt = "/behandling/$behandlingId/avklaring/${avklaring.id}",
                                // language=JSON
                                body = """{"begrunnelse":"tekst"}""",
                            )

                        response.status shouldBe HttpStatusCode.NoContent

                        verify {
                            hendelseMediator.behandle(capture(kvitteringHendelse), any())
                        }

                        kvitteringHendelse.isCaptured shouldBe true
             */
        }
    }

    private fun medSikretBehandlingApi(block: suspend SimulertDagpengerSystem.(TestContext) -> Unit) {
        System.setProperty("Grupper.saksbehandler", "dagpenger-saksbehandler")
        System.setProperty("Grupper.beslutter", "dagpenger-beslutter")
        nyttScenario {
            inntektSiste12Mnd = 350000
        }.test {
            withMockAuthServerAndTestApplication(this.api) { block(this) }
        }
        System.clearProperty("Grupper.saksbehandler")
        System.clearProperty("Grupper.beslutter")
    }

    private companion object {
        private val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}

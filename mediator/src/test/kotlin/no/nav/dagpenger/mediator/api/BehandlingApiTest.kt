package no.nav.dagpenger.mediator.api

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.escapeIfNeeded
import no.nav.dagpenger.dato.april
import no.nav.dagpenger.mediator.api.TestApplication.maskinToken
import no.nav.dagpenger.mediator.api.TestApplication.testAzureAdToken
import no.nav.dagpenger.mediator.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.mediator.api.models.BehandlingDTO
import no.nav.dagpenger.mediator.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.mediator.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.mediator.api.models.EnhetDTO
import no.nav.dagpenger.mediator.api.models.FerietilleggKvitteringDTO
import no.nav.dagpenger.mediator.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.mediator.api.models.OpplysningerDTO
import no.nav.dagpenger.mediator.api.models.OpplysningstypeDTO
import no.nav.dagpenger.mediator.api.models.SaksbehandlersVurderingerDTO
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.scenario.SimulertDagpengerSystem
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.uuid.UUIDv7
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.Year

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
    fun `opprett ny behandling på en gitt person`() {
        medSikretBehandlingApi { testContext ->
            val response = testContext.autentisert(endepunkt = "/person/behandling", body = """{"ident":"${person.ident}"}""")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()

            person.behandlingId.shouldNotBeNull()

            person.avklaringer shouldHaveSize 1
            person.avklaringer.single().kode shouldBe "ManuellBehandling"
        }
    }

    @Test
    fun `opprett behandling med Ferietillegg regelverk`() {
        medSikretBehandlingApi { testContext ->
            val response =
                testContext.autentisert(
                    endepunkt = "/person/behandling",
                    body = """{"ident":"${person.ident}", "regelverk": "Ferietillegg"}""",
                )
            response.status shouldBe HttpStatusCode.OK

            person.behandlingId.shouldNotBeNull()
            person.avklaringer.single().kode shouldBe "ManuellBehandling"

            val behandling = objectMapper.readTree(response.bodyAsText())
            behandling["vilkår"][0]["hjemmel"]["paragraf"].asString() shouldBe "14"
        }
    }

    @Test
    fun `opprett behandling med Utestengning regelverk`() {
        medSikretBehandlingApi { testContext ->
            val response =
                testContext.autentisert(
                    endepunkt = "/person/behandling",
                    body = """{"ident":"${person.ident}", "regelverk": "Utestengning"}""",
                )
            response.status shouldBe HttpStatusCode.OK

            person.behandlingId.shouldNotBeNull()
            person.avklaringer.single().kode shouldBe "ManuellBehandling"

            val behandling = objectMapper.readTree(response.bodyAsText())
            // behandling["vilkår"][0]["hjemmel"]["kilde"]["tittel"].asString() shouldBe "Utestengning"
        }
    }

    @Test
    fun `opprett kjedet behandling på en gitt person`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.behandlingId.shouldNotBeNull()
            person.avklaringer.shouldNotBeEmpty()
            val skjeddeDato = LocalDate.now()
            val hendelseId = UUIDv7.ny()
            // language=JSON
            val body =
                """{
                  "ident": "${person.ident}",
                  "behandlingstype": "Manuell",
                  "id": "$hendelseId",
                  "skjedde": "$skjeddeDato",
                  "begrunnelse": "Automatisk opprettet av test"
            }"""
            val response = testContext.autentisert(endepunkt = "/person/behandling", body = body)
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()

            person.behandlingId.shouldNotBeNull()
            person.behandling.behandletHendelse.type shouldBe HendelseDTOTypeDTO.MANUELL
            person.behandling.behandletHendelse.id shouldBe hendelseId.toString()
            person.behandling.behandletHendelse.skjedde shouldBe skjeddeDato

            person.avklaringer.shouldNotBeEmpty()
            person.avklaringer.first().kode shouldBe "ManuellBehandling"
        }
    }

    @Test
    fun `opprett omgjøring på en gitt person`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.behandlingId.shouldNotBeNull()
            person.avklaringer.shouldNotBeEmpty()

            @Language("JSON")
            val request =
                """
                {
                  "ident": "${person.ident}",
                  "behandlingstype" : "Revurdering"
                }
                """.trimIndent()
            val response =
                testContext.autentisert(
                    endepunkt = "/person/behandling",
                    body = request,
                )
            response.status shouldBe HttpStatusCode.OK
            val bodyAsText = response.bodyAsText()
            bodyAsText.shouldNotBeEmpty()

            val behandling = objectMapper.readTree(bodyAsText)
            behandling["saksbehandlingsregler"].isEmpty shouldBe false
            behandling["saksbehandlingsregler"][0]["type"].asString() shouldBe "Prosess"
            behandling["saksbehandlingsregler"][0]["opplysninger"].isEmpty shouldBe false

            behandling["avklaringer"].isEmpty shouldBe false
            behandling["avklaringer"][0]["regelsett"][0]["type"].asString() shouldBe "Prosess"

            person.behandlingId.shouldNotBeNull()

            person.behandling.behandletHendelse.type shouldBe HendelseDTOTypeDTO.OMGJØRING

            person.avklaringer shouldHaveSize 10
            person.avklaringer.any { it.kode == "HarSvartPåOmgjøringUtenKlage" } shouldBe true
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
    fun `henter person sin rettighetsstatus`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/person/rettighetsstatus",
                    body = """{"ident":"${person.ident}"}""",
                )
            response.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `hent behandling gitt behandlingId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
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
//            with(behandlingDto.vilkår.single { it.navn == "Minsteinntekt" }) {
//                avklaringer shouldHaveSize 1
//                avklaringer.any { it.kode == "InntektNesteKalendermåned" } shouldBe true
//            }

            behandlingDto.avklaringer shouldHaveSize 9
            auditlogg.aktivitet shouldContainExactly listOf("les")
        }
    }

    @Test
    fun `hent behandling v2 gitt behandlingId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()

            val response = testContext.autentisert(httpMethod = HttpMethod.Get, endepunkt = "/behandling/v2/${person.behandlingId}")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()

            val behandlingDto = shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), BehandlingDTO::class.java) }
            behandlingDto.behandlingId shouldBe person.behandlingId
            behandlingDto.vilkår.shouldNotBeEmpty()
            behandlingDto.avklaringer.shouldNotBeEmpty()

            with(behandlingDto.behandletHendelse) {
                shouldNotBeNull()
                type shouldBe HendelseDTOTypeDTO.SØKNAD
            }
            with(behandlingDto.vilkår.single { it.navn == "Minsteinntekt" }) {
                this.opplysninger.shouldNotBeEmpty()
            }
            with(behandlingDto.fastsettelser.single { it.navn == "Dagpengegrunnlag" }) {
                this.opplysninger.shouldNotBeEmpty()
            }

            behandlingDto.opplysninger.shouldNotBeEmpty()

            with(behandlingDto.opplysninger.find { it.opplysningTypeId == TapAvArbeidsinntektOgArbeidstid.nyArbeidstid.id.uuid }) {
                this.shouldNotBeNull()
                this.perioder.first().verdi shouldBe DesimaltallVerdiDTO(verdi = 0.0, enhet = EnhetDTO.TIMER)
            }

            behandlingDto.avklaringer shouldHaveSize 9
            auditlogg.aktivitet shouldContainExactly listOf("les")
        }
    }

    @Test
    fun `hent behandlingsresultat gitt behandlingId - autentisert som saksbehandler`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()
            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Get,
                    endepunkt = "/behandling/${person.behandlingId}/behandlingsresultat",
                )
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()
            val vedtakDTO = shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), BehandlingsresultatDTO::class.java) }
            vedtakDTO.behandlingId shouldBe person.behandlingId
        }
    }

    @Test
    fun `henter ferietillegg`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(
                dato = 1.juli(2025),
                ønskerFraDato = 1.juli(2025),
            )
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    adgrupper = oppsett.adminGrupper,
                    endepunkt = "/ferietillegg/generer/2025",
                )
            response.status shouldBe HttpStatusCode.OK
            val respDTO = shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), FerietilleggKvitteringDTO::class.java) }
            respDTO.antallBestilt shouldBe 1

            rapidInspektør.key(rapidInspektør.size - 1) shouldBe person.ident
            val message = rapidInspektør.message(rapidInspektør.size - 1)
            message["@event_name"].asString() shouldBe "beregn_ferietillegg"
            message["opptjeningsår"].asInt() shouldBe 2025
            message["ident"].asString() shouldBe person.ident
        }
    }

    @Test
    fun `hent vedtak gitt behandlingId - autentisert som maskintokern`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()
            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Get,
                    endepunkt = "/behandling/${person.behandlingId}/behandlingsresultat",
                    token = maskinToken("test-app"),
                )
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()
            val vedtakDTO = shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), BehandlingsresultatDTO::class.java) }
            vedtakDTO.behandlingId shouldBe person.behandlingId
        }
    }

    @Test
    fun `hent saksbehandlers vurderinger for en gitt behandlingId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()

            saksbehandler.endreOpplysning(ReellArbeidssøker.erArbeidsfør, false)

            val response = testContext.autentisert(HttpMethod.Get, "/behandling/${person.behandlingId}/vurderinger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()

            val behandlingDto =
                shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), SaksbehandlersVurderingerDTO::class.java) }
            behandlingDto.behandlingId shouldBe person.behandlingId

            behandlingDto.opplysninger.shouldNotBeEmpty()
            behandlingDto.opplysninger
                .flatMap { it.perioder }
                .all { periode -> periode.kilde?.type?.value == "Saksbehandler" } shouldBe true
        }
    }

    @Test
    fun `lagrer saksbehandlers begrunnelse for en gitt kildeId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
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

            behandlingDto.opplysninger shouldHaveSize 1
        }
    }

    @Test
    fun `avbryt behandling gitt behandlingId`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
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
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()

            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/${person.behandlingId}/rekjor",
                    body = """{"ident":"${person.ident}"}""",
                )
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText().shouldBeEmpty()
        }
    }

    @Test
    fun `flytt behandling til ny behandling`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.søkDagpenger()
            behovsløsere.løsTilForslag()

            val hengende = person.behandlingId

            person.søkDagpenger()
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/$hengende/flytt",
                    body = """{"nyBasertPå":"${person.behandlingId}"}""",
                )
            response.status shouldBe HttpStatusCode.Accepted
            response.bodyAsText().shouldBeEmpty()
        }
    }

    @Test
    fun `test overgangene for behandling mellom saksbehandler og beslutter`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
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
                            ADGrupper = listOf(oppsett.saksbehandlerGruppe),
                            navIdent = "555",
                        ),
                ).status shouldBe HttpStatusCode.Created

            // TODO: Assert vedtak
        }
    }

    @Test
    @Disabled("Må finne ut av hvorfor denne gir 500")
    fun `kan endre alle typer opplysninger som er redigerbare`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()

            val behandlingId = person.behandlingId
            val opplysninger =
                listOf(
                    Pair(Minsteinntekt.inntekt12, "100"),
                    Pair(Alderskrav.fødselsdato, """"${LocalDate.of(2020, 1, 1)}""""),
                    Pair(TapAvArbeidsinntektOgArbeidstid.nyArbeidstid, "10.12"),
                    Pair(ReellArbeidssøker.erArbeidsfør, "false"),
                    Pair(
                        DagpengenesStørrelse.barn,
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
                ).associate { (opplysning, verdi) ->
                    verdi to person.behandling(behandlingId).opplysninger.single { it.navn == opplysning.navn }
                }
            opplysninger.forEach { (opplysning: Any, type: OpplysningerDTO) ->
                testContext
                    .autentisert(
                        httpMethod = HttpMethod.Put,
                        endepunkt = "/behandling/$behandlingId/opplysning/${type.perioder.last().id}",
                        // language=JSON
                        body = """{"begrunnelse":"tekst", "verdi": $opplysning }""",
                    ).status shouldBe HttpStatusCode.OK
            }
        }
    }

    @Test
    fun `saksbehandler kan kvittere ut avklaring`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(LocalDate.now().year))
            behovsløsere.løsTilForslag()

            val behandlingId = person.behandlingId
            val avklaring = person.avklaringer.first()
            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Put,
                    endepunkt = "/behandling/$behandlingId/avklaring/${avklaring.id}",
                    // language=JSON
                    body = """{"begrunnelse":"tekst"}""",
                )

            response.status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `opplysning kan ikke få tilOgMed før fraOgMed`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(1.april(Year.now().value))
            behovsløsere.løsTilForslag()

            val behandlingId = person.behandlingId
            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/$behandlingId/opplysning/",
                    // language=JSON
                    body = """{ "opplysningstype": "${
                        ReellArbeidssøker.kanJobbeDeltid.id.uuid
                    }","verdi":"true","begrunnelse":"tekst", "gyldigFraOgMed": "2024-01-01", "gyldigTilOgMed": "2023-01-01" }""",
                )

            response.status shouldBe HttpStatusCode.BadRequest
            val bodyAsText = response.bodyAsText()
            bodyAsText shouldContain """Til og med dato \"2023-01-01\" kan ikke være før fra og med dato \"2024-01-01\""""
        }
    }

    private fun medSikretBehandlingApi(block: suspend SimulertDagpengerSystem.(TestContext) -> Unit) {
        nyttScenario {
            inntektSiste12Mnd = 350000
            saksbehandlerGruppe = "dagpenger-saksbehandler"
            adminGrupper = listOf("enkel-admin")
            maskintilgangnavn = "test-app"
        }.test {
            withMockAuthServerAndTestApplication(this.api) { block(this) }
        }
    }

    private companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}

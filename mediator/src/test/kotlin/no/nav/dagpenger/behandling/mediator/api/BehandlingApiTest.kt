package no.nav.dagpenger.behandling.mediator.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldBeEmpty
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
import io.ktor.http.escapeIfNeeded
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.TestOpplysningstyper
import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlersVurderingerDTO
import no.nav.dagpenger.behandling.db.InMemoryPersonRepository
import no.nav.dagpenger.behandling.mediator.HendelseMediator
import no.nav.dagpenger.behandling.mediator.api.TestApplication.autentisert
import no.nav.dagpenger.behandling.mediator.api.TestApplication.testAzureAdToken
import no.nav.dagpenger.behandling.mediator.audit.Auditlogg
import no.nav.dagpenger.behandling.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Verneplikt.avtjentVerneplikt
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.first

internal class BehandlingApiTest {
    private val ident = "12345123451"
    private val rapid = spyk(TestRapid())
    private val hendelse =
        SøknadInnsendtHendelse(
            meldingsreferanseId = UUIDv7.ny(),
            ident = ident,
            søknadId = UUIDv7.ny(),
            gjelderDato = LocalDate.now(),
            fagsakId = 1,
            opprettet = LocalDateTime.now(),
        )

    private val avklaringer =
        listOf(
            Avklaring.rehydrer(
                UUIDv7.ny(),
                Avklaringkode("tittel 1", "beskrivelse ", "kanKvitteres"),
                mutableListOf(
                    Avklaring.Endring.Avbrutt(),
                ),
            ),
            Avklaring.rehydrer(
                UUIDv7.ny(),
                Avklaringkode("tittel 2", "beskrivelse ", "kanKvitteres"),
                mutableListOf(
                    Avklaring.Endring.Avklart(
                        avklartAv = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("Z123456")),
                        begrunnelse = "heia",
                    ),
                ),
            ),
            Avklaring.rehydrer(
                UUIDv7.ny(),
                Avklaringkode("tittel 3", "beskrivelse ", "kanKvitteres"),
                mutableListOf(
                    Avklaring.Endring.UnderBehandling(),
                ),
            ),
            Avklaring.rehydrer(
                UUIDv7.ny(),
                Avklaringspunkter.InntektNesteKalendermåned,
                mutableListOf(
                    Avklaring.Endring.Avklart(
                        avklartAv = Systemkilde(UUIDv7.ny(), LocalDateTime.now()),
                        begrunnelse = "heia",
                    ),
                ),
            ),
        )
    private val behandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = hendelse,
            gjeldendeOpplysninger =
                Opplysninger(
                    listOf(
                        Faktum(prøvingsdato, LocalDate.now()),
                        Faktum(
                            avtjentVerneplikt,
                            true,
                        ),
                        Faktum(
                            opplysningstype = Søknadstidspunkt.søknadsdato,
                            verdi = LocalDate.now(),
                            kilde =
                                Saksbehandlerkilde(
                                    UUIDv7.ny(),
                                    Saksbehandler("Z123456"),
                                ),
                        ),
                        Faktum(
                            opplysningstype = Minsteinntekt.inntekt12,
                            verdi = Beløp(3000.034.toBigDecimal()),
                        ),
                        Faktum(
                            opplysningstype = TestOpplysningstyper.heltall,
                            verdi = 3,
                        ),
                        Faktum(
                            opplysningstype = TestOpplysningstyper.desimal,
                            verdi = 3.0,
                        ),
                        Faktum(
                            opplysningstype = TestOpplysningstyper.boolsk,
                            verdi = true,
                        ),
                        Faktum(
                            opplysningstype = TestOpplysningstyper.dato,
                            verdi = LocalDate.now(),
                        ),
                        Faktum(
                            opplysningstype = TestOpplysningstyper.beløpA,
                            verdi = Beløp(1000.toBigDecimal()),
                        ),
                        Faktum(
                            opplysningstype = TestOpplysningstyper.periode,
                            verdi = Periode(LocalDate.now(), LocalDate.now().plusDays(10)),
                        ),
                        Faktum(
                            opplysningstype = TestOpplysningstyper.barn,
                            verdi =
                                BarnListe(
                                    listOf(
                                        Barn(
                                            LocalDate.now(),
                                            kvalifiserer = true,
                                        ),
                                    ),
                                ),
                            kilde = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("Z123456")),
                        ),
                    ),
                ),
            basertPå = emptyList(),
            tilstand = Behandling.TilstandType.TilGodkjenning,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = avklaringer,
        )

    private val person = Person(ident.tilPersonIdentfikator(), listOf(behandling))

    private val personRepository =
        InMemoryPersonRepository().also {
            it.lagre(person)
        }
    private val hendelseMediator = mockk<HendelseMediator>(relaxed = true)
    private val auditlogg = mockk<Auditlogg>(relaxed = true)
    private val apiRepositoryPostgres = mockk<ApiRepositoryPostgres>(relaxed = true)

    @AfterEach
    fun tearDown() {
        personRepository.reset()
    }

    @Test
    fun `ikke autentiserte kall returnerer 401`() {
        medSikretBehandlingApi {
            val response =
                client.post("/behandling") {
                    setBody("""{"ident":"$ident"}""")
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `kall uten saksbehandlingsADgruppe i claims returnerer 401`() {
        medSikretBehandlingApi {
            autentisert(
                token = testAzureAdToken(ADGrupper = emptyList()),
                endepunkt = "/behandling",
                body = """{"ident":"$ident"}""",
            ).status shouldBe HttpStatusCode.Unauthorized

            autentisert(
                token = testAzureAdToken(ADGrupper = listOf("ikke-saksbehandler")),
                endepunkt = "/behandling",
                body = """{"ident":"$ident"}""",
            ).status shouldBe HttpStatusCode.Unauthorized

            autentisert(
                token = testAzureAdToken(ADGrupper = listOf("dagpenger-saksbehandler")),
                endepunkt = "/behandling",
                body = """{"ident":"$ident"}""",
            ).status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `hent behandlinger gitt person`() {
        medSikretBehandlingApi {
            val response = autentisert("/behandling", body = """{"ident":"$ident"}""")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()
            verify {
                auditlogg.les(any(), any(), any())
            }
        }
    }

    @Test
    fun `gir 404 hvis person ikke eksisterer`() {
        medSikretBehandlingApi {
            val response = autentisert("/behandling", body = """{"ident":"09876543311"}""")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `hent behandling gitt behandlingId`() {
        medSikretBehandlingApi {
            val behandlingId = person.behandlinger().first().behandlingId
            val response = autentisert(httpMethod = HttpMethod.Get, endepunkt = "/behandling/$behandlingId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()
            val behandlingDto = shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), BehandlingDTO::class.java) }
            behandlingDto.behandlingId shouldBe behandlingId
            behandlingDto.vilkår.shouldNotBeEmpty()
            behandlingDto.opplysninger.all { it.redigerbar } shouldBe false
            behandlingDto.avklaringer.shouldNotBeEmpty()

            with(behandlingDto.behandletHendelse) {
                shouldNotBeNull()
                type shouldBe HendelseDTOTypeDTO.SØKNAD
                id shouldBe hendelse.eksternId.id.toString()
            }
            with(behandlingDto.vilkår.single { it.navn == "Minsteinntekt" }) {
                avklaringer shouldHaveSize 1
                avklaringer.single().kode shouldBe "InntektNesteKalendermåned"
            }

            /*
             * TODO: Testen bør bruke mer mocka data og ikke være så koblet til oppførsel i modellen
            with(behandlingDto.vilkår.single { it.navn == "Verneplikt" }) {
                avklaringer shouldHaveSize 1
                val aktivAvklaring = behandling.aktiveAvklaringer().first()
                with(avklaringer.single()) {
                    kode shouldBe "Verneplikt"

                    tittel shouldBe aktivAvklaring.kode.tittel
                    beskrivelse shouldBe aktivAvklaring.kode.beskrivelse
                    id shouldBe aktivAvklaring.id
                }
            }*/

            behandlingDto.avklaringer shouldHaveSize 3

            verify {
                auditlogg.les(any(), any(), any())
            }
        }
    }

    @Test
    fun `hent saksbehandlers vurderinger for en gitt behandlingId`() {
        medSikretBehandlingApi {
            val behandlingId = person.behandlinger().first().behandlingId
            val response = autentisert(httpMethod = HttpMethod.Get, endepunkt = "/behandling/$behandlingId/vurderinger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()
            val behandlingDto =
                shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), SaksbehandlersVurderingerDTO::class.java) }
            behandlingDto.behandlingId shouldBe behandlingId

            behandlingDto.regelsett.shouldNotBeEmpty()
            behandlingDto.avklaringer.shouldNotBeEmpty()
            behandlingDto.opplysninger.shouldNotBeEmpty()

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
        medSikretBehandlingApi {
            val behandling = person.behandlinger().first()
            val behandlingId = behandling.behandlingId

            val opplysning = behandling.opplysninger().finnAlle().single { it.opplysningstype.navn == "Søknadsdato" }
            autentisert(
                httpMethod = HttpMethod.Put,
                endepunkt = "/behandling/$behandlingId/vurderinger/${opplysning.kilde?.id}",
                body = """{ "begrunnelse":"tekst" }""",
            )

            val response = autentisert(httpMethod = HttpMethod.Get, endepunkt = "/behandling/$behandlingId/vurderinger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText().shouldNotBeEmpty()
            val behandlingDto =
                shouldNotThrowAny { objectMapper.readValue(response.bodyAsText(), SaksbehandlersVurderingerDTO::class.java) }
            behandlingDto.behandlingId shouldBe behandlingId

            behandlingDto.regelsett.shouldNotBeEmpty()
            behandlingDto.avklaringer.shouldNotBeEmpty()
            behandlingDto.opplysninger.shouldNotBeEmpty()

            with(behandlingDto.regelsett.single { it.navn == "Søknadstidspunkt" }) {
                avklaringer.shouldBeEmpty()
                opplysningIder.shouldHaveSize(1)
            }

            with(behandlingDto.avklaringer.single { it.kode == "tittel 2" }) {
                avklartAv.shouldBeInstanceOf<SaksbehandlerDTO>()
            }

            behandlingDto.opplysninger shouldHaveSize 2
            behandlingDto.opplysninger.all { it.kilde?.type?.value == "Saksbehandler" } shouldBe true
        }
    }

    @Test
    fun `avbryt behandling gitt behandlingId`() {
        medSikretBehandlingApi {
            val behandlingId = person.behandlinger().first().behandlingId
            val response =
                autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/$behandlingId/avbryt",
                    body = """{"ident":"09876543311"}""",
                )
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText().shouldBeEmpty()
            verify {
                hendelseMediator.behandle(any<AvbrytBehandlingHendelse>(), any())
            }
        }
    }

    @Test
    fun `rekjør behandling med gitt behandlingId`() {
        medSikretBehandlingApi {
            val behandlingId = person.behandlinger().first().behandlingId
            val response =
                autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/$behandlingId/rekjor",
                    body = """{"ident":"09876543311"}""",
                )
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText().shouldBeEmpty()
            verify {
                hendelseMediator.behandle(any<RekjørBehandlingHendelse>(), any())
            }
        }
    }

    @Test
    fun `test overgangene for behandling mellom saksbehandler og beslutter`() {
        medSikretBehandlingApi {
            val behandlingId = person.behandlinger().first().behandlingId
            val response =
                autentisert(
                    httpMethod = HttpMethod.Post,
                    endepunkt = "/behandling/$behandlingId/godkjenn",
                    body = """{"ident":"09876543311"}""",
                )
            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText().shouldBeEmpty()
            verify {
                hendelseMediator.behandle(any<GodkjennBehandlingHendelse>(), any())
            }

            // Send tilbake til saksbehandler
            autentisert(
                httpMethod = HttpMethod.Post,
                endepunkt = "/behandling/$behandlingId/send-tilbake",
                body = """{"ident":"09876543311"}""",
            ).status shouldBe HttpStatusCode.Created
            verify {
                hendelseMediator.behandle(any<SendTilbakeHendelse>(), any())
            }

            // Godkjenn igjen
            autentisert(
                httpMethod = HttpMethod.Post,
                endepunkt = "/behandling/$behandlingId/godkjenn",
                body = """{"ident":"09876543311"}""",
            ).status shouldBe HttpStatusCode.Created
            verify {
                hendelseMediator.behandle(any<GodkjennBehandlingHendelse>(), any())
            }

            // Beslutt
            autentisert(
                httpMethod = HttpMethod.Post,
                endepunkt = "/behandling/$behandlingId/beslutt",
                body = """{"ident":"09876543311"}""",
            ).status shouldBe HttpStatusCode.Created
            verify {
                hendelseMediator.behandle(any<BesluttBehandlingHendelse>(), any())
            }
        }
    }

    @Test
    fun `kan endre alle typer opplysninger som er redigerbare`() {
        medSikretBehandlingApi {
            val behandlingId = person.behandlinger().first().behandlingId
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
                autentisert(
                    httpMethod = HttpMethod.Put,
                    endepunkt = "/behandling/$behandlingId/opplysning/${opplysning.second.id}",
                    // language=JSON
                    body = """{"begrunnelse":"tekst", "verdi": ${opplysning.first} }""",
                ).status shouldBe HttpStatusCode.OK
            }
        }
    }

    @Test
    fun `saksbehandler kan kvittere ut avklaring`() {
        medSikretBehandlingApi {
            val kvitteringHendelse = slot<AvklaringKvittertHendelse>()

            val behandlingId = person.behandlinger().first().behandlingId
            val avklaring =
                person
                    .behandlinger()
                    .first()
                    .aktiveAvklaringer()
                    .first()
            val response =
                autentisert(
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
        }
    }

    private fun medSikretBehandlingApi(
        personRepository: PersonRepository = this.personRepository,
        hendelseMediator: HendelseMediator = this.hendelseMediator,
        apiRepositoryPostgres: ApiRepositoryPostgres = this.apiRepositoryPostgres,
        test: suspend TestContext.() -> Unit,
    ) {
        System.setProperty("Grupper.saksbehandler", "dagpenger-saksbehandler")
        TestApplication.withMockAuthServerAndTestApplication(
            moduleFunction = {
                behandlingApi(personRepository, hendelseMediator, auditlogg, emptySet(), apiRepositoryPostgres) { rapid }
            },
            test,
        )
        System.clearProperty("Grupper.saksbehandler")
    }

    private companion object {
        private val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}

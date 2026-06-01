package no.nav.dagpenger.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import com.natpryce.konfig.ConfigurationMap
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.server.application.Application
import no.nav.dagpenger.ferietillegg.FerietilleggRegistrering
import no.nav.dagpenger.konfigurasjon.Configuration
import no.nav.dagpenger.mediator.BehandlingRuntime
import no.nav.dagpenger.mediator.IAktivitetsloggMediator
import no.nav.dagpenger.mediator.api.TestApplication
import no.nav.dagpenger.mediator.api.TestApplication.AZUREAD_ISSUER_ID
import no.nav.dagpenger.mediator.api.TestApplication.testAzureAdToken
import no.nav.dagpenger.mediator.api.TestContext
import no.nav.dagpenger.mediator.api.auth.AuthFactory
import no.nav.dagpenger.mediator.api.auth.AuthFactory.azure_app
import no.nav.dagpenger.mediator.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.mediator.audit.Auditlogg
import no.nav.dagpenger.mediator.db.DBTestContext
import no.nav.dagpenger.mediator.db.withMigratedDb
import no.nav.dagpenger.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.modell.Person
import no.nav.dagpenger.regel.DagpengerRegistrering
import no.nav.dagpenger.regelverk.RegelverkRegistrering
import no.nav.dagpenger.scenario.assertions.BehandlingsresultatAssertions
import org.approvaltests.Approvals
import tools.jackson.databind.JsonNode
import java.util.UUID
import kotlin.random.Random

internal class SimulertDagpengerSystem(
    dbTestContext: DBTestContext,
    val oppsett: ScenarioOptions,
) {
    companion object {
        fun nyttScenario(block: ScenarioOptions.() -> Unit = {}) = ScenarioOptions().apply(block)
    }

    private val rapid = TestRapid()
    private val regelverk: List<RegelverkRegistrering> = listOf(DagpengerRegistrering(), FerietilleggRegistrering())

    val auditlogg = TestAuditlogg()
    private val authFactory =
        AuthFactory(
            ConfigurationMap(
                mapOf(
                    Configuration.Grupper.saksbehandler.name to oppsett.saksbehandlerGruppe,
                    Configuration.Maskintilgang.navn.name to oppsett.maskintilgangnavn,
                    Configuration.Grupper.admin.name to oppsett.adminGrupper.joinToString(","),
                    azure_app.client_id.name to TestApplication.CLIENT_ID,
                    azure_app.well_known_url.name to "${TestApplication.mockOAuth2Server.wellKnownUrl(AZUREAD_ISSUER_ID)}",
                ),
            ),
        )
    private val runtime =
        BehandlingRuntime(
            authFactory = authFactory,
            dbSession = dbTestContext.dbSession,
            rapidsConnection = rapid,
            auditlogg = auditlogg,
            regelverk = regelverk,
            aktivitetsloggMediator =
                object : IAktivitetsloggMediator {
                    override fun håndter(
                        context: MessageContext,
                        hendelse: no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse,
                    ) {
                    }
                },
        ) { rapid }

    init {
        runtime.registrerMottak()
        runtime.lagreOpplysningstyper()
    }

    val api: Application.() -> Unit = runtime.api

    val person = Mennesket(rapid, oppsett)
    val behovsløsere = Behovsløsere(rapid, person)
    val saksbehandler = TestSaksbehandler2(person, runtime.hendelseMediator, runtime.personRepository, rapid)

    val rapidInspektør get() = rapid.inspektør

    inline fun behandlingsresultatForslag(
        nummer: Int? = null,
        block: BehandlingsresultatAssertions.() -> Unit,
    ) {
        val forslag = behovsløsere.sisteBehandlingsresultatForslag()
        if (nummer != null) {
            require(nummer == forslag.first) {
                "Fant ikke forventet forslag til behandlingsresultat, forventet: $nummer, fikk: ${forslag.first}"
            }
        }
        BehandlingsresultatAssertions(forslag.second).block()
    }

    inline fun behandlingsresultat(
        nummer: Int? = null,
        block: BehandlingsresultatAssertions.() -> Unit,
    ) {
        val behandlingsresultat = behovsløsere.sisteBehandlingsresultat()
        if (nummer != null) {
            require(nummer == behandlingsresultat.first) {
                "Fant ikke forventet behandlingsresultat, forventet: $nummer, fikk: ${behandlingsresultat.first}"
            }
        }
        BehandlingsresultatAssertions(behandlingsresultat.second).block()
    }

    fun BehandlingsresultatDTO.harOpplysning(opplysningId: UUID): Boolean {
        val behandling = runtime.personRepository.hentBehandling(person.behandlingId)
        return runCatching { behandling!!.opplysninger.finnOpplysning(opplysningId) }.isSuccess
    }

    class ScenarioOptions(
        var ident: String = Random.nextLong(10000000000, 19999999999).toString(),
        var alder: Int = 33,
        var inntektSiste12Mnd: Int = 50000,
        var permittering: Boolean = false,
        var permittertfraFiskeforedling: Boolean = false,
        val ordinær: Boolean = false,
        var verneplikt: Boolean = false,
        var kanJobbeDeltid: Boolean = true,
        var saksbehandlerGruppe: String = "dagpenger-saksbehandler",
        var adminGrupper: List<String> = listOf("enkel-admin"),
        var maskintilgangnavn: String = "test-app",
    ) {
        inline fun test(crossinline block: SimulertDagpengerSystem.() -> Unit) {
            withMigratedDb {
                val test = SimulertDagpengerSystem(this, this@ScenarioOptions)
                test.opprettPerson(ident)
                test.block()

                godkjennMeldinger(test.rapid.inspektør)
            }
        }
    }

    private fun opprettPerson(ident: String) {
        runtime.personRepository.lagre(Person(ident.tilPersonIdentfikator()))
    }

    class TestRapidMessageContext(
        private val rapid: TestRapid,
    ) : MessageContext {
        override fun publish(message: String) {
            rapid.sendTestMessage(message)
        }

        override fun publish(
            key: String,
            message: String,
        ) {
            rapid.sendTestMessage(message, key)
        }

        override fun publish(messages: List<OutgoingMessage>): Pair<List<SentMessage>, List<FailedMessage>> {
            TODO("Not yet implemented")
        }

        override fun rapidName(): String {
            TODO("Not yet implemented")
        }
    }

    fun sendFerietillegg(
        ident: String,
        ferietilleggId: UUID,
        opptjeningsår: Int,
    ) {
        rapid
            .sendTestMessage(
                JsonMessage
                    .newMessage(
                        "beregn_ferietillegg",
                        mapOf(
                            "ferietilleggId" to ferietilleggId,
                            "ident" to ident,
                            "opptjeningsår" to opptjeningsår,
                        ),
                    ).toJson(),
                ident,
            )
    }

    fun løsBehovForAntallForbruksdager(antallDager: Int) {
        person.antallDagerForbrukt = antallDager
    }

    val meldekortkø = runtime.meldekortBehandlingskø(TestRapidMessageContext(rapid))

    fun meldekortBatch(avklar: Boolean = false) {
        val påbegynteMeldekort = meldekortkø.sendMeldekortTilBehandling()

        if (avklar) {
            påbegynteMeldekort.forEach { eksternMeldekortId ->
                saksbehandler.lukkAlleAvklaringer()
                saksbehandler.godkjenn()

                // Marker som ferdig (vi klarer ikke å fange det i VedtakFattetMottak)
                runtime.meldekortRepository.markerSomFerdig(eksternMeldekortId)
            }
        }
    }

    internal suspend fun TestContext.autentisert(
        httpMethod: HttpMethod = HttpMethod.Post,
        endepunkt: String,
        body: String? = null,
        adgrupper: List<String> = listOf(oppsett.saksbehandlerGruppe),
        token: String =
            testAzureAdToken(
                ADGrupper = adgrupper,
                navIdent = "123",
            ),
    ): HttpResponse =
        client.request(endepunkt) {
            this.method = httpMethod
            body?.let { this.setBody(TextContent(it, ContentType.Application.Json)) }
            this.header(HttpHeaders.Authorization, "Bearer $token")
            this.header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }
}

private fun godkjennMeldinger(inspektør: TestRapid.RapidInspector) {
    val meldinger = mutableListOf<String>()
    for (offset in 0..<inspektør.size) {
        val melding = inspektør.message(offset)
        when (melding["@event_name"].asString()) {
            "behov" -> {
                meldinger.add("Behov:${melding["@behov"].values().map { it.asString() }.sorted().joinToString("\n- ", "\n- ")}")
            }

            "NyAvklaring" -> {
                meldinger.add("Laget avklaring om ${melding["kode"].asString()}")
            }

            "behandling_opprettet" -> {
                meldinger.add("Opprettet ny behandling")
            }

            "behandling_endret_tilstand" -> {
                meldinger.add("Behandling endret tilstand til: ${melding["gjeldendeTilstand"].asString()}")
            }

            "forslag_til_vedtak" -> {
                meldinger.add("Forslag til vedtak med utfall=${melding["fastsatt"]["utfall"].asBoolean()}")
            }

            "vedtak_fattet" -> {
                meldinger.add("Har fattet vedtak med utfall=${melding["fastsatt"]["utfall"].asBoolean()}")
            }

            "forslag_til_behandlingsresultat" -> {}

            "behandlingsresultat" -> {}

            else -> {
                meldinger.add("melding: ${melding["@event_name"].asString()}")
            }
        }
    }
    Approvals.verify(meldinger.joinToString("\n"))
}

fun TestRapid.RapidInspector.sisteMelding(navn: String): Pair<Int, JsonNode> {
    val treff = mutableListOf<JsonNode>()
    for (offset in 0 until size) {
        val message = message(offset)
        if (message["@event_name"].asString() == navn) {
            treff.add(message)
        }
    }
    if (treff.isNotEmpty()) {
        return (treff.lastIndex + 1) to treff.last()
    }
    throw NoSuchElementException("Fant ingen melding av type=$navn")
}

class TestAuditlogg internal constructor() : Auditlogg {
    val aktivitet = mutableListOf<String>()

    override fun les(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) {
        aktivitet.add("les")
    }

    override fun opprett(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) {
        aktivitet.add("opprett")
    }

    override fun oppdater(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) {
        aktivitet.add("oppdater")
    }

    override fun slett(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) {
        aktivitet.add("slett")
    }
}

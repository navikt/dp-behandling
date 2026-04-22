package no.nav.dagpenger.behandling.helpers.scenario

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import io.ktor.server.application.Application
import io.mockk.mockk
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.db.Postgres
import no.nav.dagpenger.behandling.helpers.scenario.assertions.BehandlingsresultatAssertions
import no.nav.dagpenger.behandling.mediator.BehovMediator
import no.nav.dagpenger.behandling.mediator.HendelseMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.api.behandlingApi
import no.nav.dagpenger.behandling.mediator.audit.Auditlogg
import no.nav.dagpenger.behandling.mediator.meldekort.MeldekortBehandlingskø
import no.nav.dagpenger.behandling.mediator.melding.PostgresMeldingRepository
import no.nav.dagpenger.behandling.mediator.mottak.MarkerMeldekortSomBehandletMottak
import no.nav.dagpenger.behandling.mediator.registrerRegelverk
import no.nav.dagpenger.behandling.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.VentendeMeldekortDings
import no.nav.dagpenger.behandling.mediator.standardRegelverk
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.RegelverkFerietillegg
import org.approvaltests.Approvals
import java.util.UUID
import kotlin.random.Random

internal class SimulertDagpengerSystem(
    oppsett: ScenarioOptions,
) {
    companion object {
        fun nyttScenario(block: ScenarioOptions.() -> Unit = {}) = ScenarioOptions().apply(block)
    }

    private val rapid = TestRapid()
    private val opplysningerRepository = OpplysningerRepositoryPostgres()
    private val registrertForretningsprosess = standardRegelverk()
    private val personRepository =
        PersonRepositoryPostgres(
            BehandlingRepositoryPostgres(
                opplysningerRepository,
                AvklaringRepositoryPostgres(AvklaringKafkaObservatør(rapid)),
                registrertForretningsprosess,
            ),
        )
    private val meldekortRepository = MeldekortRepositoryPostgres()
    private val ventendeMeldekort = VentendeMeldekortDings(meldekortRepository)
    private val hendelseMediator =
        HendelseMediator(
            personRepository = personRepository,
            meldekortRepository = meldekortRepository,
            behovMediator = BehovMediator(),
            aktivitetsloggMediator = mockk(relaxed = true),
            listOf(ventendeMeldekort),
        )

    private val postgresMeldingRepository = PostgresMeldingRepository()

    private val apiRepositoryPostgres = ApiRepositoryPostgres(postgresMeldingRepository)
    val auditlogg = TestAuditlogg()

    private val opplysningstyper: Set<Opplysningstype<*>> = RegelverkDagpenger.produserer + RegelverkFerietillegg.produserer

    init {
        MarkerMeldekortSomBehandletMottak(rapid, meldekortRepository)
        MessageMediator(
            rapidsConnection = rapid,
            hendelseMediator = hendelseMediator,
            meldingRepository = postgresMeldingRepository,
            opplysningstyper = opplysningstyper,
            meldekortRepository = meldekortRepository,
            apiRepositoryPostgres = apiRepositoryPostgres,
        )
        registrerRegelverk(opplysningerRepository, Opplysningstype.definerteTyper)
    }

    val api: Application.() -> Unit = {
        behandlingApi(
            personRepository,
            hendelseMediator,
            auditlogg,
            opplysningstyper,
            apiRepositoryPostgres,
            meldekortRepository,
        ) { rapid }
    }

    val person = Mennesket(rapid, oppsett)
    val behovsløsere = Behovsløsere(rapid, person)
    val saksbehandler = TestSaksbehandler2(person, hendelseMediator, personRepository, rapid)

    val rapidInspektør get() = rapid.inspektør

    fun behandlingsresultatForslag(
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

    fun behandlingsresultat(
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
        val behandling = personRepository.hentBehandling(person.behandlingId)
        return runCatching { behandling!!.opplysninger.finnOpplysning(opplysningId) }.isSuccess
    }

    class ScenarioOptions(
        var ident: String = Random.Default.nextLong(10000000000, 19999999999).toString(),
        var alder: Int = 33,
        var inntektSiste12Mnd: Int = 50000,
        var permittering: Boolean = false,
        var permittertfraFiskeforedling: Boolean = false,
        val ordinær: Boolean = false,
        var verneplikt: Boolean = false,
    ) {
        fun test(block: SimulertDagpengerSystem.() -> Unit) {
            Postgres.withMigratedDb {
                val test = SimulertDagpengerSystem(this)
                test.opprettPerson(ident)
                test.block()

                godkjennMeldinger(test.rapid.inspektør)
            }
        }
    }

    private fun opprettPerson(ident: String) {
        personRepository.lagre(Person(ident.tilPersonIdentfikator()))
    }

    class TestLol(
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

    val meldekortkø = MeldekortBehandlingskø(personRepository, meldekortRepository, TestLol(rapid))

    fun meldekortBatch(avklar: Boolean = false) {
        val påbegynteMeldekort = meldekortkø.sendMeldekortTilBehandling()

        if (avklar) {
            påbegynteMeldekort.forEach { eksternMeldekortId ->
                saksbehandler.lukkAlleAvklaringer()
                saksbehandler.godkjenn()

                // Marker som ferdig (vi klarer ikke å fange det i VedtakFattetMottak)
                meldekortRepository.markerSomFerdig(eksternMeldekortId)
            }
        }
    }
}

private fun godkjennMeldinger(inspektør: TestRapid.RapidInspector) {
    val meldinger = mutableListOf<String>()
    for (offset in 0..<inspektør.size) {
        val melding = inspektør.message(offset)
        when (melding["@event_name"].asText()) {
            "behov" -> {
                meldinger.add("Behov:${melding["@behov"].joinToString("\n- ", "\n- ") { it.asText() }}")
            }

            "NyAvklaring" -> {
                meldinger.add("Laget avklaring om ${melding["kode"].asText()}")
            }

            "behandling_opprettet" -> {
                meldinger.add("Opprettet ny behandling")
            }

            "behandling_endret_tilstand" -> {
                meldinger.add("Behandling endret tilstand til: ${melding["gjeldendeTilstand"].asText()}")
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
                meldinger.add("melding: ${melding["@event_name"].asText()}")
            }
        }
    }
    Approvals.verify(meldinger.joinToString("\n"))
}

fun TestRapid.RapidInspector.sisteMelding(navn: String): Pair<Int, JsonNode> {
    val treff = mutableListOf<JsonNode>()
    for (offset in 0 until size) {
        val message = message(offset)
        if (message["@event_name"].asText() == navn) {
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

package no.nav.dagpenger.behandling.scenario

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.ktor.server.application.Application
import io.mockk.mockk
import no.nav.dagpenger.behandling.api.models.VedtakDTO
import no.nav.dagpenger.behandling.db.Postgres
import no.nav.dagpenger.behandling.mediator.BehovMediator
import no.nav.dagpenger.behandling.mediator.HendelseMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.api.behandlingApi
import no.nav.dagpenger.behandling.mediator.audit.Auditlogg
import no.nav.dagpenger.behandling.mediator.melding.PostgresMeldingRepository
import no.nav.dagpenger.behandling.mediator.registrerRegelverk
import no.nav.dagpenger.behandling.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.scenario.assertions.ForslagAssertions
import no.nav.dagpenger.behandling.scenario.assertions.KlumpenAssertions
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.RegelverkDagpenger
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
    private val personRepository =
        PersonRepositoryPostgres(
            BehandlingRepositoryPostgres(
                opplysningerRepository,
                AvklaringRepositoryPostgres(AvklaringKafkaObservatør(rapid)),
            ),
        )
    private val meldekortRepository = MeldekortRepositoryPostgres()
    private val hendelseMediator =
        HendelseMediator(
            personRepository = personRepository,
            meldekortRepository = meldekortRepository,
            behovMediator = BehovMediator(),
            aktivitetsloggMediator = mockk(relaxed = true),
        )

    private val postgresMeldingRepository = PostgresMeldingRepository()

    private val apiRepositoryPostgres = ApiRepositoryPostgres(postgresMeldingRepository)
    val auditlogg = TestAuditlogg()

    init {
        MessageMediator(
            rapidsConnection = rapid,
            hendelseMediator = hendelseMediator,
            meldingRepository = postgresMeldingRepository,
            opplysningstyper = RegelverkDagpenger.produserer,
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
            RegelverkDagpenger.produserer,
            apiRepositoryPostgres,
        ) { rapid }
    }

    val person = Mennesket(rapid, oppsett)
    val behovsløsere = Behovsløsere(rapid, person)
    val saksbehandler = TestSaksbehandler2(person, hendelseMediator, personRepository, rapid)

    val rapidInspektør get() = rapid.inspektør

    fun forslag(block: ForslagAssertions.() -> Unit) {
        ForslagAssertions(behovsløsere.sisteForslag()).block()
    }

    fun vedtak(block: ForslagAssertions.() -> Unit) {
        ForslagAssertions(behovsløsere.sisteVedtak()).block()
    }

    fun klumpen(block: KlumpenAssertions.() -> Unit) {
        KlumpenAssertions(behovsløsere.sisteKlumpen()).block()
    }

    fun VedtakDTO.harOpplysning(opplysningId: UUID): Boolean {
        val behandling = personRepository.hentBehandling(person.behandlingId)
        return runCatching { behandling!!.opplysninger.finnOpplysning(opplysningId) }.isSuccess
    }

    class ScenarioOptions(
        var ident: String = Random.Default.nextLong(10000000000, 19999999999).toString(),
        var alder: Int = 33,
        var inntektSiste12Mnd: Int = 50000,
        var permittering: Boolean = false,
        val ordinær: Boolean = !permittering,
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
}

private fun godkjennMeldinger(inspektør: TestRapid.RapidInspector) {
    val meldinger = mutableListOf<String>()
    for (offset in 0..<inspektør.size) {
        val melding = inspektør.message(offset)
        when (melding["@event_name"].asText()) {
            "behov" -> meldinger.add("Behov:${melding["@behov"].joinToString("\n- ", "\n- ") { it.asText() }}")
            "NyAvklaring" -> meldinger.add("Laget avklaring om ${melding["kode"].asText()}")
            "behandling_opprettet" -> meldinger.add("Opprettet ny behandling")
            "behandling_endret_tilstand" -> meldinger.add("Behandling endret tilstand til: ${melding["gjeldendeTilstand"].asText()}")
            "forslag_til_vedtak" -> meldinger.add("Forslag til vedtak med utfall=${melding["fastsatt"]["utfall"].asBoolean()}")
            "vedtak_fattet" -> meldinger.add("Har fattet vedtak med utfall=${melding["fastsatt"]["utfall"].asBoolean()}")
            "klumpen_er_laget" -> {}

            else -> meldinger.add("melding: ${melding["@event_name"].asText()}")
        }
    }
    Approvals.verify(meldinger.joinToString("\n"))
}

fun TestRapid.RapidInspector.sisteMelding(navn: String): JsonNode {
    for (offset in size - 1 downTo 0) {
        val message = message(offset)
        if (message["@event_name"].asText() == navn) {
            return message
        }
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

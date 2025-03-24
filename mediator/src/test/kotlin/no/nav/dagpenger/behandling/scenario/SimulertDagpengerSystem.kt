package no.nav.dagpenger.behandling.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import no.nav.dagpenger.behandling.db.Postgres
import no.nav.dagpenger.behandling.mediator.BehovMediator
import no.nav.dagpenger.behandling.mediator.HendelseMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.melding.PostgresHendelseRepository
import no.nav.dagpenger.behandling.mediator.registrerRegelverk
import no.nav.dagpenger.behandling.mediator.repository.AvklaringKafkaObservatør
import no.nav.dagpenger.behandling.mediator.repository.AvklaringRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.behandling.scenario.assertions.ForslagAssertions
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.RegelverkDagpenger

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

    init {
        MessageMediator(
            rapidsConnection = rapid,
            hendelseMediator = hendelseMediator,
            hendelseRepository = PostgresHendelseRepository(),
            opplysningstyper = RegelverkDagpenger.produserer,
            meldekortRepository = meldekortRepository,
        )
        registrerRegelverk(opplysningerRepository, Opplysningstype.definerteTyper)
    }

    val person = Mennesket(rapid, oppsett)
    val behovsløsere = Behovsløsere(rapid, person)
    val saksbehandler = TestSaksbehandler2(person, hendelseMediator, personRepository, rapid)

    fun forslag(block: ForslagAssertions.() -> Unit) {
        ForslagAssertions(behovsløsere.sisteForslag()).block()
    }

    fun vedtak(block: ForslagAssertions.() -> Unit) {
        ForslagAssertions(behovsløsere.sisteVedtak()).block()
    }

    class ScenarioOptions(
        var ident: String = "12312312311",
        var alder: Int = 33,
        var inntektSiste12Mnd: Int = 50000,
        var permittering: Boolean = false,
        val ordinær: Boolean = !permittering,
    ) {
        fun test(block: SimulertDagpengerSystem.() -> Unit) {
            Postgres.withMigratedDb {
                val test = SimulertDagpengerSystem(this)
                test.block()
            }
        }
    }
}

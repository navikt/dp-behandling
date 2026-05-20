package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.avklaring.Avklaringer
import no.nav.dagpenger.behandling.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.somKjede
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AvklaringRepositoryPostgresTest {
    @Test
    fun `lagrer og rehydrer avklaringer`() {
        avklaringTest {
            val kode1 = Avklaringkode("JobbetUtenforNorge", "Arbeid utenfor Norge", "Personen har oppgitt arbeid utenfor Norge")
            val kode2 = Avklaringkode("HarVunnetNobelprisen", "Har vunnet nobelprisen", "Personen har vunnet en nobelpris ")

            val behandling = TestBehandling(behandlingRepository, avklaring(kode1), avklaring(kode2))
            val avklaringer = repository.hentAvklaringer(behandling.behandlingId)

            avklaringer.size shouldBe 2
        }
    }

    @Test
    fun `lagrer kilde og begrunnelse på avklarte avklaringer`() {
        avklaringTest {
            val kode1 = Avklaringkode("JobbetUtenforNorge", "Arbeid utenfor Norge", "Personen har oppgitt arbeid utenfor Norge")

            val behandling = TestBehandling(behandlingRepository, avklaring(kode1))
            behandling.avklar("123", "begrunnelse")

            val avklaringer = repository.hentAvklaringer(behandling.behandlingId)

            avklaringer.shouldHaveSize(1)
            with(avklaringer.first().endringer.last()) {
                shouldBeInstanceOf<Avklaring.Endring.Avklart>()

                this.begrunnelse shouldBe "begrunnelse"
            }
        }
    }

    @Test
    fun `tar vare på rekkefølge av endringer`() {
        avklaringTest {
            val kode1 = Avklaringkode("JobbetUtenforNorge", "Arbeid utenfor Norge", "Personen har oppgitt arbeid utenfor Norge")

            val avklaring = avklaring(kode1)
            val avklaringer = Avklaringer(emptyList(), listOf(avklaring))
            avklaringer.kvitter(avklaring.id, Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("123")), "begrunnelse")
            avklaringer.gjenåpne(avklaring.id)
            avklaringer.avklar(avklaring.id, Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("123")))

            val forventedeTilstander = listOf("UnderBehandling", "Avklart", "UnderBehandling", "Avklart")
            avklaring.endringer.map { it::class.simpleName!! } shouldBe forventedeTilstander

            val behandling = TestBehandling(behandlingRepository, avklaring)
            val avklaringerFraDb = repository.hentAvklaringer(behandling.behandlingId)

            repository.hentAvklaringer(TestBehandling(behandlingRepository, avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(behandlingRepository, avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(behandlingRepository, avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(behandlingRepository, avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(behandlingRepository, avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(behandlingRepository, avklaring).behandlingId)

            avklaringerFraDb.single().endringer.map { it::class.simpleName!! } shouldBe forventedeTilstander
        }
    }

    private class TestBehandling(
        behandlingRepository: BehandlingRepository,
        vararg avklaring: Avklaring,
    ) {
        val behandlingId get() = behandling.behandlingId
        private val behandling = TestBehandlinger.rehydrerBehandling(avklaringer = avklaring.toList())
        private val personRepository = PersonRepositoryPostgres(behandlingRepository)

        init {
            lagre()
        }

        fun lagre() {
            val person = Person(Ident(behandling.behandler.ident), listOf(behandling.somKjede()))
            personRepository.lagre(person)
        }

        fun avklar(
            saksbehandler: String,
            begrunnelse: String,
        ) {
            val avklaringId = behandling.avklaringer().first().id
            behandling.håndter(
                AvklaringKvittertHendelse(
                    meldingsreferanseId = UUIDv7.ny(),
                    ident = "123",
                    avklaringId = avklaringId,
                    behandlingId = behandlingId,
                    saksbehandler = saksbehandler,
                    begrunnelse = begrunnelse,
                    opprettet = LocalDateTime.now(),
                ),
            )
            lagre()
        }
    }

    private fun avklaring(avklaringkode: Avklaringkode) = Avklaring(avklaringkode)

    private fun avklaringTest(block: Avklaringtest.() -> Unit) {
        withMigratedDb {
            val repository = AvklaringRepositoryPostgres(dataSource)
            val prosessregister =
                Prosessregister().also {
                    TestBehandlinger.registrerTestProsesser(it)
                }
            val behandlingRepository = BehandlingRepositoryPostgres(opplysningerRepository(dataSource), repository, prosessregister)

            block(Avklaringtest(repository, behandlingRepository))
        }
    }

    private data class Avklaringtest(
        val repository: AvklaringRepository,
        val behandlingRepository: BehandlingRepositoryPostgres,
    )
}

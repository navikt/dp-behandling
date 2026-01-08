package no.nav.dagpenger.behandling.mediator.repository

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.avklaring.Avklaring.Endring.UnderBehandling
import no.nav.dagpenger.avklaring.Avklaringer
import no.nav.dagpenger.behandling.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import no.nav.dagpenger.regel.hendelse.Søknadstype
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AvklaringRepositoryPostgresTest {
    private val rapid = TestRapid()
    private val repository = AvklaringRepositoryPostgres()

    @Test
    fun `lagrer og rehydrer avklaringer`() {
        withMigratedDb {
            val kode1 = Avklaringkode("JobbetUtenforNorge", "Arbeid utenfor Norge", "Personen har oppgitt arbeid utenfor Norge")
            val kode2 = Avklaringkode("HarVunnetNobelprisen", "Har vunnet nobelprisen", "Personen har vunnet en nobelpris ")

            val behandling = TestBehandling(avklaring(kode1), avklaring(kode2))
            val avklaringer = repository.hentAvklaringer(behandling.behandlingId)

            avklaringer.size shouldBe 2
        }
    }

    @Test
    fun `lagrer kilde og begrunnelse på avklarte avklaringer`() {
        withMigratedDb {
            val kode1 = Avklaringkode("JobbetUtenforNorge", "Arbeid utenfor Norge", "Personen har oppgitt arbeid utenfor Norge")

            val behandling = TestBehandling(avklaring(kode1))
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
        withMigratedDb {
            val kode1 = Avklaringkode("JobbetUtenforNorge", "Arbeid utenfor Norge", "Personen har oppgitt arbeid utenfor Norge")

            val avklaring = avklaring(kode1)
            val avklaringer = Avklaringer(emptyList(), listOf(avklaring))
            avklaringer.kvitter(avklaring.id, Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("123")), "begrunnelse")
            avklaringer.gjenåpne(avklaring.id)
            avklaringer.avklar(avklaring.id, Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("123")))

            val forventedeTilstander = listOf("UnderBehandling", "Avklart", "UnderBehandling", "Avklart")
            avklaring.endringer.map { it::class.simpleName!! } shouldBe forventedeTilstander

            val behandling = TestBehandling(avklaring)
            val avklaringerFraDb = repository.hentAvklaringer(behandling.behandlingId)

            repository.hentAvklaringer(TestBehandling(avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(avklaring).behandlingId)
            repository.hentAvklaringer(TestBehandling(avklaring).behandlingId)

            avklaringerFraDb.single().endringer.map { it::class.simpleName!! } shouldBe forventedeTilstander
        }
    }

    private inner class TestBehandling(
        vararg avklaring: Avklaring,
    ) {
        val behandlingId get() = behandling.behandlingId
        private val behandling = behandling(*avklaring)
        private val behandlingRepository = BehandlingRepositoryPostgres(opplysningerRepository(), repository)

        init {
            lagre()
        }

        fun lagre() {
            val unitOfWork = PostgresUnitOfWork.transaction()
            behandlingRepository.lagre(behandling, unitOfWork)
            repository.lagreAvklaringer(behandling, unitOfWork)
            unitOfWork.commit()
        }

        private fun behandling(vararg avklaring: Avklaring) =
            Behandling.rehydrer(
                behandlingId = UUIDv7.ny(),
                behandler =
                    SøknadInnsendtHendelse(
                        UUIDv7.ny(),
                        "123",
                        UUIDv7.ny(),
                        LocalDate.now(),
                        1,
                        LocalDateTime.now(),
                        Søknadstype.NySøknad,
                    ),
                gjeldendeOpplysninger =
                    Opplysninger.med(
                        Faktum(hendelseTypeOpplysningstype, "Søknad"),
                        Faktum(prøvingsdato, LocalDate.now(), Gyldighetsperiode(LocalDate.now())),
                        Faktum(kravTilAlder, false),
                    ),
                opprettet = LocalDateTime.now(),
                tilstand = TilstandType.ForslagTilVedtak,
                sistEndretTilstand = LocalDateTime.now(),
                avklaringer = avklaring.toList(),
            )

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

    private fun avklaring(avklaringkode: Avklaringkode) = Avklaring.rehydrer(UUIDv7.ny(), avklaringkode, mutableListOf(UnderBehandling()))
}

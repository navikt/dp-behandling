package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class BehandlingRepositoryPostgresTest {
    private val ident = "123456789011"
    private val søknadId = UUIDv7.ny()
    private val søknadInnsendtHendelse =
        SøknadInnsendtHendelse(
            meldingsreferanseId = søknadId,
            ident = ident,
            søknadId = søknadId,
            gjelderDato = LocalDate.now(),
            fagsakId = 1,
            opprettet = LocalDateTime.now(),
        )
    private val prøvingsdatoOpplysning = Faktum(prøvingsdato, LocalDate.now())
    private val tidligereOpplysning =
        Faktum(Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "tidligere-opplysning"), 1.0)
    private val basertPåBehandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = søknadInnsendtHendelse,
            gjeldendeOpplysninger = Opplysninger(listOf(prøvingsdatoOpplysning, tidligereOpplysning)),
            tilstand = Behandling.TilstandType.Ferdig,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = emptyList(),
        )
    private val opplysning1 = Faktum(Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "aktiv-opplysning1"), 1.0)
    private val opplysning2 = Faktum(Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "aktiv-opplysning2"), 2.0)
    private val opplysning3 = Faktum(Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "aktiv-opplysning3"), false)

    private val avklaring =
        Avklaring.rehydrer(UUIDv7.ny(), Avklaringspunkter.JobbetUtenforNorge, mutableListOf(Avklaring.Endring.UnderBehandling()))

    private val behandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = søknadInnsendtHendelse,
            gjeldendeOpplysninger = Opplysninger(listOf(opplysning1, opplysning2, opplysning3)),
            basertPå = listOf(basertPåBehandling),
            tilstand = Behandling.TilstandType.UnderBehandling,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = listOf(avklaring),
        )

    @Test
    fun `lagre og hent behandling fra postgres`() {
        withMigratedDb {
            val avklaringRepository = AvklaringRepositoryPostgres()
            val behandlingRepositoryPostgres = BehandlingRepositoryPostgres(opplysningerRepository(), avklaringRepository)

            behandlingRepositoryPostgres.lagre(basertPåBehandling)
            behandlingRepositoryPostgres.lagre(behandling)

            val rehydrertBehandling = behandlingRepositoryPostgres.hentBehandling(behandling.behandlingId).shouldNotBeNull()

            rehydrertBehandling.behandlingId shouldBe behandling.behandlingId
            rehydrertBehandling.basertPå.size shouldBe behandling.basertPå.size
            rehydrertBehandling.basertPå shouldContainExactly behandling.basertPå

            rehydrertBehandling.opplysninger().finnAlle().size shouldBe behandling.opplysninger().finnAlle().size
            rehydrertBehandling.opplysninger().finnAlle() shouldContainExactly behandling.opplysninger().finnAlle()

            val avklaringer = avklaringRepository.hentAvklaringer(behandling.behandlingId)
            avklaringer.size shouldBe 1
            with(avklaringer.first()) {
                val førsteEndring = endringer.first()

                id shouldBe avklaring.id
                kode shouldBe avklaring.kode
                endringer.size shouldBe 1

                with(førsteEndring) {
                    javaClass.simpleName shouldBe
                        avklaring.endringer
                            .first()
                            .javaClass.simpleName
                    id shouldBe avklaring.endringer.first().id
                    endret.truncatedTo(ChronoUnit.SECONDS) shouldBe
                        avklaring.endringer
                            .first()
                            .endret
                            .truncatedTo(ChronoUnit.SECONDS)
                }
            }
        }
    }
}

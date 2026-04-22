package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.mediator.registrerRegelverk
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.UnderBehandling
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.hendelse.Søknadstype
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
            Søknadstype.NySøknad,
        )
    private val prøvingsdatoOpplysning = Faktum(prøvingsdato, LocalDate.now())
    private val tidligereOpplysning =
        Faktum(Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "tidligere-opplysning"), 1.0)
    private val basertPåBehandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = søknadInnsendtHendelse,
            gjeldendeOpplysninger = Opplysninger.med(prøvingsdatoOpplysning, tidligereOpplysning),
            opprettet = LocalDateTime.now(),
            tilstand = Ferdig,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = emptyList(),
        )
    private val opplysningstype1 = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "aktiv-opplysning1")
    private val opplysningstype2 = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "aktiv-opplysning2")
    private val opplysningstype3 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "aktiv-opplysning3")
    private val opplysningstype4 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "aktiv-opplysning4")
    private val opplysningstype5 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "aktiv-opplysning5")

    private val opplysningerRepository = OpplysningerRepositoryPostgres()
    private val opplysningstyper =
        listOf(opplysningstype1, opplysningstype2, opplysningstype3, opplysningstype4, opplysningstype5, prøvingsdato).toSet()

    private val opplysning1 = Faktum(opplysningstype1, 1.0)
    private val opplysning2 = Faktum(opplysningstype2, 2.0)
    private val opplysning3 = Faktum(opplysningstype3, false)
    private val opplysning4 = Faktum(opplysningstype4, false)
    private val opplysning5 = Faktum(opplysningstype5, false)

    private val avklaring =
        Avklaring.rehydrer(UUIDv7.ny(), Avklaringspunkter.JobbetUtenforNorge, mutableListOf(Avklaring.Endring.UnderBehandling()))

    private val behandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = søknadInnsendtHendelse,
            gjeldendeOpplysninger = Opplysninger.med(opplysning1, opplysning2, opplysning3),
            basertPå = basertPåBehandling,
            opprettet = LocalDateTime.now(),
            tilstand = UnderBehandling,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = listOf(avklaring),
        )

    private val behandlingGren =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = søknadInnsendtHendelse,
            gjeldendeOpplysninger = Opplysninger.med(opplysning4),
            basertPå = basertPåBehandling,
            opprettet = LocalDateTime.now(),
            tilstand = UnderBehandling,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = listOf(avklaring),
        )

    fun nyBehandling(
        basertPå: Behandling? = null,
        tilstand: Behandling.TilstandType,
        opplysninger: Opplysninger = Opplysninger.med(opplysning1, opplysning2, opplysning3),
    ) = Behandling.rehydrer(
        behandlingId = UUIDv7.ny(),
        behandler = søknadInnsendtHendelse,
        gjeldendeOpplysninger = opplysninger, // Opplysninger.med(opplysning4),
        basertPå = basertPå,
        opprettet = LocalDateTime.now(),
        tilstand = tilstand,
        sistEndretTilstand = LocalDateTime.now(),
        avklaringer = listOf(avklaring),
    )

    @Test
    fun `lagre og hente en kjede med grener fra postgres`() {
        withMigratedDb {
            // Registrer forretningsprosesser og opplysningstyper
            registrerRegelverk(opplysningerRepository, opplysningstyper)

            val avklaringRepository = AvklaringRepositoryPostgres()
            val behandlingRepositoryPostgres = BehandlingRepositoryPostgres(opplysningerRepository, avklaringRepository)

            val b1 = nyBehandling(null, Ferdig, Opplysninger.med(prøvingsdatoOpplysning))
            val b2 = nyBehandling(b1, Ferdig, Opplysninger.med(opplysning2))
            val bastard = nyBehandling(b2, UnderBehandling, Opplysninger.med(opplysning1))
            val b3 = nyBehandling(b2, Ferdig, Opplysninger.med(opplysning3))
            val b4 = nyBehandling(b3, Ferdig, Opplysninger.med(opplysning4))
            val b5 = nyBehandling(b4, UnderBehandling, Opplysninger.med(opplysning5))

            behandlingRepositoryPostgres.lagre(b1)
            behandlingRepositoryPostgres.lagre(b2)
            behandlingRepositoryPostgres.lagre(bastard)
            behandlingRepositoryPostgres.lagre(b3)
            behandlingRepositoryPostgres.lagre(b4)
            behandlingRepositoryPostgres.lagre(b5)

            val behandlinger =
                behandlingRepositoryPostgres.hentBehandlinger(
                    listOf(b1.behandlingId, b2.behandlingId, bastard.behandlingId, b3.behandlingId, b4.behandlingId, b5.behandlingId),
                )

            behandlinger.size shouldBe 6

            behandlinger[0].behandlingId shouldBe b1.behandlingId
            behandlinger[1].behandlingId shouldBe b2.behandlingId
            behandlinger[2].behandlingId shouldBe b3.behandlingId
            behandlinger[3].behandlingId shouldBe b4.behandlingId
            behandlinger[4].behandlingId shouldBe bastard.behandlingId
            behandlinger[5].behandlingId shouldBe b5.behandlingId
        }
    }

    @Test
    fun `flytter en eldre behandling til å peke på en nyere`() {
        withMigratedDb {
            // Registrer forretningsprosesser og opplysningstyper
            registrerRegelverk(opplysningerRepository, opplysningstyper)

            val avklaringRepository = AvklaringRepositoryPostgres()
            val behandlingRepositoryPostgres = BehandlingRepositoryPostgres(opplysningerRepository, avklaringRepository)

            val b1 = nyBehandling(null, Ferdig, Opplysninger.med(prøvingsdatoOpplysning))
            val b2 = nyBehandling(b1, UnderBehandling, Opplysninger.med(opplysning2))
            val b3 = nyBehandling(b1, Ferdig, Opplysninger.med(opplysning3))

            behandlingRepositoryPostgres.lagre(b1)
            behandlingRepositoryPostgres.lagre(b2)
            behandlingRepositoryPostgres.lagre(b3)

            behandlingRepositoryPostgres
                .hentBehandlinger(
                    listOf(b1.behandlingId, b2.behandlingId, b3.behandlingId),
                ).also { behandlingerFørFlytt ->
                    behandlingerFørFlytt.size shouldBe 3

                    behandlingerFørFlytt[0].behandlingId shouldBe b1.behandlingId
                    behandlingerFørFlytt[1].behandlingId shouldBe b2.behandlingId
                    behandlingerFørFlytt[2].behandlingId shouldBe b3.behandlingId
                }

            behandlingRepositoryPostgres.flyttBehandling(b2.behandlingId, b3.behandlingId)

            behandlingRepositoryPostgres
                .hentBehandlinger(
                    listOf(b1.behandlingId, b2.behandlingId, b3.behandlingId),
                ).also { behandlingerEtterFlytt ->
                    behandlingerEtterFlytt.size shouldBe 3

                    behandlingerEtterFlytt[0].behandlingId shouldBe b1.behandlingId
                    behandlingerEtterFlytt[1].behandlingId shouldBe b3.behandlingId
                    behandlingerEtterFlytt[2].behandlingId shouldBe b2.behandlingId
                }
        }
    }

    @Test
    fun `lagre og hent behandling fra postgres`() {
        withMigratedDb {
            // Registrer forretningsprosesser og opplysningstyper
            registrerRegelverk(opplysningerRepository, opplysningstyper)

            val avklaringRepository = AvklaringRepositoryPostgres()
            val behandlingRepositoryPostgres = BehandlingRepositoryPostgres(opplysningerRepository(), avklaringRepository)

            behandlingRepositoryPostgres.lagre(basertPåBehandling)
            behandlingRepositoryPostgres.lagre(behandling)

            val rehydrertBehandling = behandlingRepositoryPostgres.hentBehandling(behandling.behandlingId).shouldNotBeNull()

            rehydrertBehandling.behandlingId shouldBe behandling.behandlingId
            rehydrertBehandling.basertPå shouldBe behandling.basertPå

            rehydrertBehandling.opplysninger().somListe().size shouldBe behandling.opplysninger().somListe().size
            rehydrertBehandling.opplysninger().somListe() shouldContainExactly behandling.opplysninger().somListe()

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

package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.behandling.db.DBTestContext
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.UnderBehandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.somKjede
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class BehandlingRepositoryPostgresTest {
    private val ident = "12345678911"
    private val testHendelse = TestBehandlinger.lagTestHendelse(ident)

    private val datoOpplysningstype = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "test-dato")
    private val datoOpplysning = Faktum(datoOpplysningstype, LocalDate.now())
    private val tidligereOpplysning =
        Faktum(Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "tidligere-opplysning"), 1.0)
    private val basertPåBehandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = testHendelse,
            gjeldendeOpplysninger = Opplysninger.med(datoOpplysning, tidligereOpplysning),
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

    private val opplysningstyper =
        listOf(opplysningstype1, opplysningstype2, opplysningstype3, opplysningstype4, opplysningstype5, datoOpplysningstype).toSet()

    private val opplysning1 = Faktum(opplysningstype1, 1.0)
    private val opplysning2 = Faktum(opplysningstype2, 2.0)
    private val opplysning3 = Faktum(opplysningstype3, false)
    private val opplysning4 = Faktum(opplysningstype4, false)
    private val opplysning5 = Faktum(opplysningstype5, false)

    private val avklaring =
        Avklaring(TestBehandlinger.testAvklaringskode)

    private val behandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = testHendelse,
            gjeldendeOpplysninger = Opplysninger.med(opplysning1, opplysning2, opplysning3),
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
        behandler = testHendelse,
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
            val kildeRepository = KildeRepository(dataSource)
            val opplysningerRepository = OpplysningerRepositoryPostgres(dataSource, kildeRepository)
            // Registrer forretningsprosesser og opplysningstyper
            val prosessregister = Prosessregister()
            TestBehandlinger.registrerTestProsesser(prosessregister)
            opplysningerRepository.lagreOpplysningstyper(opplysningstyper)

            val avklaringRepository = AvklaringRepositoryPostgres(dataSource, kildeRepository)
            val behandlingRepositoryPostgres =
                BehandlingRepositoryPostgres(dataSource, opplysningerRepository, avklaringRepository, kildeRepository, prosessregister)

            val b1 = nyBehandling(null, Ferdig, Opplysninger.med(datoOpplysning))
            val b2 = nyBehandling(b1, Ferdig, Opplysninger.med(opplysning2))
            val bastard = nyBehandling(b2, UnderBehandling, Opplysninger.med(opplysning1))
            val b3 = nyBehandling(b2, Ferdig, Opplysninger.med(opplysning3))
            val b4 = nyBehandling(b3, Ferdig, Opplysninger.med(opplysning4))
            val b5 = nyBehandling(b4, UnderBehandling, Opplysninger.med(opplysning5))

            opprettKjede(behandlingRepositoryPostgres, listOf(b1, b2, b3, bastard, b4, b5))

            val kjeden = behandlingRepositoryPostgres.hentBehandlinger(Ident(ident)).single()

            kjeden.nesteSomKanBaseresPå shouldBe b4
            kjeden.etterkommere shouldBe 5

            val behandlinger = kjeden.toList()

            behandlinger.size shouldBe 6

            behandlinger[0].behandlingId shouldBe b1.behandlingId
            behandlinger[1].behandlingId shouldBe b2.behandlingId
            behandlinger[2].behandlingId shouldBe b3.behandlingId
            behandlinger[3].behandlingId shouldBe bastard.behandlingId
            behandlinger[4].behandlingId shouldBe b4.behandlingId
            behandlinger[5].behandlingId shouldBe b5.behandlingId
        }
    }

    @Test
    fun `flytter en eldre behandling til å peke på en nyere`() {
        withMigratedDb {
            val kildeRepository = KildeRepository(dataSource)
            val opplysningerRepository = OpplysningerRepositoryPostgres(dataSource, kildeRepository)
            // Registrer forretningsprosesser og opplysningstyper
            val prosessregister = Prosessregister()
            TestBehandlinger.registrerTestProsesser(prosessregister)
            opplysningerRepository.lagreOpplysningstyper(opplysningstyper)

            val avklaringRepository = AvklaringRepositoryPostgres(dataSource, kildeRepository)
            val behandlingRepositoryPostgres =
                BehandlingRepositoryPostgres(dataSource, opplysningerRepository, avklaringRepository, kildeRepository, prosessregister)

            val b1 = nyBehandling(null, Ferdig, Opplysninger.med(datoOpplysning))
            val b2 = nyBehandling(b1, UnderBehandling, Opplysninger.med(opplysning2))
            val b3 = nyBehandling(b1, Ferdig, Opplysninger.med(opplysning3))

            opprettKjede(behandlingRepositoryPostgres, listOf(b1, b2, b3))

            behandlingRepositoryPostgres.hentBehandlinger(Ident(ident)).single().toList().also { behandlingerFørFlytt ->
                behandlingerFørFlytt.size shouldBe 3

                behandlingerFørFlytt[0].behandlingId shouldBe b1.behandlingId
                behandlingerFørFlytt[1].behandlingId shouldBe b2.behandlingId
                behandlingerFørFlytt[2].behandlingId shouldBe b3.behandlingId
            }

            behandlingRepositoryPostgres.flyttBehandling(b2.behandlingId, b3.behandlingId)

            behandlingRepositoryPostgres.hentBehandlinger(Ident(ident)).single().toList().also { behandlingerEtterFlytt ->
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
            val kildeRepository = KildeRepository(dataSource)
            val opplysningerRepository = OpplysningerRepositoryPostgres(dataSource, kildeRepository)
            // Registrer forretningsprosesser og opplysningstyper
            val prosessregister = Prosessregister()
            TestBehandlinger.registrerTestProsesser(prosessregister)
            opplysningerRepository.lagreOpplysningstyper(opplysningstyper)

            val avklaringRepository = AvklaringRepositoryPostgres(dataSource, kildeRepository)
            val behandlingRepositoryPostgres =
                BehandlingRepositoryPostgres(
                    dataSource = dataSource,
                    opplysningRepository = opplysningerRepository(dataSource),
                    avklaringRepository = avklaringRepository,
                    kildeRepository = kildeRepository,
                    prosessregister = prosessregister,
                )

            opprettKjede(behandlingRepositoryPostgres, listOf(basertPåBehandling, behandling))

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

    private fun DBTestContext.opprettKjede(
        behandlingRepositoryPostgres: BehandlingRepositoryPostgres,
        behandlinger: List<Behandling>,
    ) {
        val personRepositoryPostgres = PersonRepositoryPostgres(dataSource, behandlingRepositoryPostgres)

        val kjeder =
            behandlinger
                .groupBy { it.behandlingskjedeId }
                .map { (_, behandlingskjede) -> behandlingskjede.somKjede() }

        Person(Ident(ident), kjeder).also {
            personRepositoryPostgres.lagre(it)
        }
    }
}

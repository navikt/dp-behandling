package no.nav.dagpenger.mediator.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.mediator.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.mediator.db.DBTestContext
import no.nav.dagpenger.mediator.db.withMigratedDb
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Behandling.TilstandType.Avbrutt
import no.nav.dagpenger.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.modell.Behandling.TilstandType.ForslagTilVedtak
import no.nav.dagpenger.modell.Behandling.TilstandType.TilBeslutning
import no.nav.dagpenger.modell.Behandling.TilstandType.UnderBehandling
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.Person
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.modell.somKjede
import no.nav.dagpenger.opplysning.Aktør
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Systemaktør
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class BehandlingRepositoryPostgresTest {
    private val ident = "12345678911"
    private val testHendelse =
        TestBehandlinger.lagTestHendelse(
            ident,
            opprettetAv = Saksbehandler("Z123456"),
        )

    private val datoOpplysningstype = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "test-dato")
    private val datoOpplysning = Faktum(datoOpplysningstype, LocalDate.now())
    private val tidligereOpplysningstype = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "tidligere-opplysning")
    private val tidligereOpplysning = Faktum(tidligereOpplysningstype, 1.0)
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
        listOf(
            opplysningstype1,
            opplysningstype2,
            opplysningstype3,
            opplysningstype4,
            opplysningstype5,
            datoOpplysningstype,
            tidligereOpplysningstype,
        ).toSet()

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
            val kildeRepository = KildeRepository(dbSession)
            val opplysningerRepository = OpplysningerRepositoryPostgres(dbSession, kildeRepository)
            // Registrer forretningsprosesser og opplysningstyper
            val prosessregister = Prosessregister()
            TestBehandlinger.registrerTestProsesser(prosessregister)
            opplysningerRepository.lagreOpplysningstyper(opplysningstyper)

            val avklaringRepository = AvklaringRepositoryPostgres(dbSession, kildeRepository)
            val behandlingRepositoryPostgres =
                BehandlingRepositoryPostgres(dbSession, opplysningerRepository, avklaringRepository, kildeRepository, prosessregister)

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
    fun `flytting lagres som ny behandling og gammel avbrytes`() {
        withMigratedDb {
            val (personRepository, behandlingRepository) = flyttOppsett()
            val b1 = nyBehandling(null, Ferdig, Opplysninger.med(datoOpplysning))
            val b2 = nyBehandling(b1, ForslagTilVedtak, Opplysninger.med(opplysning2))
            val b3 = nyBehandling(null, Ferdig, Opplysninger.med(opplysning3))
            opprettKjede(behandlingRepository, listOf(b1, b2, b3))

            val person = personRepository.hent(Ident(ident)).shouldNotBeNull()
            person.håndter(flytt(b2.behandlingId, b3.behandlingId))
            personRepository.lagre(person)

            val behandlinger = behandlingRepository.hentBehandlinger(Ident(ident)).flatMap { it.toList() }
            behandlinger.size shouldBe 4
            val gammel = behandlinger.single { it.behandlingId == b2.behandlingId }
            gammel.basertPå?.behandlingId shouldBe b1.behandlingId
            gammel.tilstand().first shouldBe Behandling.TilstandType.Avbrutt
            val ny = behandlinger.single { it.basertPå?.behandlingId == b3.behandlingId }
            ny.tilstand().first shouldBe Behandling.TilstandType.Redigert
        }
    }

    @Test
    fun `flytting til ny rot lar gammel avbrutt behandling bli i opprinnelig kjede`() {
        withMigratedDb {
            val (personRepository, behandlingRepository) = flyttOppsett()
            val b1 = nyBehandling(null, Ferdig, Opplysninger.med(datoOpplysning))
            val b2 = nyBehandling(b1, ForslagTilVedtak, Opplysninger.med(opplysning2))
            opprettKjede(behandlingRepository, listOf(b1, b2))

            val person = personRepository.hent(Ident(ident)).shouldNotBeNull()
            person.håndter(flytt(b2.behandlingId, null))
            personRepository.lagre(person)

            val kjeder = behandlingRepository.hentBehandlinger(Ident(ident))
            kjeder.size shouldBe 2
            val opprinnelig = kjeder.single { it.rot.behandlingId == b1.behandlingId }
            opprinnelig.map { it.behandlingId }.toSet() shouldBe setOf(b1.behandlingId, b2.behandlingId)
            opprinnelig.single { it.behandlingId == b2.behandlingId }.tilstand().first shouldBe Behandling.TilstandType.Avbrutt
            val nyKjede = kjeder.single { it.rot.behandlingId != b1.behandlingId }
            nyKjede.toList().size shouldBe 1
        }
    }

    @Test
    fun `flytt søsken lagrer ny forelder og beholder id og opplysninger`() {
        withMigratedDb {
            val (personRepository, behandlingRepository) = flyttOppsett()
            val a = nyBehandling(null, Ferdig, Opplysninger.med(datoOpplysning))
            val b1 = nyBehandling(a, Ferdig, Opplysninger.med(opplysning3))
            val b2 = nyBehandling(a, ForslagTilVedtak, Opplysninger.med(opplysning2))
            opprettKjede(behandlingRepository, listOf(a, b1, b2))

            val person = personRepository.hent(Ident(ident)).shouldNotBeNull()
            person.håndter(
                flytt(b2.behandlingId, b1.behandlingId),
            )
            personRepository.lagre(person)

            val kjeder = behandlingRepository.hentBehandlinger(Ident(ident))
            kjeder.size shouldBe 1
            val flyttet = kjeder.single().single { it.behandlingId == b2.behandlingId }
            flyttet.basertPå?.behandlingId shouldBe b1.behandlingId
            flyttet.opplysninger().somListe(Egne).map { it.id } shouldContain opplysning2.id
            kjeder.single().toList().size shouldBe 3
        }
    }

    @Test
    fun `finnSøsken utelater søsken som er ferdige, avbrutt eller til beslutning`() {
        withMigratedDb {
            val (_, behandlingRepository) = flyttOppsett()
            val a = nyBehandling(null, Ferdig, Opplysninger.med(datoOpplysning))
            val ferdig = nyBehandling(a, Ferdig, Opplysninger.med(opplysning3))
            val forslag = nyBehandling(a, ForslagTilVedtak, Opplysninger.med(opplysning2))
            val tilBeslutning = nyBehandling(a, TilBeslutning, Opplysninger.med(opplysning2))
            val avbrutt = nyBehandling(a, Avbrutt, Opplysninger.med(opplysning2))
            opprettKjede(behandlingRepository, listOf(a, ferdig, forslag, tilBeslutning, avbrutt))

            behandlingRepository.finnSøsken(ferdig.behandlingId) shouldContainExactly listOf(forslag.behandlingId)
        }
    }

    @Test
    fun `feil under flytting lagrer ingenting`() {
        withMigratedDb {
            val (personRepository, behandlingRepository) = flyttOppsett()
            val b1 = nyBehandling(null, Ferdig, Opplysninger.med(datoOpplysning))
            val b2 = nyBehandling(b1, ForslagTilVedtak, Opplysninger.med(opplysning2))
            val ikkeFerdig = nyBehandling(null, UnderBehandling, Opplysninger.med(opplysning3))
            opprettKjede(behandlingRepository, listOf(b1, b2, ikkeFerdig))

            val person = personRepository.hent(Ident(ident)).shouldNotBeNull()
            shouldThrow<IllegalArgumentException> { person.håndter(flytt(b2.behandlingId, ikkeFerdig.behandlingId)) }

            val behandlinger = behandlingRepository.hentBehandlinger(Ident(ident)).flatMap { it.toList() }
            behandlinger.size shouldBe 3
            behandlinger.single { it.behandlingId == b2.behandlingId }.tilstand().first shouldBe ForslagTilVedtak
        }
    }

    private fun flytt(
        behandlingId: UUID,
        nyBasertPåId: UUID?,
    ) = FlyttBehandlingHendelse(
        meldingsreferanseId = UUIDv7.ny(),
        ident = ident,
        behandlingId = behandlingId,
        nyBasertPåId = nyBasertPåId,
        opprettet = LocalDateTime.now(),
    )

    private fun DBTestContext.flyttOppsett(): Pair<PersonRepositoryPostgres, BehandlingRepositoryPostgres> {
        val kildeRepository = KildeRepository(dbSession)
        val opplysningerRepository = OpplysningerRepositoryPostgres(dbSession, kildeRepository)
        val prosessregister = Prosessregister()
        TestBehandlinger.registrerTestProsesser(prosessregister)
        opplysningerRepository.lagreOpplysningstyper(opplysningstyper)
        val avklaringRepository = AvklaringRepositoryPostgres(dbSession, kildeRepository)
        val behandlingRepository =
            BehandlingRepositoryPostgres(dbSession, opplysningerRepository, avklaringRepository, kildeRepository, prosessregister)
        return PersonRepositoryPostgres(dbSession, behandlingRepository) to behandlingRepository
    }

    @Test
    fun `lagre og hent behandling fra postgres`() {
        withMigratedDb {
            val kildeRepository = KildeRepository(dbSession)
            val opplysningerRepository = OpplysningerRepositoryPostgres(dbSession, kildeRepository)
            // Registrer forretningsprosesser og opplysningstyper
            val prosessregister = Prosessregister()
            TestBehandlinger.registrerTestProsesser(prosessregister)
            opplysningerRepository.lagreOpplysningstyper(opplysningstyper)

            val avklaringRepository = AvklaringRepositoryPostgres(dbSession, kildeRepository)
            val behandlingRepositoryPostgres =
                BehandlingRepositoryPostgres(
                    dbSession = dbSession,
                    opplysningRepository = opplysningerRepository(dbSession),
                    avklaringRepository = avklaringRepository,
                    kildeRepository = kildeRepository,
                    prosessregister = prosessregister,
                )

            val systemaktørOppretter = Systemaktør("dp-klient")
            val systemBehandling = nyBehandlingMedOppretter(systemaktørOppretter, Opplysninger.med(opplysning4))
            val behandlingUtenOppretter = nyBehandlingMedOppretter(null, Opplysninger.med(opplysning5))
            opprettKjede(
                behandlingRepositoryPostgres,
                listOf(basertPåBehandling, behandling, systemBehandling, behandlingUtenOppretter),
            )

            val rehydrertBehandling = behandlingRepositoryPostgres.hentBehandling(behandling.behandlingId).shouldNotBeNull()

            rehydrertBehandling.behandlingId shouldBe behandling.behandlingId
            rehydrertBehandling.basertPå shouldBe behandling.basertPå
            rehydrertBehandling.opprettetAv shouldBe Saksbehandler("Z123456")

            behandlingRepositoryPostgres.hentBehandling(systemBehandling.behandlingId)?.opprettetAv shouldBe systemaktørOppretter
            behandlingRepositoryPostgres.hentBehandling(behandlingUtenOppretter.behandlingId)?.opprettetAv shouldBe null

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

    private fun nyBehandlingMedOppretter(
        oppretter: Aktør?,
        opplysninger: Opplysninger,
    ): Behandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = TestBehandlinger.lagTestHendelse(ident, opprettetAv = oppretter),
            gjeldendeOpplysninger = opplysninger,
            opprettet = LocalDateTime.now(),
            tilstand = UnderBehandling,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = emptyList(),
        )

    private fun DBTestContext.opprettKjede(
        behandlingRepositoryPostgres: BehandlingRepositoryPostgres,
        behandlinger: List<Behandling>,
    ) {
        val personRepositoryPostgres = PersonRepositoryPostgres(dbSession, behandlingRepositoryPostgres)

        val kjeder =
            behandlinger
                .groupBy { it.behandlingskjedeId }
                .map { (_, behandlingskjede) -> behandlingskjede.somKjede() }

        Person(Ident(ident), kjeder).also {
            personRepositoryPostgres.lagre(it)
        }
    }
}

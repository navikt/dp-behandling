package no.nav.dagpenger.mediator.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotliquery.queryOf
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.mediator.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.mediator.db.DBTestContext
import no.nav.dagpenger.mediator.db.Postgres
import no.nav.dagpenger.mediator.db.withMigratedDb
import no.nav.dagpenger.mediator.repository.OpplysningerRepositoryPostgres.Companion.hentKjedeopplysninger
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Behandling.TilstandType.Avbrutt
import no.nav.dagpenger.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.modell.Behandling.TilstandType.ForslagTilVedtak
import no.nav.dagpenger.modell.Behandling.TilstandType.TilBeslutning
import no.nav.dagpenger.modell.Behandling.TilstandType.UnderBehandling
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.Person
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse.Companion.behandlingFlyttetAvklaring
import no.nav.dagpenger.modell.somKjede
import no.nav.dagpenger.opplysning.Aktør
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.OpplysningstypeRegister
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Systemaktør
import no.nav.dagpenger.opplysning.Utledning
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

            lagredeKjedeIder() shouldBe listOf(b1, b2, b3, bastard, b4, b5).associate { it.behandlingId to b1.behandlingId }

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
    fun `henting av en behandling arver ikke opplysninger fra sosken eller etterkommere`() {
        withMigratedDb {
            val repository = behandlingRepository()
            val opprinnelig = Faktum(opplysningstype1, 1.0)
            val rot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(opprinnelig))
            val egen = Faktum(opplysningstype1, 2.0, erstatter = opprinnelig)
            val barn = nyBehandling(rot, Ferdig, Opplysninger.med(egen))
            val søsken = nyBehandling(rot, Ferdig, Opplysninger.med(Faktum(opplysningstype1, 3.0, erstatter = opprinnelig)))
            val senere = nyBehandling(barn, UnderBehandling, Opplysninger.med(Faktum(opplysningstype1, 4.0, erstatter = egen)))
            opprettKjede(repository, listOf(rot, barn, søsken, senere))

            val kjedeopplysninger =
                dbSession.session { session ->
                    session.hentKjedeopplysninger(
                        KildeRepository(dbSession),
                        OpplysningstypeRegister(opplysningstyper),
                        setOf(rot.behandlingId),
                    )
                }
            kjedeopplysninger.keys shouldBe listOf(rot, barn, søsken, senere).map { it.opplysninger().id }.toSet()
            kjedeopplysninger.getValue(rot.opplysninger().id).rå().map { it.id } shouldContainExactly listOf(opprinnelig.id)

            val hentetRot = repository.hentBehandling(rot.behandlingId).shouldNotBeNull()
            hentetRot.opplysninger().somListe().map { it.id } shouldContainExactly listOf(opprinnelig.id)
            val hentetBarn = repository.hentBehandling(barn.behandlingId).shouldNotBeNull()
            hentetBarn.opplysninger().finnOpplysning(opplysningstype1).verdi shouldBe 2.0
            hentetBarn.opplysninger().somListe(Egne).map { it.id } shouldContainExactly listOf(egen.id)
            hentetBarn
                .opplysninger()
                .finnOpplysning(opplysningstype1)
                .erstatter
                ?.id shouldBe opprinnelig.id
        }
    }

    @Test
    fun `eksistenssjekken avgrenser til flyttede kjeder og finner koblinger fra etterkommere`() {
        withMigratedDb {
            val repository = behandlingRepository()
            val gammel = Faktum(opplysningstype1, 1.0)
            val gammelRot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(gammel))
            val egen = gammel.revider(2.0)
            val flyttet = nyBehandling(gammelRot, Ferdig, Opplysninger.med(egen))
            val utledet = Faktum(opplysningstype3, true, utledetAv = Utledning("test", listOf(gammel)))
            val barn = nyBehandling(flyttet, UnderBehandling, Opplysninger.med(utledet))
            val nyRot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(opplysning2))
            val sosken = nyBehandling(nyRot, Ferdig, Opplysninger.med(opplysning5))
            opprettKjede(repository, listOf(gammelRot, flyttet, barn, nyRot, sosken))
            val sql = requireNotNull(javaClass.getResource("/sql/finnes-kjedekoblinger-etter-flytting.sql")).readText()
            val listeSql = requireNotNull(javaClass.getResource("/sql/finn-kjedekoblinger-etter-flytting.sql")).readText()

            fun finnesAvvik(): Boolean =
                dbSession.session { session ->
                    session.run(queryOf(sql).map { it.boolean("finnes_kobling_utenfor_kjeden") }.asSingle).shouldNotBeNull()
                }

            fun finnAvvik(): List<Triple<UUID, String, Boolean>> =
                dbSession.session { session ->
                    session.run(
                        queryOf(listeSql)
                            .map {
                                val opplysningId = it.uuid("opplysning_id")
                                val eier = if (opplysningId == egen.id) flyttet else barn
                                it.uuid("behandlingskjede_id") shouldBe nyRot.behandlingId
                                it.uuid("behandling_id") shouldBe eier.behandlingId
                                it.uuid("opplysninger_id") shouldBe eier.opplysninger().id
                                it.uuid("maal_opplysning_id") shouldBe gammel.id
                                it.uuid("maal_opplysninger_id") shouldBe gammelRot.opplysninger().id
                                Triple(opplysningId, it.string("koblingstype"), it.boolean("maal_fjernet"))
                            }.asList,
                    )
                }

            finnesAvvik() shouldBe false
            finnAvvik() shouldBe emptyList()
            dbSession.transaction {
                AvklaringRepositoryPostgres(dbSession, KildeRepository(dbSession)).lagreAvklaringer(
                    listOf(flyttet, barn).map { it to Avklaring(behandlingFlyttetAvklaring) },
                    this,
                )
            }
            finnesAvvik() shouldBe false
            finnAvvik() shouldBe emptyList()
            dbSession.session { session ->
                session.run(
                    queryOf(
                        "UPDATE behandling SET basert_på_behandling_id = :nyRot WHERE behandling_id = :flyttet",
                        mapOf("nyRot" to nyRot.behandlingId, "flyttet" to flyttet.behandlingId),
                    ).asUpdate,
                )
            }
            finnesAvvik() shouldBe true
            finnAvvik() shouldContainExactlyInAnyOrder
                listOf(
                    Triple(egen.id, "erstatter", false),
                    Triple(egen.id, "identitet", false),
                    Triple(utledet.id, "utledet_av", false),
                )
            dbSession.session { session ->
                session.run(
                    queryOf(
                        "UPDATE opplysning SET erstatter_id = NULL, identitet_id = NULL WHERE id = :id",
                        mapOf("id" to egen.id),
                    ).asUpdate,
                )
                session.run(queryOf("UPDATE opplysning SET fjernet = TRUE WHERE id = :id", mapOf("id" to gammel.id)).asUpdate)
            }
            finnesAvvik() shouldBe true
            finnAvvik() shouldContainExactly listOf(Triple(utledet.id, "utledet_av", true))
            dbSession.session { session ->
                session.run(
                    queryOf(
                        "UPDATE opplysning_utledet_av SET utledet_av = :maal WHERE opplysning_id = :id",
                        mapOf("maal" to opplysning5.id, "id" to utledet.id),
                    ).asUpdate,
                )
            }
            finnesAvvik() shouldBe false
            finnAvvik() shouldBe emptyList()
        }
    }

    @Test
    fun `automatisk kjedekontroll summerer batcher uten a dobbelttelle behandlinger`() {
        withMigratedDb {
            val repository = behandlingRepository()
            val gammel = Faktum(UUID(0, 0), opplysningstype1, 1.0, opprettet = LocalDateTime.now())
            val rot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(gammel))
            val egen = gammel.revider(2.0)
            val annenEgen = gammel.revider(3.0)
            val utledet = Faktum(opplysningstype3, true, utledetAv = Utledning("test", listOf(gammel)))
            val flyttet = nyBehandling(rot, UnderBehandling, Opplysninger.med(egen, annenEgen, utledet))
            val nyRot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(opplysning2))
            opprettKjede(repository, listOf(rot, flyttet, nyRot))

            val header = "referansetype,avvikstype,maal_fjernet,antall_koblinger,antall_behandlinger,antall_kjeder"
            val utenAvvik = kjorAutomatiskKjedekontroll(batchstorrelse = 1)
            utenAvvik shouldContain "5,5,5"
            utenAvvik.substringAfter("AVVIK\n").trim() shouldBe header

            dbSession.session { session ->
                session.run(
                    queryOf(
                        "UPDATE behandling SET basert_på_behandling_id = :nyRot WHERE behandling_id = :flyttet",
                        mapOf("nyRot" to nyRot.behandlingId, "flyttet" to flyttet.behandlingId),
                    ).asUpdate,
                )
            }
            val medAvvik = kjorAutomatiskKjedekontroll(batchstorrelse = 1)
            medAvvik shouldContain "5,5,5"
            medAvvik
                .substringAfter("AVVIK\n")
                .trim()
                .lines()
                .toSet() shouldBe
                setOf(
                    header,
                    "erstatter,ulik behandlingskjede,f,2,1,1",
                    "identitet,ulik behandlingskjede,f,2,1,1",
                    "utledet_av,ulik behandlingskjede,f,1,1,1",
                )
            dbSession.session { session ->
                session.run(queryOf("UPDATE opplysning SET fjernet = TRUE WHERE id = :id", mapOf("id" to gammel.id)).asUpdate)
            }
            val medFjernetMaal = kjorAutomatiskKjedekontroll(batchstorrelse = 1)
            medFjernetMaal shouldContain "5,4,5"
            medFjernetMaal
                .substringAfter("AVVIK\n")
                .trim()
                .lines()
                .toSet() shouldBe
                setOf(
                    header,
                    "erstatter,ulik behandlingskjede,t,2,1,1",
                    "identitet,ulik behandlingskjede,t,2,1,1",
                    "utledet_av,ulik behandlingskjede,t,1,1,1",
                )
        }
    }

    @Test
    fun `automatisk kjedekontroll trenger ikke skriverettigheter til produksjonstabellene`() {
        withMigratedDb {
            val repository = behandlingRepository()
            val rot = nyBehandling(tilstand = Ferdig)
            opprettKjede(repository, listOf(rot))
            val bruker = "kjedekontroll_${UUID.randomUUID().toString().replace("-", "")}"
            dbSession.session { session ->
                session.run(
                    queryOf(
                        """
                        CREATE ROLE $bruker LOGIN;
                        GRANT USAGE ON SCHEMA public TO $bruker;
                        GRANT SELECT ON ALL TABLES IN SCHEMA public TO $bruker;
                        """.trimIndent(),
                    ).asExecute,
                )
            }
            try {
                dbSession.session { session ->
                    session.run(
                        queryOf(
                            "SELECT has_table_privilege(:bruker, 'opplysning', 'UPDATE') AS kan_endre",
                            mapOf("bruker" to bruker),
                        ).map { it.boolean("kan_endre") }.asSingle,
                    ) shouldBe false
                }
                val resultat =
                    Postgres.kjorPsql(
                        this,
                        "sql/kontroller-kjedekoblinger.psql",
                        mapOf("batchstorrelse" to "1"),
                        bruker = bruker,
                        pgOptions = "-c work_mem=64kB -c plan_cache_mode=force_generic_plan -c default_transaction_read_only=on",
                    )
                withClue(resultat.stderr) {
                    resultat.exitCode shouldBe 0
                }
                resultat.stdout shouldContain "3,3,3"
            } finally {
                dbSession.session { session ->
                    session.run(queryOf("DROP OWNED BY $bruker; DROP ROLE $bruker;").asExecute)
                }
            }
        }
    }

    @Test
    fun `automatisk kjedekontroll fullforer nar databasen er tom`() {
        withMigratedDb {
            val rapport = kjorAutomatiskKjedekontroll()
            rapport shouldContain "0,0,0"
            rapport
                .substringAfter("AVVIK\n")
                .trim()
                .lines()
                .size shouldBe 1
        }
    }

    @Test
    fun `kontrollsporringen finner gamle kjedekoblinger uten persistert kjede-ID`() {
        withMigratedDb {
            val repository = behandlingRepository()
            val gammel = Faktum(UUID(0, 0), opplysningstype1, 1.0, opprettet = LocalDateTime.now())
            val rot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(gammel))
            val egen = gammel.revider(2.0)
            val utledet = Faktum(opplysningstype3, true, utledetAv = Utledning("test", listOf(gammel)))
            val flyttet = nyBehandling(rot, UnderBehandling, Opplysninger.med(egen, utledet))
            val nyRot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(opplysning2))
            opprettKjede(repository, listOf(rot, flyttet, nyRot))
            val kontrollSql = requireNotNull(javaClass.getResource("/sql/kontroller-kjedekoblinger.sql")).readText()

            data class Kontrollrad(
                val nesteEtterId: UUID?,
                val ferdig: Boolean,
                val antallLeste: Long,
                val antallAktive: Long,
                val referansetype: String?,
                val maalFjernet: Boolean,
                val antallKoblinger: Long,
            )

            fun finnBatch(
                etterId: UUID? = null,
                batchstorrelse: Int = 1000,
            ): List<Kontrollrad> =
                dbSession.session { session ->
                    session.run(
                        queryOf(
                            kontrollSql
                                .replace("NULL::uuid AS etter_id", ":etterId::uuid AS etter_id")
                                .replace("1000 AS batchstorrelse", ":batchstorrelse::int AS batchstorrelse"),
                            mapOf("etterId" to etterId, "batchstorrelse" to batchstorrelse),
                        ).map {
                            Kontrollrad(
                                nesteEtterId = it.uuidOrNull("neste_etter_id"),
                                ferdig = it.boolean("ferdig"),
                                antallLeste = it.long("antall_leste"),
                                antallAktive = it.long("antall_aktive"),
                                referansetype = it.stringOrNull("referansetype"),
                                maalFjernet = it.boolean("maal_fjernet"),
                                antallKoblinger = it.long("antall_koblinger"),
                            )
                        }.asList,
                    )
                }

            fun finnAvvik(): Map<String, Pair<Boolean, Long>> =
                finnBatch().mapNotNull { rad -> rad.referansetype?.let { it to (rad.maalFjernet to rad.antallKoblinger) } }.toMap()

            fun verifiserBatcher(forventetAntallAktive: Long = 4L) {
                var etterId: UUID? = null
                var antallLeste = 0L
                var antallAktive = 0L
                val markorer = mutableSetOf<UUID>()
                val avvik = mutableMapOf<String, Pair<Boolean, Long>>()
                while (true) {
                    val batch = finnBatch(etterId, batchstorrelse = 1)
                    val status = batch.first()
                    batch.map { it.nesteEtterId }.toSet().size shouldBe 1
                    antallLeste += status.antallLeste
                    antallAktive += status.antallAktive
                    batch.forEach { rad ->
                        rad.referansetype?.let { type ->
                            avvik[type] = rad.maalFjernet to ((avvik[type]?.second ?: 0L) + rad.antallKoblinger)
                        }
                    }
                    if (status.ferdig) {
                        status.antallLeste shouldBe 0L
                        status.nesteEtterId.shouldBeNull()
                        break
                    }
                    status.antallLeste shouldBe 1L
                    etterId = status.nesteEtterId.shouldNotBeNull()
                    markorer.add(etterId) shouldBe true
                }
                antallLeste shouldBe 4L
                markorer.size shouldBe 4
                avvik shouldBe finnAvvik()
                antallAktive shouldBe forventetAntallAktive
            }

            finnAvvik() shouldBe emptyMap()
            verifiserBatcher()
            dbSession.session { session ->
                session.run(
                    queryOf(
                        "UPDATE behandling SET basert_på_behandling_id = :nyRot WHERE behandling_id = :flyttet",
                        mapOf("nyRot" to nyRot.behandlingId, "flyttet" to flyttet.behandlingId),
                    ).asUpdate,
                )
            }
            finnAvvik() shouldBe mapOf("erstatter" to (false to 1L), "identitet" to (false to 1L), "utledet_av" to (false to 1L))
            verifiserBatcher()

            dbSession.session { session ->
                session.run(queryOf("UPDATE opplysning SET fjernet = TRUE WHERE id = :id", mapOf("id" to gammel.id)).asUpdate)
            }
            finnAvvik() shouldBe mapOf("erstatter" to (true to 1L), "identitet" to (true to 1L), "utledet_av" to (true to 1L))
            verifiserBatcher(forventetAntallAktive = 3L)
        }
    }

    @Test
    fun `kontrollsporringen bruker primaernokkelmarkor uten tempfiler med mange uvedkommende behandlinger`() {
        withMigratedDb {
            val repository = behandlingRepository()
            val gammel = Faktum(UUID(0, 0), opplysningstype1, 1.0, opprettet = LocalDateTime.now())
            val egen =
                Faktum(
                    id = UUID(0, 1),
                    opplysningstype = opplysningstype1,
                    verdi = 2.0,
                    opprettet = LocalDateTime.now(),
                    erstatter = gammel,
                    identitet = gammel.id,
                )
            val gammelRot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(gammel))
            val nyRot = nyBehandling(tilstand = Ferdig, opplysninger = Opplysninger.med(opplysning2))
            val flyttet = nyBehandling(nyRot, UnderBehandling, Opplysninger.med(egen))
            opprettKjede(repository, listOf(gammelRot, nyRot, flyttet))
            val kontrollSql =
                requireNotNull(javaClass.getResource("/sql/kontroller-kjedekoblinger.sql"))
                    .readText()
                    .replace("NULL::uuid AS etter_id", "'${gammel.id}'::uuid AS etter_id")
                    .replace("1000 AS batchstorrelse", "1 AS batchstorrelse")

            dbSession.transaction {
                session.run(
                    queryOf(
                        """
                        WITH bakgrunn AS MATERIALIZED (
                            SELECT gen_random_uuid() AS id FROM generate_series(1, 10000)
                        ),
                        opplysningssett AS (
                            INSERT INTO opplysninger (opplysninger_id) SELECT id FROM bakgrunn
                            RETURNING opplysninger_id
                        ),
                        behandlinger AS (
                            INSERT INTO behandling (behandling_id, behandlingskjede_id, tilstand)
                            SELECT opplysninger_id, opplysninger_id, 'Ferdig' FROM opplysningssett
                            RETURNING behandling_id
                        ),
                        koblinger AS (
                            INSERT INTO behandling_opplysninger (behandling_id, opplysninger_id)
                            SELECT behandling_id, behandling_id FROM behandlinger
                            RETURNING opplysninger_id
                        )
                        INSERT INTO opplysning (id, opplysninger_id, status, opplysningstype_id, datatype, opprettet)
                        SELECT k.opplysninger_id, k.opplysninger_id, o.status, o.opplysningstype_id, o.datatype, o.opprettet
                        FROM koblinger k
                        CROSS JOIN opplysning o
                        WHERE o.id = :id
                        """.trimIndent(),
                        mapOf("id" to gammel.id),
                    ).asUpdate,
                )
                session.run(queryOf("ANALYZE behandling, behandling_opplysninger, opplysning").asExecute)
                session.run(queryOf("SET LOCAL work_mem = '64kB'").asExecute)
                val plan =
                    session
                        .run(
                            queryOf("EXPLAIN (ANALYZE, BUFFERS) $kontrollSql")
                                .map { it.string("QUERY PLAN") }
                                .asList,
                        ).joinToString("\n")
                plan shouldContain "Index Cond: (id > '${gammel.id}'::uuid)"
                plan shouldNotContain "temp "
                val rader =
                    session.run(
                        queryOf(kontrollSql)
                            .map {
                                it.long("antall_leste") shouldBe 1L
                                it.uuid("neste_etter_id") shouldBe egen.id
                                it.string("avvikstype") shouldBe "ulik behandlingskjede"
                                it.long("antall_koblinger") shouldBe 1L
                                it.string("referansetype")
                            }.asList,
                    )
                rader.toSet() shouldBe setOf("erstatter", "identitet")
            }
            val rapport = kjorAutomatiskKjedekontroll()
            rapport shouldContain "10003,10003,11"
            rapport
                .substringAfter("AVVIK\n")
                .trim()
                .lines()
                .toSet() shouldBe
                setOf(
                    "referansetype,avvikstype,maal_fjernet,antall_koblinger,antall_behandlinger,antall_kjeder",
                    "erstatter,ulik behandlingskjede,f,1,1,1",
                    "identitet,ulik behandlingskjede,f,1,1,1",
                )
            dbSession.transaction {
                AvklaringRepositoryPostgres(dbSession, KildeRepository(dbSession)).lagreAvklaringer(
                    listOf(flyttet to Avklaring(behandlingFlyttetAvklaring)),
                    this,
                )
                session.run(queryOf("SET LOCAL work_mem = '64kB'").asExecute)
                val eksistensSql = requireNotNull(javaClass.getResource("/sql/finnes-kjedekoblinger-etter-flytting.sql")).readText()
                val listeSql = requireNotNull(javaClass.getResource("/sql/finn-kjedekoblinger-etter-flytting.sql")).readText()
                listOf(eksistensSql, listeSql).forEach { sql ->
                    val plan =
                        session
                            .run(
                                queryOf("EXPLAIN (ANALYZE, BUFFERS) $sql")
                                    .map { it.string("QUERY PLAN") }
                                    .asList,
                            ).joinToString("\n")
                    plan shouldNotContain "Seq Scan on opplysning "
                    plan shouldNotContain "temp "
                }
                session.run(queryOf(eksistensSql).map { it.boolean("finnes_kobling_utenfor_kjeden") }.asSingle) shouldBe true
                session
                    .run(
                        queryOf(listeSql)
                            .map {
                                it.uuid("behandling_id") shouldBe flyttet.behandlingId
                                it.uuid("opplysning_id") shouldBe egen.id
                                it.uuid("maal_opplysning_id") shouldBe gammel.id
                                it.string("koblingstype")
                            }.asList,
                    ).toSet() shouldBe setOf("erstatter", "identitet")
            }
        }
    }

    private fun DBTestContext.kjorAutomatiskKjedekontroll(batchstorrelse: Int = 1000): String {
        val resultat =
            Postgres.kjorPsql(
                this,
                "sql/kontroller-kjedekoblinger.psql",
                mapOf("batchstorrelse" to batchstorrelse.toString()),
            )
        withClue(resultat.stderr) {
            resultat.exitCode shouldBe 0
        }
        return resultat.stdout
    }

    private fun DBTestContext.behandlingRepository(): BehandlingRepositoryPostgres {
        val kildeRepository = KildeRepository(dbSession)
        val opplysningerRepository = OpplysningerRepositoryPostgres(dbSession, kildeRepository)
        opplysningerRepository.lagreOpplysningstyper(opplysningstyper)
        val prosessregister = Prosessregister()
        TestBehandlinger.registrerTestProsesser(prosessregister)
        return BehandlingRepositoryPostgres(
            dbSession,
            opplysningerRepository,
            AvklaringRepositoryPostgres(dbSession, kildeRepository),
            kildeRepository,
            prosessregister,
        )
    }

    private fun DBTestContext.lagredeKjedeIder(): Map<UUID, UUID> =
        dbSession.session { session ->
            session
                .run(
                    queryOf("SELECT behandling_id, behandlingskjede_id FROM behandling")
                        .map { it.uuid("behandling_id") to it.uuid("behandlingskjede_id") }
                        .asList,
                ).toMap()
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

package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.mediator.registrerRegelverk
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.hendelse.Søknadstype
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

/**
 * Performance-tester for BehandlingRepositoryPostgres.
 *
 * Disse testene verifiserer at repository-et håndterer store datamengder,
 * inkludert mange opplysninger per behandling og lange kjeder av behandlinger.
 *
 * Bruker leggTil() med gyldighetsperioder for realistisk testing.
 */
class BehandlingRepositoryPostgresPerformanceTest {
    private val ident = "12345678901"

    // Pre-generer opplysningstyper som kan gjenbrukes
    private val opplysningstyper = genererOpplysningstyper(500)

    @Test
    fun `performance test - lagre og hent behandling med mange opplysninger i lang kjede`() {
        val antallBehandlingerIKjede = 10
        val antallOpplysningerPerBehandling = 100

        withMigratedDb {
            val avklaringRepository = AvklaringRepositoryPostgres()
            val opplysningerRepository = OpplysningerRepositoryPostgres()
            // Registrer forretningsprosesser og opplysningstyper
            registrerRegelverk(opplysningerRepository, opplysningstyper.toSet())
            val behandlingRepository = BehandlingRepositoryPostgres(opplysningerRepository, avklaringRepository)

            val behandlinger = mutableListOf<Behandling>()

            // Lag kjede av behandlinger, hver med mange opplysninger
            val lagreTid =
                measureTimeMillis {
                    var forrigeBehandling: Behandling? = null

                    repeat(antallBehandlingerIKjede) { behandlingNummer ->
                        val søknadId = UUIDv7.ny()
                        val hendelse = lagSøknadHendelse(søknadId)

                        // Start med tomme opplysninger og legg til via leggTil()
                        val opplysninger = Opplysninger()
                        leggTilOpplysninger(opplysninger, behandlingNummer, antallOpplysningerPerBehandling)

                        val behandling =
                            Behandling.rehydrer(
                                behandlingId = UUIDv7.ny(),
                                behandler = hendelse,
                                gjeldendeOpplysninger = opplysninger,
                                basertPå = forrigeBehandling,
                                opprettet = LocalDateTime.now(),
                                tilstand =
                                    if (behandlingNummer < antallBehandlingerIKjede - 1) {
                                        Behandling.TilstandType.Ferdig
                                    } else {
                                        Behandling.TilstandType.UnderBehandling
                                    },
                                sistEndretTilstand = LocalDateTime.now(),
                                avklaringer = emptyList(),
                            )

                        behandlingRepository.lagre(behandling)
                        behandlinger.add(behandling)
                        forrigeBehandling = behandling
                    }
                }

            println("\n=== Performance Resultater ===")
            println("Konfigurasjon:")
            println("  - Antall behandlinger i kjede: $antallBehandlingerIKjede")
            println("  - Antall opplysninger per behandling: $antallOpplysningerPerBehandling")
            println("  - Totalt antall opplysninger: ${antallBehandlingerIKjede * antallOpplysningerPerBehandling}")
            println()
            println("Lagring:")
            println("  - Total tid: ${lagreTid}ms")
            println("  - Gjennomsnitt per behandling: ${lagreTid / antallBehandlingerIKjede}ms")

            // Hent siste behandling (som har hele kjeden som basertPå)
            val sisteBehandling = behandlinger.last()
            var hentetBehandling: Behandling?

            val hentTid =
                measureTimeMillis {
                    hentetBehandling = behandlingRepository.hentBehandling(sisteBehandling.behandlingId)
                }

            println()
            println("Henting av siste behandling:")
            println("  - Tid: ${hentTid}ms")

            // Verifiser at behandlingen ble hentet korrekt
            hentetBehandling.shouldNotBeNull()
            hentetBehandling!!.behandlingId shouldBe sisteBehandling.behandlingId
            hentetBehandling!!.opplysninger().somListe().size shouldBe antallOpplysningerPerBehandling

            // Hent alle behandlinger for å måle gjennomsnittlig hentetid
            var totalHentTid = 0L
            repeat(behandlinger.size) { i ->
                val tid =
                    measureTimeMillis {
                        behandlingRepository.hentBehandling(behandlinger[i].behandlingId)
                    }
                totalHentTid += tid
            }

            println()
            println("Henting av alle behandlinger:")
            println("  - Total tid: ${totalHentTid}ms")
            println("  - Gjennomsnitt per behandling: ${totalHentTid / behandlinger.size}ms")
            println("==============================\n")
        }
    }

    @Test
    fun `performance test - parallelle kjeder med delt rot`() {
        val antallKjeder = 5
        val antallBehandlingerPerKjede = 5
        val antallOpplysningerPerBehandling = 50

        withMigratedDb {
            val avklaringRepository = AvklaringRepositoryPostgres()
            val opplysningerRepository = OpplysningerRepositoryPostgres()
            // Registrer forretningsprosesser og opplysningstyper
            registrerRegelverk(opplysningerRepository, opplysningstyper.toSet())
            val behandlingRepository = BehandlingRepositoryPostgres(opplysningerRepository, avklaringRepository)

            // Lag en rot-behandling som alle kjeder baserer seg på
            val rotSøknadId = UUIDv7.ny()
            val rotHendelse = lagSøknadHendelse(rotSøknadId)
            val rotOpplysninger = Opplysninger()
            leggTilOpplysninger(rotOpplysninger, 0, antallOpplysningerPerBehandling)

            val rotBehandling =
                Behandling.rehydrer(
                    behandlingId = UUIDv7.ny(),
                    behandler = rotHendelse,
                    gjeldendeOpplysninger = rotOpplysninger,
                    opprettet = LocalDateTime.now(),
                    tilstand = Behandling.TilstandType.Ferdig,
                    sistEndretTilstand = LocalDateTime.now(),
                    avklaringer = emptyList(),
                )

            behandlingRepository.lagre(rotBehandling)

            val alleBehandlinger = mutableListOf(rotBehandling)

            val lagreTid =
                measureTimeMillis {
                    repeat(antallKjeder) { kjedeNummer ->
                        var forrigeBehandling: Behandling = rotBehandling

                        repeat(antallBehandlingerPerKjede) { behandlingNummer ->
                            val søknadId = UUIDv7.ny()
                            val hendelse = lagSøknadHendelse(søknadId)
                            val opplysninger = Opplysninger()
                            leggTilOpplysninger(
                                opplysninger,
                                kjedeNummer * 1000 + behandlingNummer,
                                antallOpplysningerPerBehandling,
                            )

                            val behandling =
                                Behandling.rehydrer(
                                    behandlingId = UUIDv7.ny(),
                                    behandler = hendelse,
                                    gjeldendeOpplysninger = opplysninger,
                                    basertPå = forrigeBehandling,
                                    opprettet = LocalDateTime.now(),
                                    tilstand =
                                        if (behandlingNummer < antallBehandlingerPerKjede - 1) {
                                            Behandling.TilstandType.Ferdig
                                        } else {
                                            Behandling.TilstandType.UnderBehandling
                                        },
                                    sistEndretTilstand = LocalDateTime.now(),
                                    avklaringer = emptyList(),
                                )

                            behandlingRepository.lagre(behandling)
                            alleBehandlinger.add(behandling)
                            forrigeBehandling = behandling
                        }
                    }
                }

            println("\n=== Performance Resultater (Parallelle Kjeder) ===")
            println("Konfigurasjon:")
            println("  - Antall kjeder: $antallKjeder")
            println("  - Antall behandlinger per kjede: $antallBehandlingerPerKjede")
            println("  - Antall opplysninger per behandling: $antallOpplysningerPerBehandling")
            println("  - Totalt antall behandlinger: ${alleBehandlinger.size}")
            println(
                "  - Totalt antall opplysninger: ${alleBehandlinger.size * antallOpplysningerPerBehandling}",
            )
            println()
            println("Lagring:")
            println("  - Total tid: ${lagreTid}ms")
            println("  - Gjennomsnitt per behandling: ${lagreTid / alleBehandlinger.size}ms")

            // Hent alle behandlinger
            var totalHentTid = 0L
            alleBehandlinger.forEach { behandling ->
                val tid =
                    measureTimeMillis {
                        behandlingRepository.hentBehandling(behandling.behandlingId).shouldNotBeNull()
                    }
                totalHentTid += tid
            }

            println()
            println("Henting av alle behandlinger:")
            println("  - Total tid: ${totalHentTid}ms")
            println("  - Gjennomsnitt per behandling: ${totalHentTid / alleBehandlinger.size}ms")
            println("=================================================\n")
        }
    }

    private fun lagSøknadHendelse(søknadId: java.util.UUID) =
        SøknadInnsendtHendelse(
            meldingsreferanseId = søknadId,
            ident = ident,
            søknadId = søknadId,
            gjelderDato = LocalDate.now(),
            fagsakId = 1,
            opprettet = LocalDateTime.now(),
            Søknadstype.NySøknad,
        )

    private fun leggTilOpplysninger(
        opplysninger: Opplysninger,
        behandlingNummer: Int,
        antall: Int,
    ) {
        val startDato = LocalDate.now()

        (0 until antall).forEach { i ->
            val index = behandlingNummer * 1000 + i
            val opplysningstype = opplysningstyper[index % opplysningstyper.size]

            // Lag gyldighetsperiode for hver opplysning (forskjellige perioder)
            val gyldighetsperiode =
                Gyldighetsperiode(
                    fraOgMed = startDato.plusDays((i * 7).toLong()),
                    tilOgMed = startDato.plusDays((i * 7 + 14).toLong()),
                )

            val faktum =
                when (opplysningstype.datatype) {
                    is Desimaltall ->
                        Faktum(
                            opplysningstype = opplysningstype as Opplysningstype<Double>,
                            verdi = (i * 1.5),
                            gyldighetsperiode = gyldighetsperiode,
                        )
                    is Boolsk ->
                        Faktum(
                            opplysningstype = opplysningstype as Opplysningstype<Boolean>,
                            verdi = i % 2 == 0,
                            gyldighetsperiode = gyldighetsperiode,
                        )
                    is Heltall ->
                        Faktum(
                            opplysningstype = opplysningstype as Opplysningstype<Int>,
                            verdi = i,
                            gyldighetsperiode = gyldighetsperiode,
                        )
                    is Dato ->
                        Faktum(
                            opplysningstype = opplysningstype as Opplysningstype<LocalDate>,
                            verdi = LocalDate.now().plusDays(i.toLong()),
                            gyldighetsperiode = gyldighetsperiode,
                        )
                    is Tekst ->
                        Faktum(
                            opplysningstype = opplysningstype as Opplysningstype<String>,
                            verdi = "verdi-$index",
                            gyldighetsperiode = gyldighetsperiode,
                        )
                    else ->
                        Faktum(
                            opplysningstype = opplysningstype as Opplysningstype<Double>,
                            verdi = (i * 1.0),
                            gyldighetsperiode = gyldighetsperiode,
                        )
                }

            opplysninger.leggTil(faktum)
        }
    }

    private fun genererOpplysningstyper(antall: Int): List<Opplysningstype<*>> =
        (0 until antall).map { i ->
            when (i % 5) {
                0 ->
                    Opplysningstype.desimaltall(
                        Opplysningstype.Id(UUIDv7.ny(), Desimaltall),
                        "perf-opplysning-desimal-$i",
                    )
                1 ->
                    Opplysningstype.boolsk(
                        Opplysningstype.Id(UUIDv7.ny(), Boolsk),
                        "perf-opplysning-bool-$i",
                    )
                2 ->
                    Opplysningstype.heltall(
                        Opplysningstype.Id(UUIDv7.ny(), Heltall),
                        "perf-opplysning-heltall-$i",
                    )
                3 ->
                    Opplysningstype.dato(
                        Opplysningstype.Id(UUIDv7.ny(), Dato),
                        "perf-opplysning-dato-$i",
                    )
                else ->
                    Opplysningstype.tekst(
                        Opplysningstype.Id(UUIDv7.ny(), Tekst),
                        "perf-opplysning-tekst-$i",
                    )
            }
        }
}

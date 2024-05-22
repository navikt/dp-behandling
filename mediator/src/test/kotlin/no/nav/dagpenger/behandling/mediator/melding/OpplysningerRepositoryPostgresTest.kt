package no.nav.dagpenger.behandling.mediator.melding

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.verdier.Ulid
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.system.measureTimeMillis

class OpplysningerRepositoryPostgresTest {
    @Test
    fun `lagrer enkle opplysninger`() {
        withMigratedDb {
            val repo = OpplysningerRepositoryPostgres()
            val heltallFaktum = Faktum(Opplysningstype.somHeltall("Heltall"), 10)
            val kildeA = Saksbehandlerkilde("foo")
            val boolskFaktum = Faktum(Opplysningstype.somBoolsk("Boolsk"), true, kilde = kildeA)
            val kildeB = Saksbehandlerkilde("bar")
            val datoFaktum = Faktum(Opplysningstype.somDato("Dato"), LocalDate.now(), kilde = kildeB)

            val opplysninger = Opplysninger(listOf(heltallFaktum, boolskFaktum, datoFaktum))
            repo.lagreOpplysninger(opplysninger)

            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.finnAlle().size shouldBe opplysninger.finnAlle().size
            fraDb.finnOpplysning(heltallFaktum.opplysningstype).verdi shouldBe heltallFaktum.verdi
            fraDb.finnOpplysning(boolskFaktum.opplysningstype).verdi shouldBe boolskFaktum.verdi
            fraDb.finnOpplysning(boolskFaktum.opplysningstype).kilde?.id shouldBe kildeA.id
            fraDb.finnOpplysning(datoFaktum.opplysningstype).verdi shouldBe datoFaktum.verdi
            fraDb.finnOpplysning(datoFaktum.opplysningstype).kilde?.id shouldBe kildeB.id
        }
    }

    @Test
    fun `lagre opplysningens gyldighetsperiode`() {
        withMigratedDb {
            val repo = OpplysningerRepositoryPostgres()
            val gyldighetsperiode1 = Gyldighetsperiode(LocalDate.now(), LocalDate.now().plusDays(14))
            val faktum1 = Faktum(Opplysningstype.somHeltall("Fakum1"), 10, gyldighetsperiode1)
            val opplysninger = Opplysninger(listOf(faktum1))
            repo.lagreOpplysninger(opplysninger)
            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.finnOpplysning(faktum1.opplysningstype).gyldighetsperiode shouldBe gyldighetsperiode1
        }
    }

    @Test
    fun `lagrer grenseverdier for dato opplysninger`() {
        withMigratedDb {
            val repo = OpplysningerRepositoryPostgres()
            val kilde = Saksbehandlerkilde("foo")
            val maksDatoFaktum = Faktum(Opplysningstype.somDato("MaksDato"), LocalDate.MAX, kilde = kilde)
            val minDatoFaktum = Faktum(Opplysningstype.somDato("MinDato"), LocalDate.MIN, kilde = kilde)
            val opplysninger = Opplysninger(listOf(maksDatoFaktum, minDatoFaktum))
            repo.lagreOpplysninger(opplysninger)

            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.finnOpplysning(maksDatoFaktum.opplysningstype).verdi shouldBe maksDatoFaktum.verdi
            fraDb.finnOpplysning(minDatoFaktum.opplysningstype).verdi shouldBe minDatoFaktum.verdi
        }
    }

    @Test
    @Disabled("Modellen støtter ikke å bruke opplysninger med samme navn og ulik type")
    fun `lagrer opplysninger med samme navn og ulik type`() {
        withMigratedDb {
            val repo = OpplysningerRepositoryPostgres()
            val opplysningstype = Opplysningstype.somUlid("Ulid")
            val opplysningstype1 = Opplysningstype.somBoolsk("Ulid")

            val ulidFaktum = Faktum(opplysningstype, Ulid("01E5Z6Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1"))
            val ulidBoolskFaktum = Faktum(opplysningstype1, false)

            val opplysninger = Opplysninger(listOf(ulidFaktum, ulidBoolskFaktum))
            repo.lagreOpplysninger(opplysninger)

            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.finnOpplysning(opplysningstype).verdi shouldBe ulidFaktum.verdi
            fraDb.finnOpplysning(opplysningstype1).verdi shouldBe ulidBoolskFaktum.verdi
        }
    }

    @Test
    fun `lagrer opplysninger med utledning`() {
        withMigratedDb {
            val repo = OpplysningerRepositoryPostgres()
            val baseOpplysningstype = Opplysningstype.somDato("Dato")
            val utledetOpplysningstype = Opplysningstype.somHeltall("Utledet")

            val baseOpplysning = Faktum(baseOpplysningstype, LocalDate.now())

            val regelsett = Regelsett("Regelsett") { regel(utledetOpplysningstype) { oppslag(baseOpplysningstype) { 5 } } }
            val opplysninger = Opplysninger().also { Regelkjøring(LocalDate.now(), it, regelsett) }
            opplysninger.leggTil(baseOpplysning)

            repo.lagreOpplysninger(opplysninger)

            val fraDb = repo.hentOpplysninger(opplysninger.id).also { Regelkjøring(LocalDate.now(), it) }
            fraDb.finnAlle().size shouldBe opplysninger.finnAlle().size

            with(fraDb.finnOpplysning(utledetOpplysningstype)) {
                verdi shouldBe 5
                utledetAv.shouldNotBeNull()
                utledetAv!!.regel shouldBe "Oppslag"
                utledetAv!!.opplysninger shouldContainExactly listOf(baseOpplysning)
            }
            with(fraDb.finnOpplysning(baseOpplysning.id)) {
                id shouldBe baseOpplysning.id
                verdi shouldBe baseOpplysning.verdi
                gyldighetsperiode shouldBe baseOpplysning.gyldighetsperiode
                opplysningstype shouldBe baseOpplysning.opplysningstype
                utledetAv.shouldBeNull()
            }
        }
    }

    @Test
    fun `Klarer å lagre store mengder opplysninger effektivt`() {
        withMigratedDb {
            val repo = OpplysningerRepositoryPostgres()
            val fakta = (1..50000).map { Faktum(Opplysningstype.somHeltall("Desimal".id("desitall")), it) }
            val opplysninger = Opplysninger(fakta)

            val tidBrukt = measureTimeMillis { repo.lagreOpplysninger(opplysninger) }
            tidBrukt shouldBeLessThan 5555

            val fraDb = repo.hentOpplysninger(opplysninger.id)
            fraDb.finnAlle().size shouldBe fakta.size
        }
    }

    @Test
    fun `lagre erstattet opplysning i samme Opplysninger`() {
        withMigratedDb {
            val repo = OpplysningerRepositoryPostgres()
            val opplysningstype = Opplysningstype.somHeltall("Heltall")
            val opplysning = Faktum(opplysningstype, 10)
            val opplysningErstattet = Faktum(opplysningstype, 20)
            val opplysninger =
                Opplysninger(listOf(opplysning)).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            repo.lagreOpplysninger(opplysninger)
            opplysninger.leggTil(opplysningErstattet)
            repo.lagreOpplysninger(opplysninger)
            val fraDb =
                repo.hentOpplysninger(opplysninger.id).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            fraDb.aktiveOpplysninger() shouldContainExactly opplysninger.aktiveOpplysninger()
            fraDb.finnOpplysning(opplysningstype).verdi shouldBe opplysningErstattet.verdi
        }
    }

    @Test
    fun `kan erstatte opplysning i tidligere Opplysninger`() {
        withMigratedDb {
            val repo = OpplysningerRepositoryPostgres()

            // Lag opplysninger med opprinnelig opplysning
            val opplysningstype = Opplysningstype.somHeltall("Heltall")
            val opplysning = Faktum(opplysningstype, 10)
            val opprinneligOpplysninger =
                Opplysninger(listOf(opplysning)).also {
                    Regelkjøring(LocalDate.now(), it)
                }
            repo.lagreOpplysninger(opprinneligOpplysninger)

            // Lag ny opplysninger med erstattet opplysning
            val opplysningErstattet = Faktum(opplysningstype, 20)
            val erstattetOpplysninger = Opplysninger(opprinneligOpplysninger).also { Regelkjøring(LocalDate.now(), it) }
            erstattetOpplysninger.leggTil(opplysningErstattet)
            repo.lagreOpplysninger(erstattetOpplysninger)

            // Verifiser
            val fraDb: Opplysninger =
                // Simulerer hvordan Behandling setter opp Opplysninger
                repo.hentOpplysninger(erstattetOpplysninger.id) + repo.hentOpplysninger(opprinneligOpplysninger.id)
            Regelkjøring(LocalDate.now(), fraDb)
            fraDb.aktiveOpplysninger() shouldContainExactly erstattetOpplysninger.aktiveOpplysninger()
            fraDb.finnOpplysning(opplysningstype).verdi shouldBe opplysningErstattet.verdi

            fraDb.finnOpplysning(opplysningstype).erstatter shouldBe opplysning
        }
    }
}

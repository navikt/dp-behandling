package no.nav.dagpenger.behandling.mediator.melding

import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.mediator.repository.OpplysningRepositoryPostgres
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.verdier.Ulid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

class OpplysningRepositoryPostgresTest {
    @Test
    fun `lagre opplysning`() {
        withMigratedDb {
            val repo = OpplysningRepositoryPostgres()
            val heltallFaktum = Faktum(Opplysningstype.somHeltall("Heltall"), 5)
            val desimalFaktum =
                Faktum(
                    Opplysningstype.somDesimaltall("Desimal".id("desitall")),
                    5.5,
                    gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.now()),
                )
            val opplysninger =
                listOf(
                    heltallFaktum,
                    desimalFaktum,
                    Faktum(
                        Opplysningstype.somDato("Dato"),
                        LocalDate.now(),
                        gyldighetsperiode = Gyldighetsperiode(LocalDate.now(), LocalDate.now()),
                    ),
                    Faktum(Opplysningstype.somBoolsk("Boolsk"), true, gyldighetsperiode = Gyldighetsperiode(tom = LocalDateTime.now())),
                    Faktum(Opplysningstype.somUlid("Ulid"), Ulid("01E5Z6Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1")),
                    // Ulike typer kan bruke samme navn
                    Faktum(Opplysningstype.somBoolsk("Ulid"), false),
                )
            repo.lagreOpplysninger(opplysninger).also {
                // Duplikat skriving skal ikke lage duplikate rader
                repo.lagreOpplysninger(opplysninger)
            }

            val inserts = (1..50000).map { Faktum(Opplysningstype.somHeltall("Desimal".id("desitall")), it) }
            val tidBrukt = measureTimeMillis { repo.lagreOpplysninger(inserts) }
            tidBrukt shouldBeLessThan 5000

            with(repo.hentOpplysning(heltallFaktum.id)!!) {
                id shouldBe heltallFaktum.id
                verdi shouldBe heltallFaktum.verdi
                gyldighetsperiode shouldBe heltallFaktum.gyldighetsperiode
                opplysningstype shouldBe heltallFaktum.opplysningstype
            }
            with(repo.hentOpplysning(desimalFaktum.id)!!) {
                id shouldBe desimalFaktum.id
                verdi shouldBe desimalFaktum.verdi
                gyldighetsperiode shouldBe desimalFaktum.gyldighetsperiode
                opplysningstype shouldBe desimalFaktum.opplysningstype
            }
        }
    }
}

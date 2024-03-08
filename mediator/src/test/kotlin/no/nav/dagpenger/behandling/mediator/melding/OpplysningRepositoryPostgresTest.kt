package no.nav.dagpenger.behandling.mediator.melding

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.verdier.Ulid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

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
                )
            val ints =
                opplysninger.map { opplysning ->
                    val a = repo.lagreOpplysning(opplysning)
                    repo.lagreOpplysning(opplysning)
                    a
                }
            ints.sum() shouldBe opplysninger.size

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

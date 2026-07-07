package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forbrukstype
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Tildelingsgrunnlag
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.regel.KvotetellingsSkriver
import no.nav.dagpenger.regel.KvotetellingsVerdi
import no.nav.dagpenger.regel.Kvotetellingsresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class KvotetellingsSkriverTest {
    private val kapasitet = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Kapasitet")
    private val aktiv = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Aktiv")

    @Test
    fun `skriver rettighetskvote`() {
        val kvote =
            KvoteDefinisjon(
                hjemmel = tomHjemmel("Selvbærende kvote"),
                forbrukstype = Forbrukstype.Rettighet,
                tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet),
                tellesNår = Beregning.erSanksjonsdag,
                forbruksteller = Beregning.forbrukt,
                gjenstående = Beregning.gjenståendeDager,
                utløsendeBetingelse = aktiv,
            )
        val resultat =
            Kvotetellingsresultat(
                forbruktTeller =
                    listOf(
                        KvotetellingsVerdi(1, Gyldighetsperiode(6.januar(2025), 6.januar(2025))),
                        KvotetellingsVerdi(2, Gyldighetsperiode(7.januar(2025), 7.januar(2025))),
                    ),
                gjenstående =
                    listOf(
                        KvotetellingsVerdi(2, Gyldighetsperiode(6.januar(2025), 6.januar(2025))),
                        KvotetellingsVerdi(1, Gyldighetsperiode(7.januar(2025), 7.januar(2025))),
                    ),
            )
        val opplysninger = Opplysninger().apply { leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025)))) }

        KvotetellingsSkriver(kvote).skriv(opplysninger, resultat)

        opplysninger.finnAlle(Beregning.forbrukt).last().verdi shouldBe 2
        opplysninger.finnAlle(Beregning.gjenståendeDager).last().verdi shouldBe 1
    }

    @Test
    fun `skriver sanksjonskvote`() {
        val kvote =
            KvoteDefinisjon(
                hjemmel = tomHjemmel("Etterfølgende kvote"),
                tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet),
                tellesNår = Beregning.erSanksjonsdag,
                forbruksteller = Beregning.forbruktSanksjonsdager,
                gjenstående = Beregning.gjenståendeSanksjonsdager,
                forbrukstype = Forbrukstype.Sanksjon,
                utløsendeBetingelse = aktiv,
            )
        val resultat =
            Kvotetellingsresultat(
                forbruktTeller =
                    listOf(
                        KvotetellingsVerdi(1, Gyldighetsperiode(6.januar(2025), 6.januar(2025))),
                        KvotetellingsVerdi(2, Gyldighetsperiode(7.januar(2025), 7.januar(2025))),
                    ),
                gjenstående =
                    listOf(
                        KvotetellingsVerdi(2, Gyldighetsperiode(6.januar(2025), 6.januar(2025))),
                        KvotetellingsVerdi(1, Gyldighetsperiode(7.januar(2025), 7.januar(2025))),
                    ),
            )
        val opplysninger = Opplysninger().apply { leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025)))) }

        KvotetellingsSkriver(kvote).skriv(opplysninger, resultat)

        opplysninger.finnAlle(Beregning.forbruktSanksjonsdager).map { it.verdi } shouldBe listOf(1, 2)
        opplysninger.finnAlle(Beregning.gjenståendeSanksjonsdager).last().verdi shouldBe 1
    }
}

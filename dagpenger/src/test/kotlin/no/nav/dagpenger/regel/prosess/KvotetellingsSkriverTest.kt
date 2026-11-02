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
import no.nav.dagpenger.opplysning.Prosesskontekst
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
                sisteForbruk = Beregning.sisteForbruksdag,
                sisteGjenstående = Beregning.sisteGjenståendeDager,
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
                sisteDagMedForbruk = KvotetellingsVerdi(7.januar(2025), Gyldighetsperiode(7.januar(2025))),
                sisteGjenstående = KvotetellingsVerdi(1, Gyldighetsperiode(7.januar(2025), 7.januar(2025))),
            )
        val opplysninger = Opplysninger().apply { leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025)))) }

        KvotetellingsSkriver(kvote).skriv(Prosesskontekst(opplysninger), resultat)

        opplysninger.finnAlle(Beregning.forbrukt).last().verdi shouldBe 2
        opplysninger.finnAlle(Beregning.gjenståendeDager).last().verdi shouldBe 1
        opplysninger.finnAlle(Beregning.sisteForbruksdag).last().verdi shouldBe 7.januar(2025)
        opplysninger.finnAlle(Beregning.sisteGjenståendeDager).last().verdi shouldBe 1
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
                sisteForbruk = Beregning.sisteSanksjonsdagMedForbruk,
                sisteGjenstående = Beregning.sisteGjenståendeSanksjonsdager,
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
                sisteDagMedForbruk = KvotetellingsVerdi(7.januar(2025), Gyldighetsperiode(7.januar(2025))),
                sisteGjenstående = KvotetellingsVerdi(1, Gyldighetsperiode(7.januar(2025), 7.januar(2025))),
            )
        val opplysninger = Opplysninger().apply { leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025)))) }

        KvotetellingsSkriver(kvote).skriv(Prosesskontekst(opplysninger), resultat)

        opplysninger.finnAlle(Beregning.forbruktSanksjonsdager).map { it.verdi } shouldBe listOf(1, 2)
        opplysninger.finnAlle(Beregning.gjenståendeSanksjonsdager).last().verdi shouldBe 1
        opplysninger.finnAlle(Beregning.sisteSanksjonsdagMedForbruk).last().verdi shouldBe 7.januar(2025)
    }

    private val kvote =
        KvoteDefinisjon(
            hjemmel = tomHjemmel("Betingelse"),
            forbrukstype = Forbrukstype.Rettighet,
            tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet),
            tellesNår = Beregning.erSanksjonsdag,
            forbruksteller = Beregning.forbrukt,
            gjenstående = Beregning.gjenståendeDager,
            sisteForbruk = Beregning.sisteForbruksdag,
            sisteGjenstående = Beregning.sisteGjenståendeDager,
            utløsendeBetingelse = aktiv,
        )

    private fun resultat(gjenstående: Int) =
        Kvotetellingsresultat(
            forbruktTeller = listOf(KvotetellingsVerdi(3 - gjenstående, Gyldighetsperiode(6.januar(2025), 6.januar(2025)))),
            gjenstående = listOf(KvotetellingsVerdi(gjenstående, Gyldighetsperiode(6.januar(2025), 6.januar(2025)))),
            sisteDagMedForbruk = KvotetellingsVerdi(6.januar(2025), Gyldighetsperiode(6.januar(2025))),
            sisteGjenstående = KvotetellingsVerdi(gjenstående, Gyldighetsperiode(6.januar(2025))),
        )

    private fun opplysningerMedKapasitet() = Opplysninger().apply { leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025)))) }

    @Test
    fun `gjentatt skriving med samme resultat er no-op`() {
        val opplysninger = opplysningerMedKapasitet()
        val kontekst = Prosesskontekst(opplysninger)

        KvotetellingsSkriver(kvote).skriv(kontekst, resultat(0))
        val antall = opplysninger.finnAlle(aktiv).size
        KvotetellingsSkriver(kvote).skriv(kontekst, resultat(0))

        antall shouldBe 1
        opplysninger.finnAlle(aktiv).size shouldBe antall
        opplysninger.finnAlle(aktiv).single().verdi shouldBe false
    }

    @Test
    fun `åpner kvoten igjen når den ikke lenger er oppbrukt`() {
        val opplysninger = opplysningerMedKapasitet()
        val kontekst = Prosesskontekst(opplysninger)

        KvotetellingsSkriver(kvote).skriv(kontekst, resultat(0))
        KvotetellingsSkriver(kvote).skriv(kontekst, resultat(1))

        opplysninger.finnAlle(aktiv).last().verdi shouldBe true
    }

    @Test
    fun `åpner kvoten fra batchstart når oppbrukt allerede gjelder ved batchstart`() {
        val opplysninger = opplysningerMedKapasitet()
        opplysninger.leggTil(Faktum(aktiv, false, Gyldighetsperiode(1.januar(2025))))

        KvotetellingsSkriver(kvote).skriv(Prosesskontekst(opplysninger), resultat(1))

        opplysninger.finnOpplysning(aktiv, 6.januar(2025)).verdi shouldBe true
        opplysninger.finnOpplysning(aktiv, 5.januar(2025)).verdi shouldBe false
    }
}

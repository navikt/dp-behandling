package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class KvotetellerTest {
    private val kapasitet = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Kapasitet")
    private val forbruk = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Forbruk")
    private val forbruktTeller = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Forbrukt")
    private val gjenstående = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Gjenstående")

    @Test
    fun `teller forbruk per dag`() {
        val kvoteteller = Kvoteteller(kapasitet, forbruk, forbruktTeller, gjenstående)

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 10, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(7.januar(2025), 7.januar(2025))))
                leggTil(Faktum(forbruk, false, Gyldighetsperiode(8.januar(2025), 8.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(9.januar(2025), 9.januar(2025))))
            }

        kvoteteller.regelkjøringFerdig(Prosesskontekst(opplysninger))

        val forbrukVerdier = opplysninger.finnAlle(forbruktTeller).sortedBy { it.gyldighetsperiode.fraOgMed }
        forbrukVerdier[0].verdi shouldBe 1
        forbrukVerdier[1].verdi shouldBe 2
        forbrukVerdier[2].verdi shouldBe 2 // false dag - ikke inkrementert
        forbrukVerdier[3].verdi shouldBe 3

        val gjenståendeVerdier = opplysninger.finnAlle(gjenstående).sortedBy { it.gyldighetsperiode.fraOgMed }
        gjenståendeVerdier[0].verdi shouldBe 9
        gjenståendeVerdier[1].verdi shouldBe 8
        gjenståendeVerdier[2].verdi shouldBe 8
        gjenståendeVerdier[3].verdi shouldBe 7
    }

    @Test
    fun `bruker utgangspunkt fra forrige periode`() {
        val kvoteteller = Kvoteteller(kapasitet, forbruk, forbruktTeller, gjenstående)

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 10, Gyldighetsperiode(1.januar(2025))))
                // Forrige periode hadde forbrukt 5
                leggTil(Faktum(forbruktTeller, 5, Gyldighetsperiode(3.januar(2025), 3.januar(2025))))
                // Denne periodens dager
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(7.januar(2025), 7.januar(2025))))
            }

        kvoteteller.regelkjøringFerdig(Prosesskontekst(opplysninger))

        val forbrukVerdier = opplysninger.finnAlle(forbruktTeller).sortedBy { it.gyldighetsperiode.fraOgMed }
        // Siste verdi skal være 5 (utgangspunkt) + 2 (nye) = 7
        forbrukVerdier.last().verdi shouldBe 7

        val gjenståendeVerdier = opplysninger.finnAlle(gjenstående).sortedBy { it.gyldighetsperiode.fraOgMed }
        gjenståendeVerdier.last().verdi shouldBe 3
    }

    @Test
    fun `gjør ingenting når kapasitet ikke finnes`() {
        val kvoteteller = Kvoteteller(kapasitet, forbruk, forbruktTeller, gjenstående)

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
            }

        kvoteteller.regelkjøringFerdig(Prosesskontekst(opplysninger))

        opplysninger.finnAlle(forbruktTeller) shouldBe emptyList()
    }

    @Test
    fun `skriver siste dag med forbruk og siste gjenstående`() {
        val sisteDag = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), no.nav.dagpenger.opplysning.Dato), "Siste dag")
        val sisteGjenstående = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Siste gjenstående")

        val kvoteteller =
            Kvoteteller(
                kapasitet,
                forbruk,
                forbruktTeller,
                gjenstående,
                sisteDagMedForbruk = sisteDag,
                sisteGjenstående = sisteGjenstående,
            )

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 10, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
                leggTil(Faktum(forbruk, false, Gyldighetsperiode(7.januar(2025), 7.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(8.januar(2025), 8.januar(2025))))
            }

        kvoteteller.regelkjøringFerdig(Prosesskontekst(opplysninger))

        opplysninger.finnAlle(sisteDag).last().verdi shouldBe 8.januar(2025)
        opplysninger.finnAlle(sisteGjenstående).last().verdi shouldBe 8
    }
}

package no.nav.dagpenger.regel

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
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.sortertEtterIlagtDato
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KvotetellerTest {
    private val kapasitet = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Kapasitet")
    private val kapasitet2 = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Kapasitet 2")
    private val forbruk = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Forbruk")
    private val forbruktTeller = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Forbrukt")
    private val gjenstående = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Gjenstående")
    private val sisteDagMedForbruk =
        Opplysningstype.dato(
            Opplysningstype.Id(UUIDv7.ny(), no.nav.dagpenger.opplysning.Dato),
            "Siste dag med forbruk",
        )
    private val sisteGjenstående = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Siste gjenstående")

    @Test
    fun `teller forbruk per dag`() {
        val kvoteteller = lagKvoteteller()

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 10, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(7.januar(2025), 7.januar(2025))))
                leggTil(Faktum(forbruk, false, Gyldighetsperiode(8.januar(2025), 8.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(9.januar(2025), 9.januar(2025))))
            }

        KvotetellingsSkriver(kvoteteller.definisjon).skriv(opplysninger, kvoteteller.beregn(opplysninger))

        // Kun true-dager skrives — false-dagen (8. jan) gir ingen ny opplysning
        val forbrukVerdier = opplysninger.finnAlle(forbruktTeller).sortedBy { it.gyldighetsperiode.fraOgMed }
        forbrukVerdier.map { it.verdi } shouldBe listOf(1, 2, 3)
        forbrukVerdier.map { it.gyldighetsperiode.fraOgMed } shouldBe
            listOf(6.januar(2025), 7.januar(2025), 9.januar(2025))

        val gjenståendeVerdier = opplysninger.finnAlle(gjenstående).sortedBy { it.gyldighetsperiode.fraOgMed }
        gjenståendeVerdier.map { it.verdi } shouldBe listOf(9, 8, 7)
    }

    @Test
    fun `bruker utgangspunkt fra forrige periode`() {
        val kvoteteller = lagKvoteteller()

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 10, Gyldighetsperiode(1.januar(2025))))
                // Forrige periode hadde forbrukt 5
                leggTil(Faktum(forbruktTeller, 5, Gyldighetsperiode(3.januar(2025), 3.januar(2025))))
                // Denne periodens dager
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(7.januar(2025), 7.januar(2025))))
            }

        KvotetellingsSkriver(kvoteteller.definisjon).skriv(opplysninger, kvoteteller.beregn(opplysninger))

        val forbrukVerdier = opplysninger.finnAlle(forbruktTeller).sortedBy { it.gyldighetsperiode.fraOgMed }
        // Siste verdi skal være 5 (utgangspunkt) + 2 (nye) = 7
        forbrukVerdier.last().verdi shouldBe 7

        val gjenståendeVerdier = opplysninger.finnAlle(gjenstående).sortedBy { it.gyldighetsperiode.fraOgMed }
        gjenståendeVerdier.last().verdi shouldBe 3
    }

    @Test
    fun `kan konfigureres med generell kvotedefinisjon`() {
        val kvoteteller =
            lagKvoteteller(
                hjemmel =
                    folketrygden.hjemmel(
                        4,
                        10,
                        "Sanksjonsperiode ved selvforskyldt arbeidsløshet",
                        "Sanksjonsperiode",
                    ),
                tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet),
                forbrukKriterium = forbruk,
            )

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 2, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
            }

        KvotetellingsSkriver(kvoteteller.definisjon).skriv(opplysninger, kvoteteller.beregn(opplysninger))

        opplysninger.finnAlle(forbruktTeller).last().verdi shouldBe 1
        opplysninger.finnAlle(gjenstående).last().verdi shouldBe 1
    }

    @Test
    fun `kvoteteller er generell og type-uavhengig`() {
        val kvoteteller =
            lagKvoteteller(
                forbrukstype = Forbrukstype.Bortfall,
                forbrukKriterium = forbruk,
            )

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
                leggTil(Faktum(forbruk, false, Gyldighetsperiode(7.januar(2025), 7.januar(2025))))
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(8.januar(2025), 8.januar(2025))))
            }

        KvotetellingsSkriver(kvoteteller.definisjon).skriv(opplysninger, kvoteteller.beregn(opplysninger))

        // False-dag (7. jan) gir ingen opplysning — kun 2 true-dager
        opplysninger.finnAlle(forbruktTeller).map { it.verdi } shouldBe listOf(1, 2)
        opplysninger.finnAlle(gjenstående).map { it.verdi } shouldBe listOf(2, 1)
    }

    @Test
    fun `gjør ingenting når kapasitet ikke finnes`() {
        val kvoteteller = lagKvoteteller()

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(forbruk, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
            }

        KvotetellingsSkriver(kvoteteller.definisjon).skriv(opplysninger, kvoteteller.beregn(opplysninger))

        opplysninger.finnAlle(forbruktTeller) shouldBe emptyList()
    }

    @Test
    fun `skriver siste dag med forbruk og siste gjenstående`() {
        val sisteDag = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), no.nav.dagpenger.opplysning.Dato), "Siste dag")
        val sisteGjenstående = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Siste gjenstående")

        val kvoteteller =
            lagKvoteteller(
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

        KvotetellingsSkriver(kvoteteller.definisjon).skriv(opplysninger, kvoteteller.beregn(opplysninger))

        opplysninger.finnAlle(sisteDag).last().verdi shouldBe 8.januar(2025)
        opplysninger.finnAlle(sisteGjenstående).last().verdi shouldBe 8
    }

    @Test
    fun `bortfallsdager fortsetter fifo fra forrige periode`() {
        val kvoteteller =
            lagKvoteteller(
                hjemmel = tomHjemmel("Tidsbegrenset bortfall"),
                kapasitet = TidsbegrensetBortfall.antallBortfallsdager,
                forbrukstype = Forbrukstype.Bortfall,
                forbrukKriterium = Beregning.erBortfallsdag,
                forbruktTeller = Beregning.forbruktBortfallsdager,
                gjenstående = Beregning.gjenståendeBortfallsdager,
            )

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(TidsbegrensetBortfall.antallBortfallsdager, 5, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(Beregning.forbruktBortfallsdager, 2, Gyldighetsperiode(3.januar(2025), 3.januar(2025))))
                leggTil(Faktum(Beregning.erBortfallsdag, true, Gyldighetsperiode(6.januar(2025), 6.januar(2025))))
                leggTil(Faktum(Beregning.erBortfallsdag, true, Gyldighetsperiode(7.januar(2025), 7.januar(2025))))
                leggTil(Faktum(Beregning.erBortfallsdag, true, Gyldighetsperiode(8.januar(2025), 8.januar(2025))))
            }

        KvotetellingsSkriver(kvoteteller.definisjon).skriv(opplysninger, kvoteteller.beregn(opplysninger))

        val forbruktBortfallsdager =
            opplysninger
                .finnAlle(Beregning.forbruktBortfallsdager)
                .filter { it.gyldighetsperiode.fraOgMed >= 6.januar(2025) }
                .sortedBy { it.gyldighetsperiode.fraOgMed }
        forbruktBortfallsdager.map { it.verdi } shouldBe listOf(3, 4, 5)

        val gjenståendeBortfallsdager =
            opplysninger
                .finnAlle(Beregning.gjenståendeBortfallsdager)
                .sortedBy { it.gyldighetsperiode.fraOgMed }
        gjenståendeBortfallsdager.map { it.verdi } shouldBe listOf(2, 1, 0)
    }

    @Test
    fun `kvoter uten ilagt dato sorteres sist`() {
        val medIlagtDato =
            lagKvoteteller(
                hjemmel = tomHjemmel("Med ilagt dato"),
                kapasitet = kapasitet2,
                tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet2),
            ).definisjon
        val utenIlagtDato =
            lagKvoteteller(
                hjemmel = tomHjemmel("Uten ilagt dato"),
                tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet),
            ).definisjon

        val sortert =
            listOf(utenIlagtDato, medIlagtDato).sortertEtterIlagtDato(
                Opplysninger().apply {
                    leggTil(Faktum(kapasitet2, 2, Gyldighetsperiode(1.januar(2025))))
                },
            )

        sortert.map { it.navn } shouldBe listOf("Med ilagt dato", "Uten ilagt dato")
    }

    private fun lagKvoteteller(
        hjemmel: no.nav.dagpenger.opplysning.Hjemmel =
            folketrygden.hjemmel(
                4,
                10,
                "Sanksjonsperiode ved selvforskyldt arbeidsløshet",
                "Sanksjonsperiode",
            ),
        kapasitet: Opplysningstype<Int> = this.kapasitet,
        forbrukstype: Forbrukstype = Forbrukstype.Rettighet,
        forbrukKriterium: Opplysningstype<Boolean> = forbruk,
        forbruktTeller: Opplysningstype<Int> = this.forbruktTeller,
        gjenstående: Opplysningstype<Int> = this.gjenstående,
        tildelingsgrunnlag: Tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet),
        sisteDagMedForbruk: Opplysningstype<LocalDate> = this.sisteDagMedForbruk,
        sisteGjenstående: Opplysningstype<Int> = this.sisteGjenstående,
    ) = Kvoteteller(
        KvoteDefinisjon(
            hjemmel = hjemmel,
            forbrukstype = forbrukstype,
            tildelingsgrunnlag = tildelingsgrunnlag,
            tellesNår = forbrukKriterium,
            forbruksteller = forbruktTeller,
            gjenstående = gjenstående,
            sisteForbruk = sisteDagMedForbruk,
            sisteGjenstående = sisteGjenstående,
        ),
    )
}

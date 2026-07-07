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
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat
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

    private fun List<LocalDate>.tilBeregningdager() =
        this.map {
            Beregningresultat.Beregningsdag.Forbruksdag(
                dag =
                    no.nav.dagpenger.regel.regelsett.beregning.Arbeidsdag(
                        it,
                        Beløp(10),
                        Timer(0),
                        Timer(0),
                        0.toBigDecimal(),
                    ),
                tilUtbetaling = Beløp(10),
                avviklerSanksjon = false,
            )
        }

    @Test
    fun `teller forbruk per dag`() {
        val kvote = lagKvoteDef()
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 10, Gyldighetsperiode(1.januar(2025))))
            }
        val dager = listOf(6.januar(2025), 7.januar(2025), 9.januar(2025))

        KvotetellingsSkriver(kvote).skriv(
            opplysninger,
            Kvotetelling.tell(
                kvote.tildeltKapasitet(opplysninger),
                kvote.forrigeForbruk(opplysninger, dager.first()),
                dager,
                dager.tilBeregningdager(),
            ),
        )

        val forbrukVerdier = opplysninger.finnAlle(forbruktTeller).sortedBy { it.gyldighetsperiode.fraOgMed }
        forbrukVerdier.map { it.verdi } shouldBe listOf(1, 2, 3)
        forbrukVerdier.map { it.gyldighetsperiode.fraOgMed } shouldBe
            listOf(6.januar(2025), 7.januar(2025), 9.januar(2025))

        val gjenståendeVerdier = opplysninger.finnAlle(gjenstående).sortedBy { it.gyldighetsperiode.fraOgMed }
        gjenståendeVerdier.map { it.verdi } shouldBe listOf(9, 8, 7)
    }

    @Test
    fun `bruker utgangspunkt fra forrige periode`() {
        val kvote = lagKvoteDef()
        val fraOgMed = 6.januar(2025)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 10, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(forbruktTeller, 5, Gyldighetsperiode(3.januar(2025), 3.januar(2025))))
            }
        val dager = listOf(6.januar(2025), 7.januar(2025))

        KvotetellingsSkriver(kvote).skriv(
            opplysninger,
            Kvotetelling.tell(
                kvote.tildeltKapasitet(opplysninger),
                kvote.forrigeForbruk(opplysninger, fraOgMed),
                dager,
                dager.tilBeregningdager(),
            ),
        )

        val forbrukVerdier = opplysninger.finnAlle(forbruktTeller).sortedBy { it.gyldighetsperiode.fraOgMed }
        forbrukVerdier.last().verdi shouldBe 7

        val gjenståendeVerdier = opplysninger.finnAlle(gjenstående).sortedBy { it.gyldighetsperiode.fraOgMed }
        gjenståendeVerdier.last().verdi shouldBe 3
    }

    @Test
    fun `kan konfigureres med generell kvotedefinisjon`() {
        val kvote =
            lagKvoteDef(
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
            }
        val dager = listOf(6.januar(2025))

        KvotetellingsSkriver(kvote).skriv(
            opplysninger,
            Kvotetelling.tell(
                kvote.tildeltKapasitet(opplysninger),
                kvote.forrigeForbruk(opplysninger, dager.first()),
                dager,
                dager.tilBeregningdager(),
            ),
        )

        opplysninger.finnAlle(forbruktTeller).last().verdi shouldBe 1
        opplysninger.finnAlle(gjenstående).last().verdi shouldBe 1
    }

    @Test
    fun `kvoteteller er generell og type-uavhengig`() {
        val kvote = lagKvoteDef(forbrukstype = Forbrukstype.Sanksjon)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025))))
            }
        val dager = listOf(6.januar(2025), 8.januar(2025))

        KvotetellingsSkriver(kvote).skriv(
            opplysninger,
            Kvotetelling.tell(
                kvote.tildeltKapasitet(opplysninger),
                kvote.forrigeForbruk(opplysninger, dager.first()),
                dager,
                dager.tilBeregningdager(),
            ),
        )

        opplysninger.finnAlle(forbruktTeller).map { it.verdi } shouldBe listOf(1, 2)
        opplysninger.finnAlle(gjenstående).map { it.verdi } shouldBe listOf(2, 1)
    }

    @Test
    fun `gjør ingenting når kapasitet ikke finnes`() {
        val kvote = lagKvoteDef()
        val opplysninger = Opplysninger()
        val dager = listOf(6.januar(2025))

        KvotetellingsSkriver(kvote).skriv(
            opplysninger,
            Kvotetelling.tell(
                kvote.tildeltKapasitet(opplysninger),
                kvote.forrigeForbruk(opplysninger, dager.first()),
                dager,
                dager.tilBeregningdager(),
            ),
        )

        opplysninger.finnAlle(forbruktTeller).first().verdi shouldBe 0
    }

    @Test
    fun `bortfallsdager fortsetter fifo fra forrige periode`() {
        val kvote =
            lagKvoteDef(
                hjemmel = tomHjemmel("Tidsbegrenset bortfall"),
                kapasitet = TidsbegrensetBortfall.antallBortfallsdager,
                forbrukstype = Forbrukstype.Sanksjon,
                forbrukKriterium = Beregning.erSanksjonsdag,
                forbruktTeller = Beregning.forbruktBortfallsdager,
                gjenstående = Beregning.gjenståendeBortfallsdager,
            )
        val fraOgMed = 6.januar(2025)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(TidsbegrensetBortfall.antallBortfallsdager, 5, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(Beregning.forbruktBortfallsdager, 2, Gyldighetsperiode(3.januar(2025), 3.januar(2025))))
            }
        val dager = listOf(6.januar(2025), 7.januar(2025), 8.januar(2025))

        KvotetellingsSkriver(kvote).skriv(
            opplysninger,
            Kvotetelling.tell(
                kvote.tildeltKapasitet(opplysninger),
                kvote.forrigeForbruk(opplysninger, fraOgMed),
                dager,
                dager.tilBeregningdager(),
            ),
        )

        val forbruktBortfallsdager =
            opplysninger
                .finnAlle(Beregning.forbruktBortfallsdager)
                .filter { it.gyldighetsperiode.fraOgMed >= fraOgMed }
                .sortedBy { it.gyldighetsperiode.fraOgMed }
        forbruktBortfallsdager.map { it.verdi } shouldBe listOf(3, 4, 5)

        val gjenståendeBortfallsdager =
            opplysninger
                .finnAlle(Beregning.gjenståendeBortfallsdager)
                .sortedBy { it.gyldighetsperiode.fraOgMed }
        gjenståendeBortfallsdager.map { it.verdi } shouldBe listOf(2, 1, 0)
    }

    @Test
    fun `setter utløsende betingelse til false fra dagen etter kvoten er brukt opp`() {
        val betingelse = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Sanksjon aktiv")
        val kvote = lagKvoteDef(forbrukstype = Forbrukstype.Sanksjon, utløsendeBetingelse = betingelse)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 2, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(betingelse, true, Gyldighetsperiode(1.januar(2025))))
            }
        val dager = listOf(6.januar(2025), 7.januar(2025))

        KvotetellingsSkriver(kvote).skriv(
            opplysninger,
            Kvotetelling.tell(
                kvote.tildeltKapasitet(opplysninger),
                kvote.forrigeForbruk(opplysninger, dager.first()),
                dager,
                dager.tilBeregningdager(),
            ),
        )

        opplysninger.finnAlle(gjenstående).last().verdi shouldBe 0
        val betingelseVerdier = opplysninger.finnAlle(betingelse).sortedBy { it.gyldighetsperiode.fraOgMed }
        betingelseVerdier.last().verdi shouldBe false
        betingelseVerdier.last().gyldighetsperiode.fraOgMed shouldBe 8.januar(2025) // dagen etter siste forbruksdag
    }

    @Test
    fun `setter ikke utløsende betingelse til false når kvoten ikke er brukt opp`() {
        val betingelse = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Sanksjon aktiv")
        val kvote = lagKvoteDef(forbrukstype = Forbrukstype.Sanksjon, utløsendeBetingelse = betingelse)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 5, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(betingelse, true, Gyldighetsperiode(1.januar(2025))))
            }
        val dager = listOf(6.januar(2025), 7.januar(2025))

        KvotetellingsSkriver(kvote).skriv(
            opplysninger,
            Kvotetelling.tell(
                kvote.tildeltKapasitet(opplysninger),
                kvote.forrigeForbruk(opplysninger, dager.first()),
                dager,
                dager.tilBeregningdager(),
            ),
        )

        opplysninger.finnAlle(gjenstående).last().verdi shouldBe 3
        // Betingelsen skal ikke endres — kun én verdi (den originale true)
        opplysninger.finnAlle(betingelse).single().verdi shouldBe true
    }

    @Test
    fun `kvoter uten ilagt dato sorteres sist`() {
        val medIlagtDato =
            lagKvoteDef(
                hjemmel = tomHjemmel("Med ilagt dato"),
                kapasitet = kapasitet2,
                tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet2),
            )
        val utenIlagtDato =
            lagKvoteDef(
                hjemmel = tomHjemmel("Uten ilagt dato"),
                tildelingsgrunnlag = Tildelingsgrunnlag(kapasitet),
            )

        val sortert =
            listOf(utenIlagtDato, medIlagtDato).sortertEtterIlagtDato(
                Opplysninger().apply {
                    leggTil(Faktum(kapasitet2, 2, Gyldighetsperiode(1.januar(2025))))
                },
            )

        sortert.map { it.navn } shouldBe listOf("Med ilagt dato", "Uten ilagt dato")
    }

    private fun lagKvoteDef(
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
        utløsendeBetingelse: Opplysningstype<Boolean> = forbruk,
    ) = KvoteDefinisjon(
        hjemmel = hjemmel,
        forbrukstype = forbrukstype,
        tildelingsgrunnlag = tildelingsgrunnlag,
        tellesNår = forbrukKriterium,
        forbruksteller = forbruktTeller,
        gjenstående = gjenstående,
        utløsendeBetingelse = utløsendeBetingelse,
    )
}

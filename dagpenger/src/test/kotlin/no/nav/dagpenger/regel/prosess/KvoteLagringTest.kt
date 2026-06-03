package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forbruksrekkefølge
import no.nav.dagpenger.opplysning.Forbrukstype
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.KvoteKilde
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.regel.KvotetellingsVerdi
import no.nav.dagpenger.regel.Kvotetellingsresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class KvoteLagringTest {
    private val kapasitet = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Kapasitet")

    @Test
    fun `skriver ferdig kvotetelling uten å tolke dager`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025))))
            }
        val kvote =
            KvoteDefinisjon(
                hjemmel = tomHjemmel("Selvbærende kvote"),
                forbrukstype = Forbrukstype.ORDINÆR,
                kilder = listOf(KvoteKilde(kapasitet)),
                forbrukKriterium = no.nav.dagpenger.regel.regelsett.beregning.Beregning.erBortfallsdag,
                forbruktTeller = no.nav.dagpenger.regel.regelsett.beregning.Beregning.forbrukt,
                gjenstående = no.nav.dagpenger.regel.regelsett.beregning.Beregning.gjenståendeDager,
                sisteDagMedForbruk = no.nav.dagpenger.regel.regelsett.beregning.Beregning.sisteForbruksdag,
                sisteGjenstående = no.nav.dagpenger.regel.regelsett.beregning.Beregning.sisteGjenståendeDager,
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

        KvoteLagring().lagre(opplysninger, listOf(KvoteTelling(kvote, resultat)))

        opplysninger.finnAlle(Beregning.forbrukt).last().verdi shouldBe 2
        opplysninger.finnAlle(Beregning.gjenståendeDager).last().verdi shouldBe 1
        opplysninger.finnAlle(Beregning.sisteForbruksdag).last().verdi shouldBe 7.januar(2025)
        opplysninger.finnAlle(Beregning.sisteGjenståendeDager).last().verdi shouldBe 1
    }

    @Test
    fun `skriver etterfølgende sanksjon uten å hente noe fra dager`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(kapasitet, 3, Gyldighetsperiode(1.januar(2025))))
            }
        val kvote =
            KvoteDefinisjon(
                hjemmel = tomHjemmel("Etterfølgende kvote"),
                kilder = listOf(KvoteKilde(kapasitet)),
                forbrukKriterium = no.nav.dagpenger.regel.regelsett.beregning.Beregning.erBortfallsdag,
                forbruktTeller = no.nav.dagpenger.regel.regelsett.beregning.Beregning.forbruktSanksjonsdager,
                gjenstående = no.nav.dagpenger.regel.regelsett.beregning.Beregning.gjenståendeSanksjonsdager,
                sisteDagMedForbruk = no.nav.dagpenger.regel.regelsett.beregning.Beregning.sisteSanksjonsdagMedForbruk,
                sisteGjenstående = no.nav.dagpenger.regel.regelsett.beregning.Beregning.sisteGjenståendeSanksjonsdager,
                forbrukstype = Forbrukstype.BORTFALL,
                forbruksrekkefølge = Forbruksrekkefølge.ETTERFØLGENDE,
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

        KvoteLagring().lagre(opplysninger, listOf(KvoteTelling(kvote, resultat)))

        opplysninger.finnAlle(Beregning.forbruktSanksjonsdager).map { it.verdi } shouldBe listOf(1, 2)
        opplysninger.finnAlle(Beregning.gjenståendeSanksjonsdager).last().verdi shouldBe 1
        opplysninger.finnAlle(Beregning.sisteSanksjonsdagMedForbruk).last().verdi shouldBe 7.januar(2025)
    }
}

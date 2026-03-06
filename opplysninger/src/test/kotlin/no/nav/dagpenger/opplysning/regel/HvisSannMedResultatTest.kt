package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.mai
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HvisSannMedResultatTest {
    private val hvisSannVerdi = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "HvisSannVerdi")
    private val hvisUsannVerdi = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "HvisUsannVerdi")
    private val resultat = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "Resultat")

    private val regelsett =
        vilkår("Test") {
            regel(a) { innhentes }
            regel(hvisSannVerdi) { innhentes }
            regel(hvisUsannVerdi) { innhentes }
            regel(resultat) { hvisSannMedResultat(a, hvisSannVerdi, hvisUsannVerdi) }
        }

    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeEach
    fun setup() {
        regelkjøring = Regelkjøring(23.mai(2024), opplysninger = opplysninger, regelsett)
    }

    @Test
    fun `returnerer hvisSann-verdi når sjekk er true`() {
        opplysninger.leggTil(Faktum(a, true))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 100.0
    }

    @Test
    fun `returnerer hvisUsann-verdi når sjekk er false`() {
        opplysninger.leggTil(Faktum(a, false))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 50.0
    }

    @Test
    fun `re-evaluerer når hvisSann-verdi endres`() {
        opplysninger.leggTil(Faktum(a, true))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 100.0

        // hvisSann endrer seg – resultat skal oppdateres
        opplysninger.leggTil(Faktum(hvisSannVerdi, 200.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 200.0
    }

    @Test
    fun `re-evaluerer når hvisUsann-verdi endres`() {
        opplysninger.leggTil(Faktum(a, false))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 50.0

        // hvisUsann endrer seg – resultat skal oppdateres
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 75.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 75.0
    }

    @Test
    fun `re-evaluerer ikke i loop når verdier er uendret`() {
        opplysninger.leggTil(Faktum(a, true))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        val rapport = regelkjøring.evaluer()

        // Ingen regler skal kjøre på nytt – alt er stabilt
        rapport.kjørteRegler.size shouldBe 1
        opplysninger.finnOpplysning(resultat).verdi shouldBe 100.0
    }
}

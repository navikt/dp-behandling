package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltall
import org.junit.jupiter.api.Test

class OpplysningerViewTest {
    @Test
    fun `view er live - ser nye opplysninger lagt til etter at viewet ble opprettet`() {
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar)))

        val view = opplysninger.forDato(5.januar)
        view.finnOpplysning(desimaltall).verdi shouldBe 1.0

        // Legg til ny opplysning som erstatter den forrige fra 3. januar
        opplysninger.leggTil(Faktum(desimaltall, 2.0, Gyldighetsperiode(3.januar)))

        // Viewet skal se den nye verdien (live, ikke snapshot)
        view.finnOpplysning(desimaltall).verdi shouldBe 2.0
    }

    @Test
    fun `view-kjeding - kunEgne og forDato kombinerer filtre`() {
        val parent = Opplysninger()
        parent.leggTil(Faktum(boolskA, true, Gyldighetsperiode(1.januar)))

        val child = Opplysninger.basertPå(parent)
        child.leggTil(Faktum(boolskA, false, Gyldighetsperiode(10.januar)))

        // kunEgne.forDato skal bare se egne opplysninger filtrert på dato
        val view = child.kunEgne.forDato(15.januar)
        view.har(boolskA) shouldBe true
        view.finnOpplysning(boolskA).verdi shouldBe false

        // forDato.kunEgne skal gi samme resultat
        val view2 = child.forDato(15.januar).kunEgne
        view2.har(boolskA) shouldBe true
        view2.finnOpplysning(boolskA).verdi shouldBe false
    }

    @Test
    fun `kunEgne-view ser ikke arvede opplysninger`() {
        val parent = Opplysninger()
        parent.leggTil(Faktum(desimaltall, 1.0))

        val child = Opplysninger.basertPå(parent)

        child.har(desimaltall) shouldBe true
        child.kunEgne.har(desimaltall) shouldBe false
    }

    @Test
    fun `dato-overload på Opplysninger gir samme resultat som forDato-view`() {
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 10.januar)))
        opplysninger.leggTil(Faktum(desimaltall, 2.0, Gyldighetsperiode(11.januar)))

        opplysninger.finnOpplysning(desimaltall, 5.januar).verdi shouldBe 1.0
        opplysninger.forDato(5.januar).finnOpplysning(desimaltall).verdi shouldBe 1.0

        opplysninger.finnOpplysning(desimaltall, 15.januar).verdi shouldBe 2.0
        opplysninger.forDato(15.januar).finnOpplysning(desimaltall).verdi shouldBe 2.0

        opplysninger.har(desimaltall, 5.januar) shouldBe true
        opplysninger.har(desimaltall, 15.januar) shouldBe true
    }

    @Test
    fun `erSann med dato-overload`() {
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(boolskA, true, Gyldighetsperiode(1.januar, 10.januar)))
        opplysninger.leggTil(Faktum(boolskA, false, Gyldighetsperiode(11.januar)))

        opplysninger.erSann(boolskA, 5.januar) shouldBe true
        opplysninger.erSann(boolskA, 15.januar) shouldBe false
    }

    @Test
    fun `somListe på view filtrerer korrekt`() {
        val parent = Opplysninger()
        parent.leggTil(Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 10.januar)))
        parent.leggTil(Faktum(desimaltall, 2.0, Gyldighetsperiode(11.januar)))

        val child = Opplysninger.basertPå(parent)
        child.leggTil(Faktum(boolskA, true, Gyldighetsperiode(5.januar)))

        // kunEgne skal bare returnere egne opplysninger
        child.kunEgne.somListe().size shouldBe 1
        child.kunEgne
            .somListe()
            .first()
            .verdi shouldBe true

        // forDato skal filtrere på dato
        child.forDato(5.januar).somListe().size shouldBe 2
    }
}

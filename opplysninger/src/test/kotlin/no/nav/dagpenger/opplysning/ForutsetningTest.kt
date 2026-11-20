package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class ForutsetningTest {
    private fun boolsk(navn: String) = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), navn)

    private val utfallA = boolsk("A")
    private val utfallB = boolsk("B")
    private val resultat = boolsk("Resultat")
    private val aktiv = boolsk("Aktiv")

    private val a = vilkår("A") { utfall(utfallA) { innhentes } }
    private val b = vilkår("B") { utfall(utfallB) { innhentes } }

    private val medAlle = fastsettelse(tomHjemmel("Alle")) { forutsetter(a, b) }
    private val medEnAv = fastsettelse(tomHjemmel("EnAv")) { forutsetterEnAv(a, b) }
    private val vurdertA = fastsettelse(tomHjemmel("Vurdert")) { forutsetterVurdert(a) }
    private val medScope =
        fastsettelse(tomHjemmel("Scope")) {
            regel(resultat) { innhentes }
            aktivNår(aktiv)
        }

    private val flaggB = boolsk("FlaggB")
    private val aktivA = boolsk("AktivA")
    private val verdiC = boolsk("VerdiC")
    private val maskererA =
        fastsettelse(tomHjemmel("MaskererA")) {
            regel(aktivA) { innhentes }
            aktivNår(flaggB)
        }
    private val maskererC =
        fastsettelse(tomHjemmel("MaskererC")) {
            regel(verdiC) { innhentes }
            aktivNår(aktivA)
        }

    private fun opplysninger(
        a: Boolean? = null,
        b: Boolean? = null,
    ) = Opplysninger().also { o ->
        a?.let { o.leggTil(Faktum(utfallA, it)) }
        b?.let { o.leggTil(Faktum(utfallB, it)) }
    }

    @Test
    fun `forutsetter krever at alle vilkår er oppfylt og skjuler ingenting`() {
        medAlle.skalKjøres(opplysninger(a = true, b = true)) shouldBe true
        medAlle.skalKjøres(opplysninger(a = true, b = false)) shouldBe false
        medAlle.skalKjøres(opplysninger(a = true)) shouldBe false
        medAlle.aktivOpplysning shouldBe null
    }

    @Test
    fun `forutsetterEnAv krever minst ett oppfylt vilkår`() {
        medEnAv.skalKjøres(opplysninger(a = false, b = true)) shouldBe true
        medEnAv.skalKjøres(opplysninger(a = false, b = false)) shouldBe false
        medEnAv.skalKjøres(opplysninger()) shouldBe false
        medEnAv.aktivOpplysning shouldBe null
    }

    @Test
    fun `forutsetterVurdert krever kun at utfallet finnes`() {
        vurdertA.skalKjøres(opplysninger(a = false)) shouldBe true
        vurdertA.skalKjøres(opplysninger()) shouldBe false
    }

    @Test
    fun `aktivNår skjuler kun i inaktive perioder og beholder aktive`() {
        val o = Opplysninger()
        o.leggTil(Faktum(resultat, true, Gyldighetsperiode(1.januar, 9.januar)))
        o.leggTil(Faktum(resultat, true, Gyldighetsperiode(10.januar, 19.januar)))
        o.leggTil(Faktum(resultat, true, Gyldighetsperiode(20.januar)))
        o.leggTil(Faktum(aktiv, true, Gyldighetsperiode(1.januar, 9.januar)))
        o.leggTil(Faktum(aktiv, false, Gyldighetsperiode(10.januar, 19.januar)))
        o.leggTil(Faktum(aktiv, true, Gyldighetsperiode(20.januar)))

        o.forDato(5.januar).har(resultat) shouldBe true
        o.forDato(15.januar).har(resultat) shouldBe false
        o.forDato(25.januar).har(resultat) shouldBe true
    }

    @Test
    fun `skjuling følger aktiv-perioden når den endres til aktiv igjen eller flyttes`() {
        val o = Opplysninger()
        o.leggTil(Faktum(resultat, true, Gyldighetsperiode(1.januar)))
        o.leggTil(Faktum(aktiv, false, Gyldighetsperiode(10.januar, 19.januar)))
        o.forDato(15.januar).har(resultat) shouldBe false

        // Perioden flyttes: det som ikke lenger er inaktivt skal tilbake, det nye skal skjules
        o.leggTil(Faktum(aktiv, true, Gyldighetsperiode(10.januar, 14.januar)))
        o.leggTil(Faktum(aktiv, false, Gyldighetsperiode(15.januar, 19.januar)))
        o.forDato(12.januar).har(resultat) shouldBe true
        o.forDato(17.januar).har(resultat) shouldBe false

        // Aktiv hele veien: alt er tilbake
        o.leggTil(Faktum(aktiv, true, Gyldighetsperiode(1.januar)))
        o.forDato(17.januar).har(resultat) shouldBe true
    }

    @Test
    fun `aktivNår hindrer også at regelsettet kjører når aktiv ikke er sann`() {
        val o = Opplysninger()
        medScope.skalKjøres(o) shouldBe false
        o.leggTil(Faktum(aktiv, false))
        medScope.skalKjøres(o) shouldBe false
        o.leggTil(Faktum(aktiv, true, Gyldighetsperiode(5.januar)))
        medScope.skalKjøres(o.forDato(6.januar)) shouldBe true
    }

    @Test
    fun `maskering oppdateres gjennom kjeden når en maskert aktiv-opplysning endres`() {
        maskererA.aktivOpplysning shouldBe flaggB
        maskererC.aktivOpplysning shouldBe aktivA

        val o = Opplysninger()
        o.leggTil(Faktum(verdiC, true))
        o.leggTil(Faktum(aktivA, false))
        o.leggTil(Faktum(flaggB, true))
        o.har(verdiC) shouldBe false

        // aktivA skjules nå, og uten aktiv-opplysning skjuler ikke lenger noe verdiC
        o.leggTil(Faktum(flaggB, false))
        o.har(aktivA) shouldBe false
        o.har(verdiC) shouldBe true
    }
}

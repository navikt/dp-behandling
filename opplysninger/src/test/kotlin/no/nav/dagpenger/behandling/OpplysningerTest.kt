package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertEquals

class OpplysningerTest {
    @Test
    fun `vilkår er avhengig av andre vilkår`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")
        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        val alder = Opplysningstype("Alder", vilkår)

        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(1.mai, opplysninger)

        opplysninger.leggTil(Faktum(minsteinntekt, true))
        opplysninger.leggTil(Faktum(alder, true))
        opplysninger.leggTil(Faktum(vilkår, true))

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.har(alder))
        assertTrue(opplysninger.har(vilkår))
    }

    @Test
    fun `tillater ikke overlappende opplysninger av samme type`() {
        val opplysningstype = Opplysningstype<Double>("Type")
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(1.mai, opplysninger)

        opplysninger.leggTil(Faktum(opplysningstype, 0.5, Gyldighetsperiode(1.mai, 10.mai)))
        assertThrows<IllegalArgumentException> {
            opplysninger.leggTil(Faktum(opplysningstype, 0.5))
        }
        opplysninger.leggTil(Faktum(opplysningstype, 1.5, Gyldighetsperiode(11.mai)))

        assertEquals(0.5, opplysninger.finnOpplysning(opplysningstype).verdi)
        assertEquals(0.5, opplysninger.finnOpplysning(opplysningstype).verdi)
        Regelkjøring(15.mai, opplysninger) // Bytt til 15. mai for regelkjøringen
        assertEquals(1.5, opplysninger.finnOpplysning(opplysningstype).verdi)
    }

    @Test
    fun `erstatte opplysning som overlapper helt`() {
        val virkningsdato = Opplysningstype<LocalDate>("Virkningsdato")
        val opplysninger =
            Opplysninger(
                listOf(
                    // Setter opp opplysninger med ting som er kjent fra før
                    // Har er ikke lengre gyldig og må hentes på nytt
                    Faktum(virkningsdato, 1.februar),
                ),
            )

        with(Regelkjøring(1.februar, opplysninger)) {
            assertEquals(1.februar, opplysninger.finnOpplysning(virkningsdato).verdi)
        }

        opplysninger.erstatt(Faktum(virkningsdato, 10.februar, Gyldighetsperiode(10.februar, 28.februar)))
        assertEquals(3, opplysninger.finnAlle().size)

        with(Regelkjøring(15.februar, opplysninger)) {
            assertEquals(10.februar, opplysninger.finnOpplysning(virkningsdato).verdi)
        }
        with(Regelkjøring(28.februar, opplysninger)) {
            assertEquals(10.februar, opplysninger.finnOpplysning(virkningsdato).verdi)
        }
        with(Regelkjøring(1.mars, opplysninger)) {
            assertEquals(1.februar, opplysninger.finnOpplysning(virkningsdato).verdi)
        }
        with(Regelkjøring(1.mars, opplysninger)) {
            assertEquals(1.februar, opplysninger.finnOpplysning(virkningsdato).verdi)
        }
    }
}

package no.nav.dagpenger.opplysning

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpplysningstypeTest {
    @Test
    fun `likhet test`() {
        val fødselsdato = Opplysningstype.somDato("Fødselsdato")
        fødselsdato shouldBeEqual fødselsdato
        fødselsdato.hashCode() shouldBeEqual fødselsdato.hashCode()
        Opplysningstype.somDato("Fødselsdato") shouldBeEqual fødselsdato
        fødselsdato shouldNotBeEqual Any()
    }

    @Test
    fun `enkle opplysningstyper`() {
        val fødselsdato = Opplysningstype.somDato("Fødselsdato")
        assertTrue(fødselsdato.er(fødselsdato))
    }

    @Test
    fun `hierarkiske opplysningstyper`() {
        val vilkår = Opplysningstype.somDato("Vilkår")
        val minsteinntekt = Opplysningstype.somDato("Minsteinntekt", vilkår)

        assertTrue(minsteinntekt.er(minsteinntekt))
        assertTrue(minsteinntekt.er(vilkår))

//        assertEquals(listOf(minsteinntekt), vilkår.bestårAv())
    }
}

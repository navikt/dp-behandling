package no.nav.dagpenger.behandling.konklusjon

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KonklusjonsresultatStrategiTest {
    companion object {
        val opplysningstype = Opplysningstype.somBoolsk("KonklusjonsStrategiTest")
    }

    private enum class TestKonklusjon(
        override val årsak: String,
    ) : Konklusjon {
        KonklusjonsStrategiTest("KonklusjonsStrategiTest"),
    }

    private val konklusjonsStrategi =
        KonklusjonsStrategi(
            årsak = TestKonklusjon.KonklusjonsStrategiTest,
        ) { opplysninger ->
            if (opplysninger.mangler(opplysningstype)) return@KonklusjonsStrategi KonklusjonsSjekk.Resultat.IkkeKonkludert
            if (opplysninger.finnOpplysning(opplysningstype).verdi) {
                return@KonklusjonsStrategi KonklusjonsSjekk.Resultat.Konkludert
            } else {
                KonklusjonsSjekk.Resultat.IkkeKonkludert
            }
        }

    @Test
    fun `teste konklusjonsstrategi`() {
        val opplysninger =
            Opplysninger().also {
                Regelkjøring(LocalDate.now(), it)
            }

        val konklusjon1 = konklusjonsStrategi.evaluer(opplysninger)
        // Har ikke opplysning og kan ikke konkludere
        assertNull(konklusjon1)

        // Har opplysning og kan konkludere
        opplysninger.leggTil(Faktum(opplysningstype, true))
        val konklusjon2 = konklusjonsStrategi.evaluer(opplysninger)
        assertEquals(TestKonklusjon.KonklusjonsStrategiTest.årsak, konklusjon2?.årsak)

        // Har  opplysning og men strategi kan ikke konkludere fordi opplysning er false
        opplysninger.leggTil(Faktum(opplysningstype, false))
        val konklusjon3 = konklusjonsStrategi.evaluer(opplysninger)
        assertNull(konklusjon3)
    }
}

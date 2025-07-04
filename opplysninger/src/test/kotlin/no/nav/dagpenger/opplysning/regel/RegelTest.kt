package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegelTest {
    @Test
    fun `kaster feil når opplysninger mangler`() {
        val a = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "a")
        val b = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "b")
        val c = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "c")
        val regelsett =
            vilkår("test") {
                regel(a) { innhentMed(b, c) }
            }

        val exception: IllegalArgumentException =
            assertThrows<IllegalArgumentException> {
                regelsett.regler().first().lagProdukt(Opplysninger.med(Faktum(b, false)))
            }

        exception.message shouldBe
            """
            |Prøver å kjøre Ekstern(a), men mangler avhengigheter.
            |Det er mismatch mellom lagPlan() og lagProdukt().
            |- Avhengigheter vi mangler: c
            |- Avhengigheter vi trenger: b, c
            |- Avhengigheter vi fant: b
            """.trimMargin()
    }
}

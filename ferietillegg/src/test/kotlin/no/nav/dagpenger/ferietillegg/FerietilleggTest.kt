package no.nav.dagpenger.ferietillegg

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg.FerietilleggKontroll
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg.harKravpåFerietillegg
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.IKontrollpunkt.Kontrollresultat.KreverAvklaring
import no.nav.dagpenger.opplysning.IKontrollpunkt.Kontrollresultat.OK
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import org.junit.jupiter.api.Test

class FerietilleggTest {
    @Test
    fun `Hvis ingen ferietillegg trengs ingen avklaring`() {
        FerietilleggKontroll
            .evaluer(
                opplysninger(),
            ) shouldBe OK
    }

    @Test
    fun `Hvis ferietillegg er false trengs ingen avklaring`() {
        FerietilleggKontroll
            .evaluer(
                opplysninger(
                    Faktum(harKravpåFerietillegg, false),
                ),
            ) shouldBe OK
    }

    @Test
    fun `Hvis man har ferietillegg trengs det avklaring`() {
        FerietilleggKontroll
            .evaluer(
                opplysninger(
                    Faktum(harKravpåFerietillegg, true),
                ),
            ).shouldBeInstanceOf<KreverAvklaring>()
    }

    private fun opplysninger(vararg opplysning: Opplysning<*>) = opplysning.asList().somOpplysninger()
}

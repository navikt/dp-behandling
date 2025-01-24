package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.Minsteinntekt.regelsett
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import org.junit.jupiter.api.Test

class MinsteinntektTest {
    @Test
    fun `valider oppførsel til hvisRelevant`() {
        // Avslag på alder skal ikke prøve inntekt
        regelsett.erRelevant(opplysninger(alder = false, inntekt = false, verneplikt = false)) shouldBe false

        // Oppfylt alder skal prøve inntekt
        regelsett.erRelevant(opplysninger(alder = true, inntekt = false, verneplikt = false)) shouldBe true

        // Fortsatt med med inntekt
        regelsett.erRelevant(opplysninger(alder = true, inntekt = true, verneplikt = false)) shouldBe true

        // Fortsatt med med verneplikt
        regelsett.erRelevant(opplysninger(alder = true, inntekt = true, verneplikt = true)) shouldBe true

        // Bare verneplikt skal minsteinntekt ikke med
        regelsett.erRelevant(opplysninger(alder = true, inntekt = false, verneplikt = true)) shouldBe false
    }

    private fun opplysninger(
        alder: Boolean,
        inntekt: Boolean,
        verneplikt: Boolean,
    ): Opplysninger =
        Opplysninger(
            listOf(
                Faktum(kravTilAlder, alder),
                Faktum(minsteinntekt, inntekt),
                Faktum(oppfyllerKravetTilVerneplikt, verneplikt),
            ),
        )
}

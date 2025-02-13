package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.Minsteinntekt.regelsett
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import org.junit.jupiter.api.Test

class VernepliktTest {
    @Test
    fun `valider oppførsel til erRelevant`() {
        // Ikke søkt verneplikt
        regelsett.påvirkerResultat(opplysninger(false, false, false)) shouldBe false

        // Søkt verneplikt, men ikke oppfyller + minsteinntekt er gunstigst
        regelsett.påvirkerResultat(opplysninger(true, false, false)) shouldBe false

        // Søkt verneplikt, oppfyller + minsteinntekt er gunstigst
        regelsett.påvirkerResultat(opplysninger(true, true, false)) shouldBe false

        // Søkt verneplikt, oppfyller og er gunstigst
        regelsett.påvirkerResultat(opplysninger(true, true, true)) shouldBe true
    }

    private fun opplysninger(
        avtjentVerneplikt: Boolean,
        oppfyllerKravTilVerneplikt: Boolean,
        vernepliktErBest: Boolean,
    ): Opplysninger =
        Opplysninger(
            listOf(
                Faktum(Verneplikt.avtjentVerneplikt, avtjentVerneplikt),
                Faktum(oppfyllerKravetTilVerneplikt, oppfyllerKravTilVerneplikt),
                Faktum(grunnlagForVernepliktErGunstigst, vernepliktErBest),
            ),
        )
}

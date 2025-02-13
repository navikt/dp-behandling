package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.Verneplikt.regelsett
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import org.junit.jupiter.api.Test

class VernepliktTest {
    @Test
    fun `valider oppførsel til erRelevant`() {
        // Ikke søkt verneplikt, ikke relevant
        regelsett.påvirkerResultat(opplysninger(false, false, false, false)) shouldBe false

        // Søkt verneplikt, både verneplikt og minsteinntekt ikke oppfylt, relevant
        regelsett.påvirkerResultat(opplysninger(true, false, false, false)) shouldBe true

        // Søkt verneplikt og er best, relevant
        regelsett.påvirkerResultat(opplysninger(false, true, true, false)) shouldBe true

        // Oppfyller begge, verneplikt er best, relevant
        regelsett.påvirkerResultat(opplysninger(false, true, true, true)) shouldBe true

        // Oppfyller begge, minsteinntekt er best, ikke relevant
        regelsett.påvirkerResultat(opplysninger(false, true, false, true)) shouldBe false

        // Oppfyller ikke verneplikt, minsteinntekt er oppfylt, ikke relevant
        regelsett.påvirkerResultat(opplysninger(false, false, false, true)) shouldBe false
    }

    private fun opplysninger(
        søktOmVerneplikt: Boolean,
        oppfyllerKravetTilVerneplikt: Boolean,
        vernepliktErBest: Boolean,
        minsteinntekt: Boolean,
    ): Opplysninger =
        Opplysninger(
            listOf(
                Faktum(Verneplikt.avtjentVerneplikt, søktOmVerneplikt),
                Faktum(Verneplikt.oppfyllerKravetTilVerneplikt, oppfyllerKravetTilVerneplikt),
                Faktum(grunnlagForVernepliktErGunstigst, vernepliktErBest),
                Faktum(Minsteinntekt.minsteinntekt, minsteinntekt),
            ),
        )
}

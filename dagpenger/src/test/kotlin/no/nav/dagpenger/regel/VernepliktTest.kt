package no.nav.dagpenger.regel
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.regelsett.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.regelsett
import org.junit.jupiter.api.Test

class VernepliktTest {
    @Test
    fun `valider oppførsel til erRelevant`() {
        // Happy path: Ikke verneplikt, kun minsteinntekt, ikke relevant
        regelsett.erRelevantForResultatet(opplysninger(false, false, false, false, true)) shouldBe false

        // Ikke søkt verneplikt, ikke relevant
        regelsett.erRelevantForResultatet(opplysninger(false, false, false, false, false)) shouldBe false

        // Har søkt om verneplikt, men saksbehandler vurderer ikke verneplikt, ikke relevant
        regelsett.erRelevantForResultatet(opplysninger(false, true, false, false, false)) shouldBe false

        // Søkt verneplikt, både verneplikt og minsteinntekt ikke oppfylt, relevant
        regelsett.erRelevantForResultatet(opplysninger(true, true, false, false, false)) shouldBe true

        // Søkt verneplikt og er best, relevant
        regelsett.erRelevantForResultatet(opplysninger(true, false, true, true, false)) shouldBe true

        // Oppfyller begge, verneplikt er best, relevant
        regelsett.erRelevantForResultatet(opplysninger(true, false, true, true, true)) shouldBe true

        // Oppfyller begge, minsteinntekt er best, ikke relevant
        regelsett.erRelevantForResultatet(opplysninger(true, false, true, false, true)) shouldBe false

        // Oppfyller ikke verneplikt, minsteinntekt er oppfylt, ikke relevant
        regelsett.erRelevantForResultatet(opplysninger(true, false, false, false, true)) shouldBe false
    }

    private fun opplysninger(
        skalVernepliktVurderes: Boolean,
        søktOmVerneplikt: Boolean,
        oppfyllerKravetTilVerneplikt: Boolean,
        vernepliktErBest: Boolean,
        minsteinntekt: Boolean,
    ): Opplysninger =
        Opplysninger.med(
            // kravTilAlder = true fordi denne testen verifiserer erRelevantForResultatet sin egen
            // interne logikk (skalVernepliktVurderes m.m.), ikke regelsettets skalKjøres-gate.
            Faktum(kravTilAlder, true),
            Faktum(Rettighetstype.skalVernepliktVurderes, skalVernepliktVurderes),
            Faktum(Verneplikt.avtjentVerneplikt, søktOmVerneplikt),
            Faktum(Verneplikt.oppfyllerKravetTilVerneplikt, oppfyllerKravetTilVerneplikt),
            Faktum(grunnlagForVernepliktErGunstigst, vernepliktErBest),
            Faktum(Minsteinntekt.minsteinntekt, minsteinntekt),
        )
}

package no.nav.dagpenger.regel
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering.regelsett
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import org.junit.jupiter.api.Test

class EtableringTest {
    @Test
    fun `skal kun kjøres når skalEtableringVurderes er sann`() {
        regelsett.skalKjøres(opplysninger(skalEtableringVurderes = true, påvirkerUtfallet = false)) shouldBe true
        regelsett.skalKjøres(opplysninger(skalEtableringVurderes = false, påvirkerUtfallet = false)) shouldBe false
    }

    @Test
    fun `valider oppførsel til påvirkerResultat`() {
        // Etablering er vurdert og påvirker utfallet, relevant
        regelsett.påvirkerResultat(opplysninger(skalEtableringVurderes = true, påvirkerUtfallet = true)) shouldBe true

        // Etablering er vurdert, men påvirker ikke utfallet, ikke relevant
        regelsett.påvirkerResultat(opplysninger(skalEtableringVurderes = true, påvirkerUtfallet = false)) shouldBe false

        // Etablering skal ikke vurderes, og påvirker dermed ikke utfallet, ikke relevant
        regelsett.påvirkerResultat(opplysninger(skalEtableringVurderes = false, påvirkerUtfallet = false)) shouldBe false
    }

    // NB: `somUtgangspunkt`-standardverdiene i Etablering.regelsett (nyVirksomhet, selvforsørget,
    // godkjentNæringsfaglig, ikkeSelvforskyldtArbeidsledig, sluttDato) kan ikke enhetstestes isolert her.
    // Regel.kjør(...) er internal i opplysninger-modulen og krever at hele regelmotoren/DAG-en kjøres
    // for å produsere disse verdiene, slik andre vilkår-regelsett-tester i dette prosjektet heller
    // ikke gjør.

    private fun opplysninger(
        skalEtableringVurderes: Boolean,
        påvirkerUtfallet: Boolean,
    ): Opplysninger =
        Opplysninger.med(
            Faktum(Rettighetstype.skalEtableringVurderes, skalEtableringVurderes),
            Faktum(Etablering.påvirkerUtfallet, påvirkerUtfallet),
        )
}

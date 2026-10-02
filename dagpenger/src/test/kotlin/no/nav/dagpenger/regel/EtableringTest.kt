package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.RettighetsperiodeStrategi
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering.regelsett
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EtableringTest {
    @Test
    fun `etablering bruker sluttdatostrategi mens utdanning beholder ordinær vilkårsvurdering`() {
        regelsett.rettighetsperiodeStrategi shouldBe RettighetsperiodeStrategi.SettTilOgMedVedOppfylt
        Utdanning.regelsett.rettighetsperiodeStrategi shouldBe RettighetsperiodeStrategi.VilkårForRett
    }

    @Test
    fun `skal kun kjøres når skalEtableringVurderes er sann`() {
        regelsett.skalKjøres(opplysninger(skalEtableringVurderes = true)) shouldBe true
        regelsett.skalKjøres(opplysninger(skalEtableringVurderes = false)) shouldBe false
    }

    @Test
    fun `er relevant bare når etablering skal vurderes`() {
        regelsett.påvirkerResultat(opplysninger(skalEtableringVurderes = true)) shouldBe true
        regelsett.påvirkerResultat(opplysninger(skalEtableringVurderes = false)) shouldBe false
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `etableringsutfallet bruker sluttdato og oppdateres når den endres`(godkjent: Boolean) {
        val prøvingsdato = 15.januar(2025)
        val opplysninger =
            opplysninger(skalEtableringVurderes = true).apply {
                leggTil(Faktum(Etablering.nyVirksomhet, godkjent))
                leggTil(Faktum(Etablering.selvforsørget, true))
                leggTil(Faktum(Etablering.godkjentNæringsfaglig, true))
                leggTil(Faktum(Etablering.egenVirksomhet, true))
                leggTil(Faktum(Etablering.overFemtiProsentEierandel, true))
                leggTil(Faktum(Etablering.ikkeSelvforskyldtArbeidsledig, true))
                leggTil(Faktum(Etablering.sluttDato, 31.januar(2025)))
            }

        Regelkjøring(prøvingsdato, opplysninger, regelsett).evaluer()

        with(opplysninger.finnOpplysning(Etablering.etableringGodkjent)) {
            verdi shouldBe godkjent
            gyldighetsperiode shouldBe Gyldighetsperiode(prøvingsdato, 31.januar(2025))
        }

        opplysninger.leggTil(Faktum(Etablering.sluttDato, 20.januar(2025)))
        Regelkjøring(prøvingsdato, opplysninger, regelsett).evaluer()

        with(opplysninger.finnOpplysning(Etablering.etableringGodkjent)) {
            verdi shouldBe godkjent
            gyldighetsperiode shouldBe Gyldighetsperiode(prøvingsdato, 20.januar(2025))
        }
    }

    @Test
    fun `negativt etableringsutfall stopper ikke fastsetting når øvrige vilkår er oppfylt`() {
        val opplysninger =
            opplysninger(skalEtableringVurderes = true).apply {
                RegelverkDagpenger.vilkårsopplysninger.forEach { leggTil(Faktum(it, true)) }
                leggTil(Faktum(Etablering.etableringGodkjent, false))
            }

        kravPåDagpenger(opplysninger) shouldBe true

        opplysninger.leggTil(Faktum(Alderskrav.kravTilAlder, false))

        kravPåDagpenger(opplysninger) shouldBe false
    }

    private fun opplysninger(skalEtableringVurderes: Boolean): Opplysninger =
        Opplysninger.med(Faktum(Rettighetstype.skalEtableringVurderes, skalEtableringVurderes))
}

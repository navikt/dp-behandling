package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.dato.mars
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Utfall
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import kotlin.test.Test

class DagpengerUtfallTest {
    @Test
    fun `uavklart når ingen perioder finnes`() {
        val opplysninger = Opplysninger()
        RegelverkDagpenger.utfall(opplysninger) shouldBe Utfall.Uavklart
    }

    @Test
    fun `innvilgelse når ny kjede har rett`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024))))
            }
        val utfall = RegelverkDagpenger.utfall(opplysninger)
        utfall.shouldBeInstanceOf<Utfall.Innvilgelse>()
        utfall.perioder.size shouldBe 1
        utfall.perioder.first().harRett shouldBe true
    }

    @Test
    fun `innvilgelse med flere perioder der minst én har rett`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024), 31.januar(2024))))
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.mars(2024))))
            }
        val utfall = RegelverkDagpenger.utfall(opplysninger)
        utfall.shouldBeInstanceOf<Utfall.Innvilgelse>()
    }

    @Test
    fun `avslag når ny kjede ikke har rett`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024))))
            }
        RegelverkDagpenger.utfall(opplysninger) shouldBe Utfall.Avslag
    }

    @Test
    fun `avslag når ny kjede har flere perioder uten rett`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024), 31.januar(2024))))
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.mars(2024))))
            }
        RegelverkDagpenger.utfall(opplysninger) shouldBe Utfall.Avslag
    }

    @Test
    fun `endring når arvet rett fortsetter uten nye perioder`() {
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024))))
            }
        val opplysninger = Opplysninger.basertPå(forrige)

        val utfall = RegelverkDagpenger.utfall(opplysninger)
        utfall.shouldBeInstanceOf<Utfall.Endring>()
    }

    @Test
    fun `stans når arvet rett går til avslag`() {
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024))))
            }
        val opplysninger =
            Opplysninger.basertPå(forrige).apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.mars(2024))))
            }

        val utfall = RegelverkDagpenger.utfall(opplysninger)
        utfall.shouldBeInstanceOf<Utfall.Stans>()
    }

    @Test
    fun `gjenopptak når arvet avslag går til innvilget`() {
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024))))
            }
        val opplysninger =
            Opplysninger.basertPå(forrige).apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.mars(2024))))
            }

        val utfall = RegelverkDagpenger.utfall(opplysninger)
        utfall.shouldBeInstanceOf<Utfall.Gjenopptak>()
    }

    @Test
    fun `avslag når arvet avslag og nye perioder heller ikke har rett`() {
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024))))
            }
        val opplysninger =
            Opplysninger.basertPå(forrige).apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.mars(2024))))
            }

        RegelverkDagpenger.utfall(opplysninger) shouldBe Utfall.Avslag
    }

    @Test
    fun `etter stans gir neste behandling endring`() {
        // Første behandling: innvilget
        val første =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024))))
            }
        // Andre behandling: stans (legger til avslag-periode)
        val andre =
            Opplysninger.basertPå(første).apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.mars(2024))))
            }
        // Verifiser at stans er korrekt
        RegelverkDagpenger.utfall(andre).shouldBeInstanceOf<Utfall.Stans>()

        // Tredje behandling (meldekort): arver alt, legger ikke til nye perioder
        val tredje = Opplysninger.basertPå(andre)

        // Ingen nye perioder = ingen endring i rett = ENDRING
        RegelverkDagpenger.utfall(tredje).shouldBeInstanceOf<Utfall.Endring>()
    }
}

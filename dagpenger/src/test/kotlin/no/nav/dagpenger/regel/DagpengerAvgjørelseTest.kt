package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.dato.mars
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import kotlin.test.Test

class DagpengerAvgjørelseTest {
    @Test
    fun `uavklart når ingen perioder finnes`() {
        val opplysninger = Opplysninger()
        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Uavklart
    }

    @Test
    fun `innvilgelse når ny kjede har rett`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024))))
            }
        val avgjørelse = RegelverkDagpenger.avgjørelse(opplysninger)
        avgjørelse.shouldBeInstanceOf<Avgjørelse.Innvilgelse>()
    }

    @Test
    fun `innvilgelse med flere perioder der minst én har rett`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024), 31.januar(2024))))
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.mars(2024))))
            }
        val avgjørelse = RegelverkDagpenger.avgjørelse(opplysninger)
        avgjørelse.shouldBeInstanceOf<Avgjørelse.Innvilgelse>()
    }

    @Test
    fun `avslag når ny kjede ikke har rett`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024))))
            }
        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Avslag
    }

    @Test
    fun `avslag når ny kjede har flere perioder uten rett`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024), 31.januar(2024))))
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.mars(2024))))
            }
        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Avslag
    }

    @Test
    fun `endring når arvet rett fortsetter uten nye perioder`() {
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024))))
            }
        val opplysninger = Opplysninger.basertPå(forrige)

        val avgjørelse = RegelverkDagpenger.avgjørelse(opplysninger)
        avgjørelse.shouldBeInstanceOf<Avgjørelse.Endring>()
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

        val avgjørelse = RegelverkDagpenger.avgjørelse(opplysninger)
        avgjørelse.shouldBeInstanceOf<Avgjørelse.Stans>()
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

        val avgjørelse = RegelverkDagpenger.avgjørelse(opplysninger)
        avgjørelse.shouldBeInstanceOf<Avgjørelse.Gjenopptak>()
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

        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Avslag
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
        RegelverkDagpenger.avgjørelse(andre).shouldBeInstanceOf<Avgjørelse.Stans>()

        // Tredje behandling (meldekort): arver alt, legger ikke til nye perioder
        val tredje = Opplysninger.basertPå(andre)

        // Siste arvede periode har ikke rett, ingen nye perioder = avslag
        RegelverkDagpenger.avgjørelse(tredje) shouldBe Avgjørelse.Endring
    }
}

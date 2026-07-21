package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.dato.februar
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.dato.mars
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalGjenopptakVurderes
import org.junit.jupiter.api.Disabled
import kotlin.test.Test

internal class DagpengerAvgjørelseTest {
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
        avgjørelse shouldBe Avgjørelse.Innvilgelse
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
        avgjørelse shouldBe Avgjørelse.Endring
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
        avgjørelse shouldBe Avgjørelse.Stans
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
        avgjørelse shouldBe Avgjørelse.Gjenopptak
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

        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Stans
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
        RegelverkDagpenger.avgjørelse(andre) shouldBe Avgjørelse.Stans

        // Tredje behandling (meldekort): arver alt, legger ikke til nye perioder
        val tredje = Opplysninger.basertPå(andre)

        // Siste arvede periode har ikke rett, ingen nye perioder = avslag
        RegelverkDagpenger.avgjørelse(tredje) shouldBe Avgjørelse.Endring
    }

    @Test
    fun `avslag når arvet avslag og gjenopptak skal vurderes`() {
        // Stans → ny behandling med gjenopptak-vurdering som også ender med avslag
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024))))
            }
        val opplysninger =
            Opplysninger.basertPå(forrige).apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.mars(2024))))
                leggTil(Faktum(skalGjenopptakVurderes, true, Gyldighetsperiode(1.mars(2024))))
            }

        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Avslag
    }

    @Test
    fun `stans når arvet avslag og gjenopptak ikke skal vurderes`() {
        // Stans → ny behandling uten gjenopptak-vurdering, fortsatt avslag = Stans
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.januar(2024))))
            }
        val opplysninger =
            Opplysninger.basertPå(forrige).apply {
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.mars(2024))))
                // skalGjenopptakVurderes er ikke satt → kunEgne.har(...) == false
            }

        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Stans
    }

    @Test
    fun `gjenopptak når arvet rett, gap, og ny rett`() {
        // Innvilget → periode uten rett → ny periode med rett (gap mellom) = Gjenopptak
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024), 31.januar(2024))))
            }
        val opplysninger =
            Opplysninger.basertPå(forrige).apply {
                // Starter 1. mars — det er mer enn 0 dager etter 31. januar
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.mars(2024))))
            }

        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Gjenopptak
    }

    @Test
    @Disabled("Denne burde bli stans?")
    fun `endring når arvet rett og ny rett kant-i-kant`() {
        // Innvilget → ny rett starter dagen etter forrige slutt = ingen gap = Endring
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024), 31.januar(2024))))
            }
        val opplysninger =
            Opplysninger.basertPå(forrige).apply {
                // 1. februar er dagen etter 31. januar — Period.between(...).days == 0
                leggTil(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2024), 31.januar(2024))))
                leggTil(Faktum(harLøpendeRett, false, Gyldighetsperiode(1.februar(2024))))
            }

        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Stans
    }
}

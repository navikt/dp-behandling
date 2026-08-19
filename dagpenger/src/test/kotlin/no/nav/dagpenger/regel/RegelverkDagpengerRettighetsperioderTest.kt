package no.nav.dagpenger.regel

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.juni
import no.nav.dagpenger.dato.november
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class RegelverkDagpengerRettighetsperioderTest {
    @Test
    fun `generer rettighetsperioder`() {
        val innvilgelse = Opplysninger()
        innvilgelse.leggTil(
            Faktum(
                KravPåDagpenger.harLøpendeRett,
                true,
                Gyldighetsperiode(
                    fom = 24.november(2025),
                ),
            ),
        )
        val stans = Opplysninger.basertPå(innvilgelse)
        stans.leggTil(
            Faktum(
                KravPåDagpenger.harLøpendeRett,
                false,
                Gyldighetsperiode(
                    fom = 8.juni(2026),
                ),
            ),
        )
        val revurdering = Opplysninger.basertPå(stans)
        revurdering.leggTil(
            Faktum(
                KravPåDagpenger.harLøpendeRett,
                true,
                Gyldighetsperiode(
                    fom = 24.november(2025),
                ),
            ),
        )

        with(RegelverkDagpenger.rettighetsperioder(innvilgelse)) {
            this.shouldHaveSize(1)
            this.first().fraOgMed shouldBe 24.november(2025)
            this.first().tilOgMed shouldBe LocalDate.MAX
            this.first().harRett shouldBe true
        }

        with(RegelverkDagpenger.rettighetsperioder(stans)) {
            this.shouldHaveSize(2)
            this.first().fraOgMed shouldBe 24.november(2025)
            this.first().tilOgMed shouldBe 7.juni(2026)
            this.first().harRett shouldBe true

            this.last().fraOgMed shouldBe 8.juni(2026)
            this.last().tilOgMed shouldBe LocalDate.MAX
            this.last().harRett shouldBe false
        }

        with(RegelverkDagpenger.rettighetsperioder(revurdering)) {
            this.shouldHaveSize(1)
            this.first().fraOgMed shouldBe 24.november(2025)
            this.first().tilOgMed shouldBe LocalDate.MAX
            this.first().harRett shouldBe true
        }
    }
}

package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.TestOpplysningstyper.b
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.MAX

class TidslinjeByggerTest {
    @Test
    fun `slår sammen perioder med lik verdi`() {
        val perioder =
            listOf(
                a.periode(1.mai(2025), 10.mai(2025), true),
                a.periode(11.mai(2025), 20.mai(2025), false),
                a.periode(21.mai(2025), 30.mai(2025), true),
            )

        val resultat =
            TidslinjeBygger(perioder).medLikVerdi()
        resultat.shouldContainExactly(
            PeriodisertVerdi(1.mai(2025), 10.mai(2025), true),
            PeriodisertVerdi(11.mai(2025), 20.mai(2025), false),
            PeriodisertVerdi(21.mai(2025), 30.mai(2025), true),
        )
    }

    @Test
    fun `tåler veldig mange verdier`() {
        val perioder = mutableListOf<Opplysning<Boolean>>()
        var gjeldendeDato = 1.januar(2020)
        val antallÅr = 15
        repeat(365 * antallÅr) { _ ->
            perioder.add(a.periode(gjeldendeDato, gjeldendeDato, gjeldendeDato.year % 2 == 0))
            gjeldendeDato = gjeldendeDato.plusDays(1)
        }
        // Legg til en periode som varer til MAX for å teste at ikke eksploderer
        perioder.add(a.periode(gjeldendeDato, MAX, false))

        val tidslinjeBygger = TidslinjeBygger(perioder)
        val resultat = tidslinjeBygger.medLikVerdi()
        resultat shouldHaveSize antallÅr + 1
    }

    @Test
    fun `innvilgelse med flere vilkår, oppfylt på ulik startdato`() {
        val perioder =
            listOf(
                a.periode(1.mai(2025), 30.mai(2025), true),
                b.periode(1.mai(2025), 4.mai(2025), false),
                b.periode(5.mai(2025), 30.mai(2025), true),
            )

        val resultat =
            TidslinjeBygger(perioder).lagPeriode { påDato ->
                påDato.isNotEmpty() && påDato.all { it.verdi }
            }
        resultat.shouldContainExactly(
            PeriodisertVerdi(1.mai(2025), 4.mai(2025), false),
            PeriodisertVerdi(5.mai(2025), 30.mai(2025), true),
        )
    }

    @Test
    fun `slår sammen perioder med lik verdi, men uten hull`() {
        val perioder =
            listOf(
                a.periode(1.mai(2025), 10.mai(2025), true),
                a.periode(21.mai(2025), 30.mai(2025), true),
            )

        val resultat = TidslinjeBygger(perioder).medLikVerdi()

        resultat.shouldContainExactly(
            PeriodisertVerdi(1.mai(2025), 10.mai(2025), true),
            PeriodisertVerdi(21.mai(2025), 30.mai(2025), true),
        )
    }

    @Test
    fun `slår sammen perioder med lik verdi, men uten hull, med defaultverdi for hull`() {
        val perioder =
            listOf(
                a.periode(1.mai(2025), 10.mai(2025), true),
                a.periode(21.mai(2025), 30.mai(2025), true),
            )

        val defaultVerdi = false
        val resultat = TidslinjeBygger(perioder).medLikVerdi(defaultVerdi)
        resultat.shouldContainExactly(
            PeriodisertVerdi(1.mai(2025), 10.mai(2025), true),
            PeriodisertVerdi(11.mai(2025), 20.mai(2025), defaultVerdi),
            PeriodisertVerdi(21.mai(2025), 30.mai(2025), true),
        )
    }

    private fun <T : Comparable<T>> Opplysningstype<T>.periode(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate? = null,
        vurdering: T,
    ): Opplysning<T> =
        Faktum(
            this,
            vurdering,
            tilOgMed?.let { Gyldighetsperiode(fraOgMed, tilOgMed) } ?: Gyldighetsperiode(fraOgMed),
        )
}

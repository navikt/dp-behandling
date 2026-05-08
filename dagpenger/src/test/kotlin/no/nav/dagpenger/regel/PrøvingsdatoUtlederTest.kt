package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.dato.november
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadstidspunkt
import no.nav.dagpenger.regel.Søknadstidspunkt.ønsketdato
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PrøvingsdatoUtlederTest {
    @Test
    fun `nivå 1 - bruker fastsatt prøvingsdato når den finnes`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(søknadIdOpplysningstype, "123", Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(søknadsdato, 1.mai(2025), Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(ønsketdato, 15.mai(2025), Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(søknadstidspunkt, 15.mai(2025), Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(prøvingsdato, 15.mai(2025), Gyldighetsperiode(15.mai(2025))))
            }

        PrøvingsdatoUtleder.utled(opplysninger) shouldBe 15.mai(2025)
    }

    @Test
    fun `nivå 2 - bruker søknadstidspunkt når prøvingsdato ikke finnes`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(søknadIdOpplysningstype, "123", Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(søknadsdato, 1.mai(2025), Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(ønsketdato, 15.mai(2025), Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(søknadstidspunkt, 15.mai(2025), Gyldighetsperiode(1.mai(2025))))
            }

        PrøvingsdatoUtleder.utled(opplysninger) shouldBe 15.mai(2025)
    }

    @Test
    fun `nivå 3 - beregner max av søknadsdato og ønsketdato`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(søknadIdOpplysningstype, "123", Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(søknadsdato, 1.mai(2025), Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(ønsketdato, 15.mai(2025), Gyldighetsperiode(1.mai(2025))))
            }

        PrøvingsdatoUtleder.utled(opplysninger) shouldBe 15.mai(2025)
    }

    @Test
    fun `nivå 3 - søknadsdato vinner når ønsketdato er tidligere`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(søknadIdOpplysningstype, "123", Gyldighetsperiode(15.mai(2025))))
                leggTil(Faktum(søknadsdato, 15.mai(2025), Gyldighetsperiode(15.mai(2025))))
                leggTil(Faktum(ønsketdato, 1.mai(2025), Gyldighetsperiode(15.mai(2025))))
            }

        PrøvingsdatoUtleder.utled(opplysninger) shouldBe 15.mai(2025)
    }

    @Test
    fun `nivå 3 - kun søknadsdato tilgjengelig`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(søknadIdOpplysningstype, "123", Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(søknadsdato, 1.mai(2025), Gyldighetsperiode(1.mai(2025))))
            }

        PrøvingsdatoUtleder.utled(opplysninger) shouldBe 1.mai(2025)
    }

    @Test
    fun `nivå 4 - faller tilbake til siste opplysnings gyldighetsperiode`() {
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(søknadIdOpplysningstype, "123", Gyldighetsperiode(27.november(2025))))
            }

        PrøvingsdatoUtleder.utled(opplysninger) shouldBe 27.november(2025)
    }

    @Test
    fun `kaster feil når ingen opplysninger har startdato`() {
        val opplysninger = Opplysninger()

        assertThrows<IllegalStateException> {
            PrøvingsdatoUtleder.utled(opplysninger)
        }
    }

    @Test
    fun `bruker kun egne opplysninger, ikke arvede`() {
        val forrige =
            Opplysninger().apply {
                leggTil(Faktum(prøvingsdato, 1.januar(2025), Gyldighetsperiode(1.januar(2025))))
            }
        val ny =
            Opplysninger.basertPå(forrige).apply {
                leggTil(Faktum(søknadIdOpplysningstype, "456", Gyldighetsperiode(1.mai(2025))))
                leggTil(Faktum(søknadsdato, 1.mai(2025), Gyldighetsperiode(1.mai(2025))))
            }

        // Skal IKKE bruke arvet prøvingsdato fra januar, men søknadsdato fra mai
        PrøvingsdatoUtleder.utled(ny) shouldBe 1.mai(2025)
    }
}

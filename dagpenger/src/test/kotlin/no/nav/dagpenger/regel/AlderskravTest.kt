package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.IKontrollpunkt.Kontrollresultat.KreverAvklaring
import no.nav.dagpenger.opplysning.IKontrollpunkt.Kontrollresultat.OK
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.Alderskrav.Under18Kontroll
import no.nav.dagpenger.regel.Alderskrav.fødselsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadsdato
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AlderskravTest {
    @Test
    fun `Er søker over 18 år skal den ikke kontrolleres`() {
        val nå = LocalDate.now()
        val absolutt18 = nå.minusYears(18).minusDays(1)
        Under18Kontroll
            .evaluer(
                opplysninger(
                    Faktum(fødselsdato, absolutt18),
                    Faktum(søknadsdato, nå),
                ),
            ) shouldBe OK
    }

    @Test
    fun `Er søker under 18 år skal den manuelt kontrollers`() {
        val nå = LocalDate.now()
        val nesten18 = nå.minusYears(18).plusDays(1)
        Under18Kontroll
            .evaluer(
                opplysninger(
                    Faktum(fødselsdato, nesten18),
                    Faktum(søknadsdato, nå),
                ),
            ).shouldBeInstanceOf<KreverAvklaring>()
    }

    private fun opplysninger(vararg opplysning: Opplysning<*>) =
        Opplysninger(
            opplysning.toList(),
            emptyList(),
        )
}

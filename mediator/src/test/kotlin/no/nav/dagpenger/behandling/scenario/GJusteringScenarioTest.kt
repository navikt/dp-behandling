package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.helpers.scenario.sisteMelding
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.dato.juni
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag
import no.nav.dagpenger.regel.prosessvilkår.OmgjøringUtenKlage
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GJusteringScenarioTest {
    @Test
    fun `rekjøring via G-justering rekjører behandling og produserer nytt forslag`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            val antallForslagFørGJustering = antallMeldinger("forslag_til_behandlingsresultat")

            saksbehandler.startGJustering(1.juni(2018), 30.juni(2018))
            behovsløsere.løsTilForslag()

            // Nytt forslag skal ha blitt produsert etter rekjøringen
            antallMeldinger("forslag_til_behandlingsresultat") shouldBe antallForslagFørGJustering + 1

            // grunnbeløp-opplysningen skal finnes i det nye forslaget
            behandlingsresultatForslag {
                opplysninger(grunnbeløpForDagpengeGrunnlag).shouldNotBeEmpty()
            }
        }
    }

    @Test
    fun `omgjøring via G-justering oppretter ny behandling for ferdig behandling`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat { rettighetsperioder.single().harRett shouldBe true }

            val behandlingIdFørOmgjøring = person.behandlingId

            // G-justering trigger omgjøring for Ferdig-behandling
            saksbehandler.startGJustering(1.juni(2018), 30.juni(2018))

            // Ny behandling_opprettet skal ha blitt publisert
            val nyBehandlingId = rapidInspektør.sisteMelding("behandling_opprettet")["behandlingId"].asUUID()
            (nyBehandlingId != behandlingIdFørOmgjøring) shouldBe true

            // Den nye behandlingen har grunnbeløp satt fra initialOpplysninger
            behandlingsresultatForslag {
                opplysninger(grunnbeløpForDagpengeGrunnlag) shouldHaveSize 1
            }
        }
    }

    @Test
    fun `omgjøring med eksplisitt initialOpplysning for grunnbeløp overskriver arvet verdi`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat { rettighetsperioder.single().harRett shouldBe true }

            // Trigger omgjøring med eksplisitt (kunstig) grunnbeløp via Kafka-melding
            val eksplisittGrunnbeløp = BigDecimal("200000")
            saksbehandler.omgjørBehandlingMedOpplysninger(
                gjelderDato = 21.juni(2018),
                initialOpplysninger =
                    listOf(
                        mapOf(
                            "opplysningstype" to OpplysningsTyper.GrunnbeløpForGrunnlagId.uuid.toString(),
                            "verdi" to eksplisittGrunnbeløp.toPlainString(),
                            "gyldigFraOgMed" to "2018-06-21",
                        ),
                    ),
            )

            // Svare på OmgjøringUtenKlage-avklaring
            saksbehandler.endreOpplysning(OmgjøringUtenKlage.ansesUgyldigVedtak, true)
            // NyttGrunnbeløpForGrunnlag-avklaring siden 200000 ≠ library-verdien
            saksbehandler.lukkAlleAvklaringer()
            behovsløsere.løsTilForslag()

            // Verify at grunnbeløp er satt til den eksplisitte verdien
            behandlingsresultatForslag {
                val grunnbeløpPerioder = opplysninger(grunnbeløpForDagpengeGrunnlag)
                grunnbeløpPerioder.shouldNotBeEmpty()
                val verdiString =
                    grunnbeløpPerioder
                        .last()
                        .verdi.verdi
                        .toString()
                verdiString.toBigDecimalOrNull()!!.stripTrailingZeros() shouldBe eksplisittGrunnbeløp.stripTrailingZeros()
            }
        }
    }

    private fun SimulertDagpengerSystem.antallMeldinger(eventNavn: String): Int {
        var count = 0
        for (offset in 0 until rapidInspektør.size) {
            if (rapidInspektør.message(offset)["@event_name"].asText() == eventNavn) count++
        }
        return count
    }
}

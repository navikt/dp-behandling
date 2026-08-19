package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.inntektFraSkatt
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RevurderingInntektTest {
    @Test
    fun `revurdering skal innhente inntekt på nytt når inntekten er endret siden førstegangsbehandlingen`() {
        val søknadsdato = 21.juni(2018)

        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            val (opprinneligGrunnlag, opprinneligSats) = innvilgOgVerifiserFørstegangsbehandling(søknadsdato)

            // 2. Inntekten endres i inntektsregisteret i tidsrommet mellom søknad (og innhenting av inntekt) og
            // godkjenning av behandlingen. Den opprinnelige behandlingen bygger fortsatt på den gamle inntekten.
            person.inntektSiste12Mnd = 700000

            // 3. Det lages en revurdering som skal innhente inntekten på nytt, slik at riktig (oppdatert) inntekt
            // legges til grunn og grunnlag/sats blir korrigert. Her simuleres det ved at inntekten "pushes" rett
            // inn i revurderingen - se `rekjørBehandling`-varianten under for hvordan dette faktisk skjer i
            // produksjon, via inntektsverktøyets rekjør_behandling + oppfriskOpplysningIder.
            saksbehandler.omgjørBehandling(søknadsdato)
            saksbehandler.endreOpplysning(
                inntektFraSkatt,
                Inntekt(person.inntekt(person.inntektSiste12Mnd, søknadsdato.minusMonths(2))),
                "Innhentet inntekt på nytt ved revurdering",
                Gyldighetsperiode(søknadsdato),
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            verifiserOppdatertGrunnlagOgSats(opprinneligGrunnlag, opprinneligSats)
        }
    }

    @Test
    fun `revurdering skal innhente inntekt på nytt når inntektsverktøyet ber om rekjøring med oppfriskOpplysningIder`() {
        val søknadsdato = 21.juni(2018)

        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            val (opprinneligGrunnlag, opprinneligSats) = innvilgOgVerifiserFørstegangsbehandling(søknadsdato)

            // 2. Inntekten endres i inntektsregisteret i tidsrommet mellom søknad (og innhenting av inntekt) og
            // godkjenning av behandlingen. Den opprinnelige behandlingen bygger fortsatt på den gamle inntekten.
            person.inntektSiste12Mnd = 700000

            // 3. Det lages en revurdering. I stedet for å legge inn ny inntekt direkte, simulerer vi at
            // inntektsverktøyet ber om rekjøring av behandlingen med oppfriskOpplysningIder=[inntektFraSkatt] -
            // slik det faktisk skjer i produksjon. Dette utløser en tombstone av den arvede inntekten
            // (se `Opplysninger.lagTombstone`), som gjør at regelmotoren ber om et nytt Inntekt-behov,
            // som besvares med den oppdaterte inntekten.
            saksbehandler.omgjørBehandling(søknadsdato)
            saksbehandler.rekjørBehandling(listOf(inntektFraSkatt))
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            // I motsetning til den direkte-push-varianten er den nye inntekten satt av en behovsløser
            // (maskinell kilde), ikke av en saksbehandler. Da krever ikke omgjøringsprosessen
            // totrinnskontroll (se Omgjøringsprosess.kreverTotrinnskontroll), og behandlingen blir
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            verifiserOppdatertGrunnlagOgSats(opprinneligGrunnlag, opprinneligSats)
        }
    }

    // 1. Bruker innvilges dagpenger med en gitt inntekt
    private fun SimulertDagpengerSystem.innvilgOgVerifiserFørstegangsbehandling(søknadsdato: LocalDate): Pair<Any?, Any?> {
        person.søkDagpenger(søknadsdato)
        behovsløsere.løsTilForslag()
        saksbehandler.lukkAlleAvklaringer()
        saksbehandler.godkjenn()
        saksbehandler.beslutt()

        var opprinneligGrunnlag: Any? = null
        var opprinneligSats: Any? = null
        behandlingsresultat(1) {
            rettighetsperioder.single().harRett shouldBe true

            opplysninger(grunnlag) {
                shouldHaveSize(1)
                opprinneligGrunnlag = single().verdi.verdi
            }
            opplysninger(dagsatsEtterSamordningMedBarnetillegg) {
                shouldHaveSize(1)
                opprinneligSats = single().verdi.verdi
            }
        }
        return opprinneligGrunnlag to opprinneligSats
    }

    private fun SimulertDagpengerSystem.verifiserOppdatertGrunnlagOgSats(
        opprinneligGrunnlag: Any?,
        opprinneligSats: Any?,
    ) {
        behandlingsresultat(2) {
            // Riktig (oppdatert) inntekt er lagt til grunn, og grunnlag/sats er derfor endret
            opplysninger(grunnlag) {
                shouldHaveSize(1)
                single().verdi.verdi shouldNotBe opprinneligGrunnlag
            }
            opplysninger(dagsatsEtterSamordningMedBarnetillegg) {
                shouldHaveSize(1)
                single().verdi.verdi shouldNotBe opprinneligSats
            }
        }
    }
}

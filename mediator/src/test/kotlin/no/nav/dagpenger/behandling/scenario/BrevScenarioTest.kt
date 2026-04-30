package no.nav.dagpenger.behandling.scenario

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.brev.BrevBygger
import no.nav.dagpenger.brev.MarkdownRenderer
import no.nav.dagpenger.brev.Plassering
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.brev.DagpengerBrevmal
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.antallBarn
import org.junit.jupiter.api.Test

class BrevScenarioTest {
    @Test
    fun `bygger innvilgelsesbrev`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2024))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val resultatJson = behovsløsere.sisteBehandlingsresultat().second
            val brev = byggBrev(resultatJson)

            brev.shouldNotBeNull()
            brev.overskrift shouldBe "Nav har innvilget søknaden din om dagpenger"

            // Innledning skal inneholde periodeinfo
            val innledning = brev.seksjoner.filter { it.plassering == Plassering.INNLEDNING }
            val innledningTekst = innledning.flatMap { it.innhold }.joinToString("\n")
            innledningTekst.shouldContain("Du får dagpenger fra og med")
            innledningTekst.shouldNotContain("{{")

            // Fastsettelser skal finnes
            val fastsettelser = brev.seksjoner.filter { it.plassering == Plassering.FASTSETTELSE }
            fastsettelser.shouldNotBeNull()

            // Ingen uløste placeholders i hele brevet
            val altTekst = brev.seksjoner.flatMap { it.innhold }.joinToString("\n")
            altTekst.shouldNotContain("{{")

            // Print brevet for visuell inspeksjon
            println(MarkdownRenderer.render(brev))
        }
    }

    @Test
    fun `bygger avslagsbrev`() {
        nyttScenario {
            alder = 58
            inntektSiste12Mnd = 5000
        }.test {
            person.søkDagpenger(21.juni(2024))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            val resultatJson = behovsløsere.sisteBehandlingsresultat().second
            val brev = byggBrev(resultatJson)

            brev.shouldNotBeNull()
            brev.overskrift shouldBe "Nav har avslått søknaden din om dagpenger"

            // Begrunnelse skal finnes
            val begrunnelse = brev.seksjoner.filter { it.plassering == Plassering.BEGRUNNELSE }
            begrunnelse
                .flatMap { it.innhold }
                .joinToString("\n")
                .shouldContain("inntekt")

            println(MarkdownRenderer.render(brev))
        }
    }

    @Test
    fun `produserer brev om stans`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2024))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.avsluttArbeidssøkerperiode(25.juni(2024))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            val resultatJson = behovsløsere.sisteBehandlingsresultat().second
            val brev = byggBrev(resultatJson)

            brev.shouldNotBeNull()
            brev.overskrift shouldBe "Nav har stanset dagpengene dine"

            // Begrunnelse skal forklare hvorfor stans
            val begrunnelse = brev.seksjoner.filter { it.plassering == Plassering.BEGRUNNELSE }
            begrunnelse
                .flatMap { it.innhold }
                .joinToString("\n")
                .shouldContain("registrert som arbeidssøker")

            // Informasjon om gjenopptak
            val informasjon = brev.seksjoner.filter { it.plassering == Plassering.INFORMASJON }
            informasjon
                .flatMap { it.innhold }
                .joinToString("\n")
                .shouldContain("dagpenger igjen")

            println(MarkdownRenderer.render(brev))
        }
    }

    @Test
    fun `produserer ikke brev om rene meldekortberegninger`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2024))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(1)
            meldekortBatch(true)

            val resultatJson = behovsløsere.sisteBehandlingsresultat().second
            val brev = byggBrev(resultatJson)

            // Bare beregninger skal ikke lage brev
            brev.shouldBeNull()
        }
    }

    @Test
    fun `produserer brev når meldekortberegninger har andre opplysnigner`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2024))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(1)
            meldekortBatch()
            saksbehandler.endreOpplysning(antallBarn, 2, "Fødte i går", Gyldighetsperiode(24.juni(2018)))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val resultatJson = behovsløsere.sisteBehandlingsresultat().second
            val brev = byggBrev(resultatJson)

            brev.shouldNotBeNull()
            brev.overskrift shouldBe "Nav har endret dagpengene dine"
            brev.seksjoner.any { it.plassering == Plassering.FASTSETTELSE } shouldBe true
        }
    }

    @Test
    fun `produserer brev for gjenopptak`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2024))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(1)
            meldekortBatch(true)

            person.avsluttArbeidssøkerperiode(28.juni(2024))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            person.søkGjenopptak(28.juli(2024))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val resultatJson = behovsløsere.sisteBehandlingsresultat().second
            val brev = byggBrev(resultatJson)

            brev.shouldNotBeNull()
            brev.overskrift shouldBe "Nav har gjenopptatt dagpengene dine"

            // Innledning med dagsats
            val innledning = brev.seksjoner.filter { it.plassering == Plassering.INNLEDNING }
            innledning
                .flatMap { it.innhold }
                .joinToString("\n")
                .shouldContain("kroner dagen")

            // Fastsettelse (sats/beregning)
            val fastsettelse = brev.seksjoner.filter { it.plassering == Plassering.FASTSETTELSE }
            fastsettelse.shouldNotBeEmpty()

            println(MarkdownRenderer.render(brev))
        }
    }

    companion object {
        fun byggBrev(resultatJson: JsonNode) =
            BrevBygger(DagpengerBrevmal).bygg(
                objectMapper.treeToValue<BehandlingsresultatDTO>(resultatJson),
            )
    }
}

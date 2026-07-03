package no.nav.dagpenger.mediator.simulering

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Id
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Testregelsettet modellerer et forenklet alderskrav:
 *
 *   fødselsdato   (Ekstern – innhentes fra utsiden)
 *   aldersgrense  (oppslag – konstant 67)
 *   sisteMuligDag = sisteDagIMåned(fødselsdato + aldersgrense år)
 *   oppfyllerAlder = prøvingsdato <= sisteMuligDag
 *
 * prøvingsdato er en ekstern avhengighet uten Ekstern-regel i dette regelsettet.
 */
class SimuleringsEvalueringTest {
    private val evaluering = SimuleringsEvaluering()

    private val fødselsdato = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Fødselsdato")
    private val prøvingsdato = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Prøvingsdato")
    private val aldersgrense = Opplysningstype.heltall(Id(UUIDv7.ny(), Heltall), "Aldersgrense")
    private val sisteMåned = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Sistemmåned")
    private val sisteMuligDag = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Siste mulige dag")
    private val oppfyllerAlder = Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "Oppfyller alderskrav")

    private val regelsett =
        vilkår("Test alderskrav") {
            regel(fødselsdato) { innhentes }
            regel(aldersgrense) { somUtgangspunkt(67) }
            regel(sisteMåned) { leggTilÅr(fødselsdato, aldersgrense) }
            regel(sisteMuligDag) { sisteDagIMåned(sisteMåned) }
            regel(oppfyllerAlder) { førEllerLik(prøvingsdato, sisteMuligDag) }
        }

    private val dato = LocalDate.of(2024, 6, 19)

    @Test
    fun `produserer utfall når alle inndata er oppgitt`() {
        val resultat =
            evaluering.evaluer(
                regelsett,
                dato,
                mapOf("Fødselsdato" to "1980-01-01", "Prøvingsdato" to dato.toString()),
            )

        resultat.mangler.shouldBeEmpty()
        resultat.opplysninger.any { it.behovId == "Oppfyller alderskrav" } shouldBe true
    }

    @Test
    fun `rapporterer Ekstern-avhengighet som mangler`() {
        val resultat = evaluering.evaluer(regelsett, dato, mapOf("Prøvingsdato" to dato.toString()))

        resultat.mangler shouldContainExactlyInAnyOrder listOf("Fødselsdato")
    }

    @Test
    fun `rapporterer avhengighet uten Ekstern-regel som mangler`() {
        // prøvingsdato har ingen Ekstern-regel i dette regelsettet, men er nødvendig
        val resultat = evaluering.evaluer(regelsett, dato, mapOf("Fødselsdato" to "1980-01-01"))

        resultat.mangler shouldContainExactlyInAnyOrder listOf("Prøvingsdato")
    }

    @Test
    fun `rapporterer alle manglende inndata`() {
        val resultat = evaluering.evaluer(regelsett, dato, emptyMap())

        resultat.mangler shouldContainExactlyInAnyOrder listOf("Fødselsdato", "Prøvingsdato")
    }

    @Test
    fun `evalueringstre inneholder utledningskjede for utfall`() {
        val resultat =
            evaluering.evaluer(
                regelsett,
                dato,
                mapOf("Fødselsdato" to "1980-01-01", "Prøvingsdato" to dato.toString()),
            )

        val utfall = resultat.opplysninger.single { it.behovId == "Oppfyller alderskrav" }
        (utfall.utledetAv != null) shouldBe true
        utfall.utledetAv!!.avhengigheter.isNotEmpty() shouldBe true
    }

    @Test
    fun `ukjente behovId-er i inndata ignoreres stille`() {
        val resultat =
            evaluering.evaluer(
                regelsett,
                dato,
                mapOf(
                    "Fødselsdato" to "1980-01-01",
                    "Prøvingsdato" to dato.toString(),
                    "FinnesIkke" to "tulleverdi",
                ),
            )

        resultat.mangler.shouldBeEmpty()
    }

    @Test
    fun `produserer interne opplysninger uten eksternt inndata`() {
        val resultat = evaluering.evaluer(regelsett, dato, emptyMap())

        // Aldersgrense er intern og skal alltid produseres
        resultat.opplysninger.any { it.behovId == "Aldersgrense" } shouldBe true
    }
}

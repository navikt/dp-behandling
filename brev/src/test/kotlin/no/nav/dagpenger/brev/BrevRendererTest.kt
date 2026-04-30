package no.nav.dagpenger.brev

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import kotlin.test.Test

class BrevRendererTest {
    private val brev =
        Brev(
            overskrift = "Nav har innvilget søknaden din om dagpenger",
            seksjoner =
                listOf(
                    Brevseksjon(
                        plassering = Plassering.INNLEDNING,
                        innhold = listOf("Du får dagpenger fra og med 2026-04-23."),
                    ),
                    Brevseksjon(
                        plassering = Plassering.VILKÅR,
                        tittel = "Du oppfyller kravet til alder",
                        innhold = listOf("Vurderingen er gjort etter folketrygdloven § 4-23."),
                    ),
                    Brevseksjon(
                        plassering = Plassering.FASTSETTELSE,
                        tittel = "Slik har vi beregnet dagpengene dine",
                        innhold = listOf("Du får 462 kroner per dag.", "Grunnlaget er 192 508 kroner."),
                    ),
                    Brevseksjon(
                        plassering = Plassering.INFORMASJON,
                        tittel = "Du må sende meldekort",
                        innhold = listOf("For å ha rett på dagpenger må du sende meldekort hver 14. dag."),
                    ),
                ),
        )

    @Test
    fun `markdown renderer`() {
        val md = MarkdownRenderer.render(brev)

        md shouldStartWith "# Nav har innvilget søknaden din om dagpenger\n"
        md shouldContain "## Du oppfyller kravet til alder\n"
        md shouldContain "## Slik har vi beregnet dagpengene dine\n"
        md shouldContain "Du får 462 kroner per dag.\n"
        md shouldContain "Grunnlaget er 192 508 kroner.\n"
        md shouldContain "## Du må sende meldekort\n"
    }

    @Test
    fun `typst renderer`() {
        val typst = TypstRenderer.render(brev)

        typst shouldStartWith "= Nav har innvilget søknaden din om dagpenger\n"
        typst shouldContain "== Du oppfyller kravet til alder\n"
        typst shouldContain "== Slik har vi beregnet dagpengene dine\n"
        typst shouldContain "Du får 462 kroner per dag.\n"
        typst shouldContain "== Du må sende meldekort\n"
    }

    @Test
    fun `seksjon uten tittel rendres uten overskrift`() {
        val md = MarkdownRenderer.render(brev)
        val typst = TypstRenderer.render(brev)

        // Innledningen har ingen tittel — teksten kommer rett etter hovedoverskriften
        md shouldContain "# Nav har innvilget søknaden din om dagpenger\n\nDu får dagpenger fra og med"
        typst shouldContain "= Nav har innvilget søknaden din om dagpenger\n\nDu får dagpenger fra og med"
    }

    @Test
    fun `full markdown output`() {
        val expected =
            """
            |# Nav har innvilget søknaden din om dagpenger
            |
            |Du får dagpenger fra og med 2026-04-23.
            |
            |## Du oppfyller kravet til alder
            |
            |Vurderingen er gjort etter folketrygdloven § 4-23.
            |
            |## Slik har vi beregnet dagpengene dine
            |
            |Du får 462 kroner per dag.
            |
            |Grunnlaget er 192 508 kroner.
            |
            |## Du må sende meldekort
            |
            |For å ha rett på dagpenger må du sende meldekort hver 14. dag.
            |
            """.trimMargin()

        MarkdownRenderer.render(brev) shouldBe expected
    }

    @Test
    fun `full typst output`() {
        val expected =
            """
            |= Nav har innvilget søknaden din om dagpenger
            |
            |Du får dagpenger fra og med 2026-04-23.
            |
            |== Du oppfyller kravet til alder
            |
            |Vurderingen er gjort etter folketrygdloven § 4-23.
            |
            |== Slik har vi beregnet dagpengene dine
            |
            |Du får 462 kroner per dag.
            |
            |Grunnlaget er 192 508 kroner.
            |
            |== Du må sende meldekort
            |
            |For å ha rett på dagpenger må du sende meldekort hver 14. dag.
            |
            """.trimMargin()

        TypstRenderer.render(brev) shouldBe expected
    }

    @Test
    fun `typst renderer konverterer markdown-lenker og bold`() {
        val brevMedLenker =
            Brev(
                overskrift = "Test",
                seksjoner =
                    listOf(
                        Brevseksjon(
                            plassering = Plassering.INFORMASJON,
                            innhold =
                                listOf(
                                    "Les mer på [nav.no/dagpenger](https://nav.no/dagpenger).",
                                    "**Derfor får du dagpenger fra 1. mai 2026**",
                                ),
                        ),
                    ),
            )
        val typst = TypstRenderer.render(brevMedLenker)
        typst shouldContain """Les mer på #link("https://nav.no/dagpenger")[nav.no/dagpenger]."""
        typst shouldContain "*Derfor får du dagpenger fra 1. mai 2026*"
    }
}

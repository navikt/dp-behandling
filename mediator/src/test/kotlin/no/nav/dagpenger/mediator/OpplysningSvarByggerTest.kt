package no.nav.dagpenger.mediator

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.verdier.Barnekilde
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class OpplysningSvarByggerTest {
    @Test
    fun `barnMapper tolker gammelt V1-format - ren liste uten søknadbarnId`() {
        @Language("JSON")
        val v1Json =
            """
            [
              {
                "fødselsdato": "2011-08-22",
                "fornavnOgMellomnavn": "ALTERNATIV",
                "etternavn": "HUSBÅT",
                "statsborgerskap": "XUK",
                "kvalifiserer": false
              }
            ]
            """.trimIndent()

        val barnListe = barnMapper(v1Json)

        barnListe.søknadbarnId.shouldBeNull()
        barnListe.barn shouldHaveSize 1
        barnListe.barn.first().kvalifiserer shouldBe false
        barnListe.barn.first().forsørgeransvar shouldBe false
    }

    @Test
    fun `barnMapper tolker V2-format med søknadbarnId`() {
        @Language("JSON")
        val v2Json =
            """
            {
              "søknadbarnId": "a60775a8-6e1c-4a97-96c2-74b696670c24",
              "barn": [
                {
                  "fødselsdato": "2019-09-19",
                  "fornavnOgMellomnavn": "EKSTRA",
                  "etternavn": "HUSBÅT",
                  "statsborgerskap": "XUK",
                  "kvalifiserer": true
                }
              ]
            }
            """.trimIndent()

        val barnListe = barnMapper(v2Json)

        barnListe.søknadbarnId.shouldNotBeNull()
        barnListe.barn.first().kvalifiserer shouldBe true
    }

    @Test
    fun `barnMapper tar med alle nye felter fra HTTP-korrigering uten å miste data`() {
        @Language("JSON")
        val korrigertJson =
            """
            {
              "søknadbarnId": "a60775a8-6e1c-4a97-96c2-74b696670c24",
              "barn": [
                {
                  "kilde": "Saksbehandler",
                  "ident": "12345678910",
                  "fødselsdato": "2019-09-19",
                  "fornavnOgMellomnavn": "EKSTRA",
                  "etternavn": "HUSBÅT",
                  "statsborgerskap": "XUK",
                  "oppholdsland": "NOR",
                  "kvalifiserer": false,
                  "forsørgeransvar": true,
                  "begrunnelse": "Saksbehandler har overtatt forsørgeransvaret"
                }
              ]
            }
            """.trimIndent()

        val barn = barnMapper(korrigertJson).barn.single()

        barn.kilde shouldBe Barnekilde.Saksbehandler
        barn.ident shouldBe "12345678910"
        barn.oppholdsland shouldBe "NOR"
        // forsørgeransvar skal kunne overstyres uavhengig av kvalifiserer
        barn.kvalifiserer shouldBe false
        barn.forsørgeransvar shouldBe true
        barn.begrunnelse shouldBe "Saksbehandler har overtatt forsørgeransvaret"
    }
}

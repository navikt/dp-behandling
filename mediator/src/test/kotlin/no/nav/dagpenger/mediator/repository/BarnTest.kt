package no.nav.dagpenger.mediator.repository

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.opplysning.verdier.BarnListe
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.jvm.java

class BarnTest {
    @Test
    fun `skal migrere barn fra gammel til ny JSON`() {
        val barn = objectMapper.readValue(barnJSON, BarnListe::class.java)

        barn.søknadbarnId.shouldNotBeNull()
        barn.barn.first().kvalifiserer shouldBe false
        barn.barn.first().forsørgeransvar shouldBe false
    }

    @Language("JSON")
    private val barnJSON =
        """
        {
          "barn": [
            {
              "etternavn": "HUSBÅT",
              "fødselsdato": "2011-08-22",
              "kvalifiserer": false,
              "statsborgerskap": "XUK",
              "fornavnOgMellomnavn": "ALTERNATIV"
            },
            {
              "etternavn": "HUSBÅT",
              "fødselsdato": "2019-09-19",
              "kvalifiserer": true,
              "statsborgerskap": "XUK",
              "fornavnOgMellomnavn": "EKSTRA"
            }
          ],
          "søknadbarnId": "a60775a8-6e1c-4a97-96c2-74b696670c24"
        }
        """.trimIndent()
}

package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.jobber.GJustering
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class GJusteringMottakTest {
    private val gJustering = mockk<GJustering>(relaxed = true)
    private val rapid = TestRapid().also { GJusteringMottak(it, gJustering) }

    @Test
    fun `tar imot start_g_justering og kaller juster`() {
        rapid.sendTestMessage(startGJusteringMelding)

        verify {
            gJustering.startGjustering(any(), any(), any())
        }
    }

    @Language("JSON")
    private val startGJusteringMelding =
        """
        {
            "@event_name": "start_g_justering",
            "fraOgMed": "2025-05-01",
            "tilOgMed": "2025-05-20"
        }
        """.trimIndent()
}

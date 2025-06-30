package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.api.melding.FjernOpplysning
import no.nav.dagpenger.behandling.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class FjernOpplysningMottakTest {
    private val rapid = TestRapid()
    private val mediator = mockk<IMessageMediator>(relaxed = true)

    init {
        FjernOpplysningMottak(rapid, mediator, mockk(relaxed = true))
    }

    @Test
    fun `fjerner opplysning`() {
        rapid.sendTestMessage(FjernOpplysning(UUIDv7.ny(), UUIDv7.ny(), "fjas", "12345678901", "saksbehandler").toJson())

        verify {
            mediator.behandle(any<FjernOpplysningHendelse>(), any(), any())
        }
    }
}

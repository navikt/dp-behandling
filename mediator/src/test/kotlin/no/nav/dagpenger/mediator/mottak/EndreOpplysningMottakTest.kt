package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.mediator.api.melding.EndreOpplysning
import no.nav.dagpenger.modell.hendelser.EndreOpplysningHendelse
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EndreOpplysningMottakTest {
    private val rapid = TestRapid()
    private val mediator = mockk<IMessageMediator>(relaxed = true)
    private val type = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "EndresDato")

    init {
        EndreOpplysningMottak(rapid, mediator, setOf(type))
    }

    @Test
    fun `endrer verdi og gyldighetsperiode på eksisterende opplysning`() {
        val opplysningId = UUIDv7.ny()
        rapid.sendTestMessage(
            EndreOpplysning(
                behandlingId = UUIDv7.ny(),
                opplysningId = opplysningId,
                behovId = type.behovId,
                ident = "12345678901",
                verdi = LocalDate.of(2025, 3, 1),
                gyldigFraOgMed = LocalDate.of(2025, 2, 1),
                gyldigTilOgMed = null,
                saksbehandler = "Z123456",
                begrunnelse = "Feil dato",
            ).toJson(),
        )

        val hendelse = slot<EndreOpplysningHendelse>()
        verify { mediator.behandle(capture(hendelse), any(), any()) }
        hendelse.captured.opplysningId shouldBe opplysningId
        hendelse.captured.verdi shouldBe LocalDate.of(2025, 3, 1)
        hendelse.captured.gyldighetsperiode shouldBe Gyldighetsperiode(LocalDate.of(2025, 2, 1))
    }

    @Test
    fun `endring av bare gyldighetsperiode lar verdien være uendret`() {
        rapid.sendTestMessage(
            EndreOpplysning(
                behandlingId = UUIDv7.ny(),
                opplysningId = UUIDv7.ny(),
                behovId = type.behovId,
                ident = "12345678901",
                verdi = null,
                gyldigFraOgMed = LocalDate.of(2025, 2, 1),
                gyldigTilOgMed = LocalDate.of(2025, 2, 28),
                saksbehandler = "Z123456",
                begrunnelse = "Feil periode",
            ).toJson(),
        )

        val hendelse = slot<EndreOpplysningHendelse>()
        verify { mediator.behandle(capture(hendelse), any(), any()) }
        hendelse.captured.verdi shouldBe null
        hendelse.captured.gyldighetsperiode shouldBe Gyldighetsperiode(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28))
    }
}

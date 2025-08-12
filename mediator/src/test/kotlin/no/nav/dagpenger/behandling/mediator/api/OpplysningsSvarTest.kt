package no.nav.dagpenger.behandling.mediator.api

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.dagpenger.behandling.mediator.api.melding.OpplysningsSvar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class OpplysningsSvarTest {
    @Test
    fun `lager JSON med nullable gyldighetsperiode`() {
        val opplysningsSvar =
            OpplysningsSvar(
                behandlingId = UUID.randomUUID(),
                opplysningNavn = "testOpplysning",
                ident = "12345678901",
                verdi = "testVerdi",
                saksbehandler = "saksbehandler",
                begrunnelse = "testBegrunnelse",
                gyldigFraOgMed = LocalDate.now(),
            )

        with(opplysningsSvar.toJson()) {
            this shouldContain "gyldigFraOgMed"
            this shouldNotContain "gyldigTilOgMed"
        }
    }
}

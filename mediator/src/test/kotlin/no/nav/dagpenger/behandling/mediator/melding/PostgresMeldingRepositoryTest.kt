package no.nav.dagpenger.behandling.mediator.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.mockk
import no.nav.dagpenger.behandling.db.Postgres
import no.nav.dagpenger.behandling.mediator.mottak.SøknadInnsendtMessage
import no.nav.dagpenger.uuid.UUIDv7
import kotlin.test.Test

internal class PostgresMeldingRepositoryTest {
    @Test
    fun `lagre og hent hendelse`() {
        val hendelseId = UUIDv7.ny()

        // language=JSON
        val originalMessage =
            """{
            |   "@event_name": "søknad_behandlingsklar",
            |   "@id": "$hendelseId",
            |   "ident": "12345678910",
            |   "søknadId": "123e4567-e89b-12d3-a456-426614174000"
            |}
            """.trimMargin()
        val jsonMessage =
            JsonMessage(originalMessage, MessageProblems(originalMessage), mockk(relaxed = true)).also {
                it.interestedIn("@id", "ident", "søknadId")
            }
        val søknadInnsendtMessage = SøknadInnsendtMessage(jsonMessage)

        Postgres.withMigratedDb {
            val postgresHendelseRepository = PostgresMeldingRepository()
            postgresHendelseRepository.lagreMelding(
                melding = søknadInnsendtMessage,
                ident = søknadInnsendtMessage.ident,
                id = søknadInnsendtMessage.id,
                toJson = originalMessage,
            )
            postgresHendelseRepository.erBehandlet(søknadInnsendtMessage.id) shouldBeEqual false
            postgresHendelseRepository.markerSomBehandlet(søknadInnsendtMessage.id)
            postgresHendelseRepository.erBehandlet(søknadInnsendtMessage.id) shouldBeEqual true
        }
    }
}

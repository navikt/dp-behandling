package no.nav.dagpenger.behandling.mediator.melding

import io.kotest.matchers.equals.shouldBeEqual
import no.nav.dagpenger.behandling.db.withMigratedDb
import no.nav.dagpenger.behandling.mediator.repository.ApiMelding
import kotlin.test.Test

internal class PostgresMeldingRepositoryTest {
    @Test
    fun `lagre og hent hendelse`() {
        val ident = "12345678910"
        val melding = ApiMelding(ident)

        withMigratedDb {
            val postgresHendelseRepository = PostgresMeldingRepository(dbSession)
            postgresHendelseRepository.lagreMelding(
                melding = melding,
                ident = ident,
                id = melding.id,
                toJson = """{"ident": "$ident"}""",
            )
            postgresHendelseRepository.erBehandlet(melding.id) shouldBeEqual false
            postgresHendelseRepository.markerSomBehandlet(melding.id)
            postgresHendelseRepository.erBehandlet(melding.id) shouldBeEqual true
        }
    }
}

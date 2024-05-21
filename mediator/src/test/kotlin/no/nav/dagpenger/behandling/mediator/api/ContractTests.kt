package no.nav.dagpenger.behandling.mediator.api

import `in`.specmatic.test.SpecmaticJUnitSupport
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.dagpenger.behandling.db.InMemoryPersonRepository
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.mediator.api.TestApplication.AZUREAD_ISSUER_ID
import no.nav.dagpenger.behandling.mediator.api.TestApplication.CLIENT_ID
import no.nav.dagpenger.behandling.mediator.api.TestApplication.mockOAuth2Server
import no.nav.dagpenger.behandling.mediator.api.TestApplication.testAzureAdToken
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvar
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.UUIDv7
import no.nav.dagpenger.regel.Søknadstidspunkt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.time.LocalDate

class ContractTests : SpecmaticJUnitSupport() {
    companion object {
        private lateinit var server: ApplicationEngine

        private val ident = "12345123451"
        private val person =
            Person(ident.tilPersonIdentfikator()).also {
                it.håndter(
                    SøknadInnsendtHendelse(
                        meldingsreferanseId = UUIDv7.ny(),
                        ident = ident,
                        søknadId = UUIDv7.ny(),
                        gjelderDato = LocalDate.now(),
                        fagsakId = 1,
                    ),
                )
                it.håndter(
                    OpplysningSvarHendelse(
                        meldingsreferanseId = UUIDv7.ny(),
                        ident = ident,
                        behandlingId = it.behandlinger().first().behandlingId,
                        opplysninger =
                            listOf(
                                OpplysningSvar(
                                    opplysningstype = Søknadstidspunkt.søknadsdato,
                                    verdi = LocalDate.now(),
                                    tilstand = OpplysningSvar.Tilstand.Faktum,
                                    kilde = Saksbehandlerkilde("Z123456"),
                                ),
                            ),
                    ),
                )
            }
        private val personRepository =
            spyk(InMemoryPersonRepository()).also {
                it.lagre(person)
                every { it.hentBehandling(any()) } returns person.behandlinger().first()
            }

        @JvmStatic
        @BeforeAll
        fun setUp() {
            System.setProperty("host", "localhost")
            System.setProperty("port", "8081")
            System.setProperty("azure-app.client-id", CLIENT_ID)
            System.setProperty("Grupper.saksbehandler", "dagpenger-saksbehandler")
            System.setProperty("azure-app.well-known-url", "${mockOAuth2Server.wellKnownUrl(AZUREAD_ISSUER_ID)}")
            System.setProperty("azureAd", testAzureAdToken(listOf("dagpenger-saksbehandler")))
            // System.setProperty("SPECMATIC_GENERATIVE_TESTS", "true")
            withMigratedDb {
                server =
                    embeddedServer(CIO, port = 8081) {
                        behandlingApi(
                            personRepository = personRepository,
                            personMediator = mockk(relaxed = true),
                            auditlogg = mockk(relaxed = true),
                        )
                    }.start()
            }
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            server.stop(1000, 1000)
            System.clearProperty("azure-app.client-id")
            System.clearProperty("azure-app.well-known-url")
            System.clearProperty("Grupper.saksbehandler")
        }
    }
}

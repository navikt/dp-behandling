package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.UUIDv7
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingRepositoryPostgresTest {
    private val ident = "123456789011"
    private val søknadId = UUIDv7.ny()
    private val søknadInnsendtHendelse =
        SøknadInnsendtHendelse(
            søknadId = søknadId,
            ident = ident,
            meldingsreferanseId = søknadId,
            gjelderDato = LocalDate.now(),
            fagsakId = 1,
        )
    private val tidligereOpplysning = Faktum(Opplysningstype.somDesimaltall("tidligere-opplysning"), 1.0)
    private val basertPåBehandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = søknadInnsendtHendelse,
            gjeldendeOpplysninger = Opplysninger(listOf(tidligereOpplysning)),
            tilstand = Behandling.TilstandType.Ferdig,
            sistEndretTilstand = LocalDateTime.now(),
        )
    private val opplysning1 = Faktum(Opplysningstype.somDesimaltall("aktiv-opplysning1"), 1.0)
    private val opplysning2 = Faktum(Opplysningstype.somDesimaltall("aktiv-opplysning2"), 2.0)
    private val opplysning3 = Faktum(Opplysningstype.somBoolsk("aktiv-opplysning3"), false)
    private val behandling =
        Behandling(
            behandler = søknadInnsendtHendelse,
            opplysninger = listOf(opplysning1, opplysning2, opplysning3),
            basertPå = listOf(basertPåBehandling),
        )

    @Test
    fun `lagre og hent behandling fra postgres`() {
        withMigratedDb {
            val behandlingRepositoryPostgres = BehandlingRepositoryPostgres(OpplysningerRepositoryPostgres())
            behandlingRepositoryPostgres.lagre(basertPåBehandling)
            behandlingRepositoryPostgres.lagre(behandling)
            val rehydrertBehandling = behandlingRepositoryPostgres.hentBehandling(behandling.behandlingId).shouldNotBeNull()
            rehydrertBehandling.behandlingId shouldBe behandling.behandlingId
            rehydrertBehandling.basertPå.size shouldBe behandling.basertPå.size

            rehydrertBehandling.basertPå shouldContainExactly behandling.basertPå
            rehydrertBehandling.opplysninger().finnAlle().size shouldBe behandling.opplysninger().finnAlle().size
            rehydrertBehandling.opplysninger().finnAlle() shouldContainExactly behandling.opplysninger().finnAlle()
        }
    }
}

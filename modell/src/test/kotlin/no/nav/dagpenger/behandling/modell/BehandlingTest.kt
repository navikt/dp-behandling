package no.nav.dagpenger.behandling.modell

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BehandlingTest {
    private val ident = "123456789011"
    private val søknadId = UUIDv7.ny()
    private val søknadInnsendtHendelse =
        SøknadInnsendtHendelse(
            søknadId = søknadId,
            ident = ident,
            meldingsreferanseId = søknadId,
            gjelderDato = LocalDate.now(),
        )
    private val tidligereOpplysning = Opplysningstype.somDesimaltall("opplysning-fra-tidligere-behandling")

    @Test
    fun `Behandling basert på tidligere behandlinger`() {
        val behandlingskjede = behandlingskjede(5, søknadInnsendtHendelse)
        behandlingskjede.opplysninger().finnAlle() shouldHaveSize 5
        behandlingskjede.opplysninger().finnAlle().map {
            it.verdi
        } shouldContainExactly listOf(1.0, 2.0, 3.0, 4.0, 5.0)
    }

    private fun behandlingskjede(
        antall: Int,
        hendelse: SøknadInnsendtHendelse,
    ): Behandling {
        var fomTom = LocalDate.now()
        var forrigeBehandling: Behandling? = null
        for (nummer in 1..antall) {
            val behandling =
                Behandling(
                    behandler = hendelse,
                    opplysninger = listOf(Faktum(tidligereOpplysning, nummer.toDouble(), Gyldighetsperiode(fomTom, fomTom))),
                    basertPå = forrigeBehandling?.let { listOf(it) } ?: emptyList(),
                )
            forrigeBehandling = behandling
            // TODO: Det burde eksplodere uten denne
            //  fomTom = fomTom.plusDays(1)
        }
        return forrigeBehandling!!
    }

    @Test
    fun `behandling sender ut behandling opprettet eventer `() {
        val behandling =
            Behandling(
                behandler =
                    søknadInnsendtHendelse.also {
                        søknadInnsendtHendelse.kontekst(it)
                    },
                opplysninger = emptyList(),
            )

        behandling.behandle(søknadInnsendtHendelse)
        søknadInnsendtHendelse.hendelse() shouldHaveSize 1
        val hendelse = søknadInnsendtHendelse.hendelse().first()
        hendelse.type.name shouldBe "behandling_opprettet"
        val kontekst = hendelse.kontekst()
        kontekst.shouldContain("behandlingId", behandling.behandlingId.toString())
        kontekst.shouldContain("søknadId", søknadId.toString())
        kontekst.shouldContain("ident", ident)
    }
}

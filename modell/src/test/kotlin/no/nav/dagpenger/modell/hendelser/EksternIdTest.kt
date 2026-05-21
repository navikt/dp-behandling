package no.nav.dagpenger.modell.hendelser

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class EksternIdTest {
    @Test
    fun `likhet test`() {
        val søknadId = SøknadId(UUIDv7.ny())
        søknadId shouldBeEqual søknadId
        søknadId shouldNotBeEqual SøknadId(UUIDv7.ny())
        søknadId shouldNotBeEqual Any()
        søknadId.hashCode() shouldBeEqual søknadId.hashCode()
        søknadId.hashCode() shouldNotBeEqual SøknadId(UUIDv7.ny()).hashCode()
        søknadId.hashCode() shouldNotBeEqual Any().hashCode()
    }
}

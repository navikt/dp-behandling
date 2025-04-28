package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldContainExactly
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.MinstAv
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class AvhengighetsgrafTest {
    @Test
    fun `skal finne mulige opplysninger basert på regler`() {
        val opplysningstypeA = opplysningstype("A")
        val opplysningstypeB = opplysningstype("B")
        val opplysningstypeC = opplysningstype("C")
        val opplysningstypeD = opplysningstype("D")

        // En opplysning uten regel (tilsvarende fagsakId eller søknadId)
        val opplysningstypeE = opplysningstype("E")

        val regelA = Ekstern(opplysningstypeA, emptyList())
        val regelB = MinstAv(opplysningstypeB, opplysningstypeA)
        val regelC = MinstAv(opplysningstypeC, opplysningstypeB)

        // Denne er ikke en del av treet for C -> B -> A
        val regelD = Ekstern(opplysningstypeD, emptyList())

        val alleRegler = setOf(regelA, regelB, regelC, regelD)
        val opplysninger =
            Opplysninger(
                listOf(
                    Faktum(opplysningstypeA, 2.0),
                    Faktum(opplysningstypeB, 2.0),
                    Faktum(opplysningstypeC, 2.0),
                    Faktum(opplysningstypeD, 2.0),
                    Faktum(opplysningstypeE, 2.0),
                ),
            )

        // Det er opplysningstype C vi ønsker å finne
        val ønsketResultat = listOf(opplysningstypeC)

        // Finn hvilke opplysninger som skal være mulige å ha når ønsket resultat er C
        val vasken = Avhengighetsgraf(alleRegler)
        val resultat = vasken.nødvendigeOpplysninger(opplysninger, ønsketResultat)

        resultat shouldContainExactly setOf(opplysningstypeA, opplysningstypeB, opplysningstypeC, opplysningstypeE)
    }

    private fun opplysningstype(navn: String) = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), navn)
}

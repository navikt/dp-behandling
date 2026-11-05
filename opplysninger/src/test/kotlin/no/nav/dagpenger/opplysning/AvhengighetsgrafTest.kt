package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldContainExactly
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.HvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.MinstAv
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AvhengighetsgrafTest {
    private val sjekk = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Sjekk")
    private val sannInput = opplysningstype("Sann input")
    private val usannInput = opplysningstype("Usann input")
    private val sannGren = opplysningstype("Sann gren")
    private val usannGren = opplysningstype("Usann gren")
    private val valgt = opplysningstype("Valgt")
    private val resultat = opplysningstype("Resultat")
    private val betingetGraf =
        Avhengighetsgraf(
            setOf(
                Ekstern(sjekk, emptyList()),
                Ekstern(sannInput, emptyList()),
                Ekstern(usannInput, emptyList()),
                MinstAv(sannGren, sannInput),
                MinstAv(usannGren, usannInput),
                HvisSannMedResultat(valgt, sjekk, sannGren, usannGren),
                MinstAv(resultat, valgt),
            ),
        )

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
            Opplysninger.med(
                Faktum(opplysningstypeA, 2.0),
                Faktum(opplysningstypeB, 2.0),
                Faktum(opplysningstypeC, 2.0),
                Faktum(opplysningstypeD, 2.0),
                Faktum(opplysningstypeE, 2.0),
            )

        // Det er opplysningstype C vi ønsker å finne
        val ønsketResultat = listOf(opplysningstypeC)

        // Finn hvilke opplysninger som skal være mulige å ha når ønsket resultat er C
        val vasken = Avhengighetsgraf(alleRegler)
        val resultat = vasken.nødvendigeOpplysninger(opplysninger, opplysninger.forDato(5.oktober(2026)), ønsketResultat)

        resultat shouldContainExactly setOf(opplysningstypeA, opplysningstypeB, opplysningstypeC, opplysningstypeE)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `kjent sjekk beholder bare valgt gren og dens avhengigheter selv om grenens verdi mangler`(sjekkverdi: Boolean) {
        val opplysninger = Opplysninger.med(Faktum(sjekk, sjekkverdi))

        val nødvendige =
            betingetGraf.nødvendigeOpplysninger(opplysninger, opplysninger.forDato(5.oktober(2026)), listOf(resultat))

        nødvendige shouldContainExactly
            if (sjekkverdi) {
                setOf(resultat, valgt, sjekk, sannGren, sannInput)
            } else {
                setOf(resultat, valgt, sjekk, usannGren, usannInput)
            }
    }

    @Test
    fun `ukjent sjekk beholder begge grener og deres avhengigheter`() {
        val opplysninger = Opplysninger.med()

        val nødvendige =
            betingetGraf.nødvendigeOpplysninger(opplysninger, opplysninger.forDato(5.oktober(2026)), listOf(resultat))

        nødvendige shouldContainExactly setOf(resultat, valgt, sjekk, sannGren, sannInput, usannGren, usannInput)
    }

    @Test
    fun `grenvalg bruker sjekken på prøvingsdato og bevarer opplysninger uten regel utenfor datoviewet`() {
        val utenRegel = opplysningstype("Uten regel")
        val opplysninger =
            Opplysninger.med(
                Faktum(sjekk, true, Gyldighetsperiode(5.oktober(2026), 2.november(2026))),
                Faktum(sjekk, false, Gyldighetsperiode(3.november(2026))),
                Faktum(utenRegel, 2.0, Gyldighetsperiode(3.november(2026))),
            )

        betingetGraf.nødvendigeOpplysninger(opplysninger, opplysninger.forDato(5.oktober(2026)), listOf(resultat)) shouldContainExactly
            setOf(resultat, valgt, sjekk, sannGren, sannInput, utenRegel)
        betingetGraf.nødvendigeOpplysninger(opplysninger, opplysninger.forDato(3.november(2026)), listOf(resultat)) shouldContainExactly
            setOf(resultat, valgt, sjekk, usannGren, usannInput, utenRegel)
    }

    @Test
    fun `fremtidig sjekk regnes som ukjent før gyldighetsperioden starter`() {
        val opplysninger = Opplysninger.med(Faktum(sjekk, false, Gyldighetsperiode(3.november(2026))))

        val nødvendige =
            betingetGraf.nødvendigeOpplysninger(opplysninger, opplysninger.forDato(5.oktober(2026)), listOf(resultat))

        nødvendige shouldContainExactly setOf(resultat, valgt, sjekk, sannGren, sannInput, usannGren, usannInput)
    }

    private fun opplysningstype(navn: String) = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), navn)
}

package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.februar
import no.nav.dagpenger.opplysning.januar
import no.nav.dagpenger.opplysning.mars
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class AntallAvTest {
    private val barnetype = Opplysningstype.barn(Opplysningstype.Id(UUIDv7.ny(), BarnDatatype), "Barn")
    private val antallBarn = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Antall barn")
    val regelsett =
        vilkår("Antall barn som kvalifiserer") {
            regel(barnetype) { innhentes }
            regel(antallBarn) { antallAv(barnetype) { kvalifiserer } }
        }

    @Test
    fun `teller antall barn som kvalifiser`() {
        val opplysninger = Opplysninger()
        val list =
            BarnListe(
                listOf(
                    Barn(fødselsdato = 1.januar(2020), kvalifiserer = true),
                    Barn(fødselsdato = 1.februar(2020), kvalifiserer = false),
                    Barn(fødselsdato = 1.mars(2020), kvalifiserer = false),
                ),
            )
        opplysninger.leggTil(Faktum(barnetype, list))

        val regelkjøring = Regelkjøring(1.januar(2020), opplysninger, regelsett)
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(antallBarn).verdi shouldBe 1

        val nyListe =
            BarnListe(
                list + Barn(fødselsdato = 1.januar(2020), kvalifiserer = true),
            )

        // Legg til nytt barn
        opplysninger.leggTil(Faktum(barnetype, nyListe)).also {
            regelkjøring.evaluer()
        }
        opplysninger.finnOpplysning(antallBarn).verdi shouldBe 2
    }
}

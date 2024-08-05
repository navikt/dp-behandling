package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.StreikOgLockout
import no.nav.dagpenger.regel.Søknadstidspunkt

class StreikOgLockoutSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(StreikOgLockout.regelsett)
    private val opplysninger = Opplysninger()

    @BeforeStep
    fun kjørRegler() {
        Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søker har søkt om dagpenger under streik eller lockout") {
            opplysninger.leggTil(Faktum(Søknadstidspunkt.søknadstidspunkt, fraDato))
        }

        Og("saksbehandler vurderer at søker {boolsk} i streik eller lockout") { deltar: Boolean ->
            opplysninger.leggTil(Faktum(StreikOgLockout.deltarIStreikOgLockout, deltar))
        }

        Og("saksbehandler vurderer at søker ikke blir {boolsk} av streik eller lockout i samme bedrift") { påvirket: Boolean ->
            opplysninger.leggTil(Faktum(StreikOgLockout.sammeBedriftOgPåvirket, påvirket))
        }

        Så("skal kravet om å ikke være i streik eller lockout være {boolsk}") { utfall: Boolean ->
            val faktum = opplysninger.finnOpplysning(StreikOgLockout.ikkeStreikEllerLockout)
            faktum.verdi shouldBe utfall
        }
    }
}
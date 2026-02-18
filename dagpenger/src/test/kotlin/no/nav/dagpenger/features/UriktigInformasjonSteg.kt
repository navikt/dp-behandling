package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.`prosessvilkår`.Uriktigeopplysninger
import org.junit.jupiter.api.Assertions.assertTrue

class UriktigInformasjonSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(Uriktigeopplysninger.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har {boolsk} uriktige opplysninger") { svar: Boolean ->
            opplysninger
                .leggTil(Faktum<Boolean>(Uriktigeopplysninger.uriktigeOpplysninger, svar) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }
        Gitt("at søker har {boolsk} holder tilbake opplysninger") { svar: Boolean ->
            opplysninger
                .leggTil(Faktum<Boolean>(Uriktigeopplysninger.holderTilbake, svar) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Gitt("at søker har {boolsk} unnlater å etterkomme pålegg") { svar: Boolean ->
            opplysninger
                .leggTil(Faktum<Boolean>(Uriktigeopplysninger.unnlateråEtterkommePålegg, svar) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Så("skal vilkåret for uriktige opplysninger være {boolsk}") { utfall: Boolean ->
            assertTrue(opplysninger.har(Uriktigeopplysninger.oppfyllerVilkårManglendeEllerUriktigeOpplysninger))
            assertTrue(opplysninger.finnOpplysning(Uriktigeopplysninger.oppfyllerVilkårManglendeEllerUriktigeOpplysninger).verdi == utfall)
        }
    }
}

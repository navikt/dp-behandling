package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.testsupport.somLocalDate

class EksportSteg : No {
    private var fraDato = 1.januar(2025)
    private val regelsett = RegelverkDagpenger.regelsettFor(Eksport.oppfyllerVilkårForEksport).toList()
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at eksport av dagpenger skal vurderes fra {string}") { dato: String ->
            fraDato = dato.somLocalDate()
            kjørRegler()
            opplysninger
                .leggTil(
                    Faktum(
                        Rettighetstype.skalEksportVurderes,
                        true,
                        Gyldighetsperiode(dato.somLocalDate()),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Og("at personen registrerer seg i vertslandet {string}") { dato: String ->
            fraDato = dato.somLocalDate()
            kjørRegler()
            opplysninger
                .leggTil(
                    Faktum(
                        Eksport.registrertIVertsland,
                        true,
                        Gyldighetsperiode(dato.somLocalDate()),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Så("skal vilkåret om eksport være oppfylt fra og med {string}") { dato: String ->
            opplysninger.finnOpplysning(Eksport.oppfyllerVilkårForEksport).verdi shouldBe true
            opplysninger.finnOpplysning(Eksport.oppfyllerVilkårForEksport).gyldighetsperiode.fraOgMed shouldBe dato.somLocalDate()
        }

        Så("skal vilkåret om eksport ikke være oppfylt") {
            opplysninger.finnOpplysning(Eksport.oppfyllerVilkårForEksport).verdi shouldBe false
        }
    }
}

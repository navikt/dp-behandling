package no.nav.dagpenger.ferietillegg.features

import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.ferietillegg.FerietilleggBeløp
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg
import no.nav.dagpenger.ferietillegg.RegelverkFerietillegg
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.testsupport.tilBeløp
import java.math.BigDecimal
import java.time.LocalDate

class FerietilleggSteg : No {
    private val regelverksdato = LocalDate.of(2022, 5, 10)
    private val opplysninger = Opplysninger()
    private val regelkjøring =
        Regelkjøring(
            regelverksdato,
            opplysninger,
            opplysningerTilRegelkjøring,
            *RegelverkFerietillegg.regelsett.toTypedArray(),
        )

    init {
        Gitt("at søker har forbrukt {int} dager") { antallDager: Int ->
            opplysninger.leggTil(Faktum(KravPåFerietillegg.antallDagerForbruk, antallDager))
            opplysninger.leggTil(Faktum(KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor, 2022))
            regelkjøring.evaluer()
        }

        Gitt("at søker har utbetalt {string} kroner i opptjeningsåret") { utbetalt: String ->
            opplysninger.leggTil(Faktum(FerietilleggBeløp.sumUtbetaltForÅr, Beløp(BigDecimal(utbetalt))))
            opplysninger.leggTil(Faktum(KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor, 2022))
            regelkjøring.evaluer()
        }

        Så("har søker {boolsk}") { harKravPåFerietillegg: Boolean ->
            opplysninger.finnOpplysning(KravPåFerietillegg.harKravpåFerietillegg).verdi shouldBe harKravPåFerietillegg
        }

        Så("er ferietillegget {string} kroner") { beløp: String ->
            opplysninger.finnOpplysning(FerietilleggBeløp.ferietilleggBeløp).verdi shouldBe beløp.tilBeløp()
        }
    }
}

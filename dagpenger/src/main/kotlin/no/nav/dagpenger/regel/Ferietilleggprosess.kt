package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import java.time.LocalDate

class Ferietilleggprosess : Forretningsprosess(RegelverkFerietillegg) {
    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val regelverksdato = LocalDate.now()

        return Regelkjøring(
            regelverksdato = regelverksdato,
            prøvingsperiode = prøvingsperiode(opplysninger),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = LocalDate.now()

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }

    private fun prøvingsperiode(opplysninger: LesbarOpplysninger): Regelkjøring.Periode {
        val år = opplysninger.kunEgne.finnOpplysning(KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor).verdi
        return Regelkjøring.Periode(
            start = LocalDate.of(år, 1, 1),
            endInclusive = LocalDate.of(år, 12, 31),
        )
    }
}

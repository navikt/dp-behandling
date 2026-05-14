package no.nav.dagpenger.ferietillegg

import no.nav.dagpenger.behandling.modell.hendelser.hendelseTypeOpplysningstype
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg.FerietilleggKontroll
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
        // TODO: Finn en enda riktigere dato?
        val skjedde =
            opplysninger.kunEgne
                .finnOpplysning(hendelseTypeOpplysningstype)
                .gyldighetsperiode.fraOgMed
        return Regelkjøring.Periode(skjedde)
    }

    override fun kontrollpunkter() =
        listOf(
            FerietilleggKontroll,
        )
}

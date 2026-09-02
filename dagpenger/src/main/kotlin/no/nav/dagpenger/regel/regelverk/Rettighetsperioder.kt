package no.nav.dagpenger.regel.regelverk

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.Rettighetsperiodeberegning
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger

internal object Rettighetsperioder : Rettighetsperiodeberegning {
    override fun rettighetsperioder(opplysninger: LesbarOpplysninger): List<Rettighetsperiode> {
        val egne = opplysninger.somListe(LesbarOpplysninger.Filter.Egne)
        return opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett).map { periode ->
            Rettighetsperiode(
                fraOgMed = periode.gyldighetsperiode.fraOgMed,
                tilOgMed = periode.gyldighetsperiode.tilOgMed,
                harRett = periode.verdi,
                endret = egne.contains(periode),
                // Perioden overskriver/omgjør en tidligere, allerede vurdert periode uten rett - altså en
                // stans som nå oppheves, selv om det kalendermessig kan se ut som en ren videreføring.
                opphevetStans = periode.erstatter?.verdi == false,
            )
        }
    }
}

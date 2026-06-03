package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav

class AlderskravPlugin : ProsessPlugin {
    override fun underOpprettelse(kontekst: Prosesskontekst) {
        kontekst.kontekst(this)
        val sisteDagForAldersKrav = kontekst.opplysninger.finnOpplysning(Alderskrav.sisteDagIMåned)
        val meldeperiodeOpplysning = kontekst.opplysninger.finnNullableOpplysning(Beregning.meldeperiode)
        if (meldeperiodeOpplysning != null) {
            if (sisteDagForAldersKrav.verdi in meldeperiodeOpplysning.verdi) {
                val stansdato = sisteDagForAldersKrav.verdi.plusDays(1)
                kontekst.info("Setter ${Alderskrav.kravTilAlder.navn} til false fra og med $stansdato")
                kontekst.opplysninger.leggTil(
                    Faktum(
                        Alderskrav.kravTilAlder,
                        false,
                        Gyldighetsperiode(fraOgMed = stansdato),
                    ),
                )
            }
        }
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst = SpesifikkKontekst("AlderskravPlugin")
}

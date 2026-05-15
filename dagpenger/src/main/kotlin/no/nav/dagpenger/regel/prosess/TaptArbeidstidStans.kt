package no.nav.dagpenger.regel.prosess
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid

/**
 * Stans av dagpenger ved manglende tapt arbeidstid over flere meldeperioder.
 *
 * Dersom bruker ikke oppfyller kravet til tapt arbeidstid i [Beregning.maksAntallPerioderMedIkkeTaptArbeidstid]
 * påfølgende meldeperioder, oppfylles ikke lenger vilkåret om tap av arbeidstid (§ 4-3),
 * og dagpengene stanses fra og med den første meldeperioden som ikke oppfylte kravet.
 */
class TaptArbeidstidStans : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        val opplysninger = kontekst.opplysninger

        val påfølgendeUtenTapt =
            opplysninger
                .finnAlle(Beregning.oppfyllerKravTilTaptArbeidstidIPerioden)
                .sortedBy { it.gyldighetsperiode.fraOgMed }
                .takeLastWhile { !it.verdi }

        val maksAntallPerioder = opplysninger.finnOpplysning(Beregning.maksAntallPerioderMedIkkeTaptArbeidstid)
        if (påfølgendeUtenTapt.size >= maksAntallPerioder.verdi) {
            val stansFraOgMed = påfølgendeUtenTapt.first().gyldighetsperiode.fraOgMed
            val stansperiode = Gyldighetsperiode(stansFraOgMed)

            // Unngå å legge til opplysningen en gang til.
            val harLøpendeRett = opplysninger.finnOpplysning(KravPåDagpenger.harLøpendeRett, stansFraOgMed)
            if (!harLøpendeRett.verdi || harLøpendeRett.kilde is Saksbehandlerkilde) {
                return
            }

            opplysninger.leggTil(
                Faktum(TapAvArbeidsinntektOgArbeidstid.kravTilTaptArbeidstid, false, stansperiode),
            )

            kontekst.beOmRekjøring()
        }
    }
}

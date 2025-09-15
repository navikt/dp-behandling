package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.TidslinjeBygger

class RettighetsperiodePlugin(
    private val regelverk: Regelverk,
) : ProsessPlugin {
    override fun ferdig(opplysninger: Opplysninger) {
        val egne = opplysninger.kunEgne

        // Om saksbehandler har pilla, skal vi ikke overstyre med automatikk
        val harPerioder = egne.har(KravPåDagpenger.harLøpendeRett)
        val harPilla = harPerioder && egne.finnOpplysning(KravPåDagpenger.harLøpendeRett).kilde is Saksbehandlerkilde
        if (harPilla) return

        val vilkår =
            regelverk
                .relevanteVilkår(opplysninger)
                .flatMap { it.utfall }

        val utfall =
            egne
                .somListe()
                .filter { it.opplysningstype in vilkår }
                .filterIsInstance<Opplysning<Boolean>>()

        return TidslinjeBygger(utfall)
            .lagPeriode { påDato ->
                val harVurdertAlle = påDato.map { it.opplysningstype }.containsAll(vilkår)
                if (!harVurdertAlle) return@lagPeriode null

                val alleVilkårOppfylt = påDato.all { it.verdi }
                alleVilkårOppfylt
            }.forEach { periode ->
                val gyldighetsperiode = Gyldighetsperiode(periode.fraOgMed, periode.tilOgMed)
                opplysninger.leggTil(Faktum(KravPåDagpenger.harLøpendeRett, periode.verdi, gyldighetsperiode))
            }
    }
}

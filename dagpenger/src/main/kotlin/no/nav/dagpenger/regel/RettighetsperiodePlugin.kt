package no.nav.dagpenger.regel

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.TidslinjeBygger
import java.time.LocalDate

class RettighetsperiodePlugin(
    private val regelverk: Regelverk,
) : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        val opplysninger = kontekst.opplysninger
        val egne = opplysninger.kunEgne

        // Om saksbehandler har pilla, skal vi ikke overstyre med automatikk
        val harPerioder = egne.har(KravPåDagpenger.harLøpendeRett)
        val harPilla = harPerioder && egne.finnOpplysning(KravPåDagpenger.harLøpendeRett).kilde is Saksbehandlerkilde
        if (harPilla) return

        val vilkår =
            regelverk
                .relevanteVilkår(opplysninger)
                .mapNotNull { it.utfall }

        val utfall =
            opplysninger
                .somListe()
                .filter { it.opplysningstype in vilkår }
                .filterIsInstance<Opplysning<Boolean>>()

        logger.info {
            """RettighetsperiodePlugin beregner rettighetsperiode basert på i
            |vilkår(${vilkår.size}): $vilkår 
            |utfall(${utfall.size}): $utfall
            """.trimMargin()
        }

        // Fjern gamle perioder før vi legger til nye
        egne.finnAlle(KravPåDagpenger.harLøpendeRett).forEach {
            opplysninger.fjern(it.id)
        }

        val eksisterende = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
        return TidslinjeBygger(utfall)
            .lagPeriode { påDato ->
                val harVurdertAlle = påDato.map { it.opplysningstype }.containsAll(vilkår)
                if (!harVurdertAlle) return@lagPeriode null

                val alleVilkårOppfylt = påDato.all { it.verdi }
                alleVilkårOppfylt
            }.forEach { periode ->
                val gyldighetsperiode = Gyldighetsperiode(periode.fraOgMed, periode.tilOgMed)
                require(!periode.fraOgMed.isEqual(LocalDate.MIN)) { "Rettighetsperioder kan ikke begynne fra LocalDate.MIN" }

                // Ikke legg til perioder som har lik fra- og med eksisterende perioder med samme verdi
                // Denne unngår at vi legger til en forkortet rettighetsperiode men lener oss på "uterstatning" logikk i opplysninger.
                if (eksisterende.any {
                        it.gyldighetsperiode.fraOgMed == gyldighetsperiode.fraOgMed && it.verdi == periode.verdi
                    }
                ) {
                    // return@forEach
                }

                opplysninger.leggTil(Faktum(KravPåDagpenger.harLøpendeRett, periode.verdi, gyldighetsperiode))
            }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

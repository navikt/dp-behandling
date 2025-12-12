package no.nav.dagpenger.regel

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.TidslinjeBygger
import java.time.LocalDate

class RettighetsperiodePlugin(
    private val regelverk: Regelverk,
) : ProsessPlugin {
    override fun regelkjøringFerdig(opplysninger: Opplysninger) {
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
            egne
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

        return TidslinjeBygger(utfall)
            .lagPeriode { påDato ->
                val harVurdertAlle = påDato.map { it.opplysningstype }.containsAll(vilkår)
                if (!harVurdertAlle) return@lagPeriode null

                val alleVilkårOppfylt = påDato.all { it.verdi }
                alleVilkårOppfylt
            }.forEach { periode ->
                val gyldighetsperiode = Gyldighetsperiode(periode.fraOgMed, periode.tilOgMed)
                require(!periode.fraOgMed.isEqual(LocalDate.MIN)) { "Rettighetsperioder kan ikke begynne fra LocalDate.MIN" }

                opplysninger.leggTil(Faktum(KravPåDagpenger.harLøpendeRett, periode.verdi, gyldighetsperiode))
            }.also {
                // Automatisk setter prøvingsdato til første mulige innvilgelse
                egne.finnAlle(KravPåDagpenger.harLøpendeRett).firstOrNull { it.verdi }?.let {
                    val gjeldende = opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato)
                    if (gjeldende.verdi.isEqual(it.gyldighetsperiode.fraOgMed)) return@let

                    opplysninger.leggTil(
                        Faktum(
                            Søknadstidspunkt.prøvingsdato,
                            it.gyldighetsperiode.fraOgMed,
                            Gyldighetsperiode(it.gyldighetsperiode.fraOgMed),
                        ),
                    )
                }
            }
    }
}

private val logger = KotlinLogging.logger {}

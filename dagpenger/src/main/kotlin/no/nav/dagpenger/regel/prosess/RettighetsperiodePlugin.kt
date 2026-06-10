package no.nav.dagpenger.regel.prosess

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.PeriodisertVerdi
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.TidslinjeBygger
import no.nav.dagpenger.opplysning.Utledning
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger

fun interface PeriodeOverskrivingsStrategi {
    fun skalIkkeLeggesTil(
        eksisterende: List<Opplysning<Boolean>>,
        gyldighetsperiode: Gyldighetsperiode,
        periode: PeriodisertVerdi<Boolean>,
    ): Boolean

    companion object {
        val BEHOLD_EKSISTERENDE =
            PeriodeOverskrivingsStrategi { eksisterende, gyldighetsperiode, periode ->
                eksisterende.any { it.gyldighetsperiode.fraOgMed == gyldighetsperiode.fraOgMed && it.verdi == periode.verdi }
            }
        val OVERSKRIV_ALLTID = PeriodeOverskrivingsStrategi { _, _, _ -> false }
    }
}

class RettighetsperiodePlugin(
    private val regelverk: Regelverk,
    private val overskrivingsStrategi: PeriodeOverskrivingsStrategi = PeriodeOverskrivingsStrategi.BEHOLD_EKSISTERENDE,
    private val slåSammenLike: Boolean = true,
) : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        kontekst.kontekst(this)
        val opplysninger = kontekst.opplysninger
        val egne = opplysninger.kunEgne

        // Om saksbehandler eller hendelse har pilla, skal vi ikke overstyre med automatikk
        val harPerioder = egne.har(KravPåDagpenger.harLøpendeRett)
        val harPilla = harPerioder && egne.finnOpplysning(KravPåDagpenger.harLøpendeRett).kilde != null
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

        // Fjern gamle perioder før vi legger til nye
        egne.finnAlle(KravPåDagpenger.harLøpendeRett).forEach {
            opplysninger.fjern(it.id)
        }

        val eksisterende = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
        return TidslinjeBygger(utfall)
            .lagPeriode(slåSammenLike) { påDato ->
                val harVurdertAlle = påDato.map { it.opplysningstype }.containsAll(vilkår)
                if (!harVurdertAlle) return@lagPeriode null

                val alleVilkårOppfylt = påDato.all { it.verdi }
                alleVilkårOppfylt
            }.forEach { periode ->
                val gyldighetsperiode = Gyldighetsperiode(periode.fraOgMed, periode.tilOgMed)
                require(gyldighetsperiode.harStartdato) { "Rettighetsperioder kan ikke begynne fra LocalDate.MIN" }

                // Ikke legg til perioder som har lik fra- og med eksisterende perioder med samme verdi
                // Denne unngår at vi legger til en forkortet rettighetsperiode men lener oss på "uterstatning" logikk i opplysninger.
                if (overskrivingsStrategi.skalIkkeLeggesTil(eksisterende, gyldighetsperiode, periode)) {
                    return@forEach
                }

                loggVilkårsvurdering(vilkår, utfall, kontekst)

                opplysninger.leggTil(
                    Faktum(
                        KravPåDagpenger.harLøpendeRett,
                        periode.verdi,
                        gyldighetsperiode,
                        Utledning(this.javaClass.simpleName, utfall),
                    ),
                )
            }
    }

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst(
            "RettighetsperiodePlugin",
        )

    private fun loggVilkårsvurdering(
        vilkår: List<Opplysningstype<Boolean>>,
        utfall: List<Opplysning<Boolean>>,
        kontekst: Prosesskontekst,
    ) {
        val vurderte = utfall.associateBy { it.opplysningstype }
        val alleOppfylt = vurderte.size == vilkår.size && vurderte.values.all { it.verdi }
        val antallOppfylt = vurderte.values.count { it.verdi }
        val antallIkkeOppfylt = vurderte.values.count { !it.verdi }
        val antallMangler = vilkår.size - vurderte.size

        val oppsummering =
            buildString {
                append("Rettighetsperiode: ${vilkår.size} vilkår vurdert")
                if (alleOppfylt) {
                    append(", alle oppfylt")
                } else {
                    if (antallOppfylt > 0) append(", $antallOppfylt oppfylt")
                    if (antallIkkeOppfylt > 0) append(", $antallIkkeOppfylt ikke oppfylt")
                    if (antallMangler > 0) append(", $antallMangler mangler vurdering")
                }
            }
        kontekst.info(oppsummering)

        vilkår.forEach { vilkårType ->
            val vurdering = vurderte[vilkårType]
            val linje =
                when {
                    vurdering == null -> "⊘ $vilkårType (mangler vurdering)"
                    vurdering.verdi -> "✓ $vilkårType (${vurdering.gyldighetsperiode})"
                    else -> "✗ $vilkårType (${vurdering.gyldighetsperiode})"
                }
            kontekst.info(linje)
        }

        logger.info { oppsummering }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

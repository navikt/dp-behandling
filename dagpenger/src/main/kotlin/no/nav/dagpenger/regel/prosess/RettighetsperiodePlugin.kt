package no.nav.dagpenger.regel.prosess

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
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
    @WithSpan("RettighetsperiodePlugin.regelkjøringFerdig")
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        kontekst.kontekst(this)
        val opplysninger = kontekst.opplysninger
        val egne = opplysninger.kunEgne

        if (harSaksbehandlerEllerHendelsePillet(egne)) return

        val vilkår = regelverk.relevanteVilkår(opplysninger).mapNotNull { it.utfall }
        val utfall = finnVurdertUtfall(opplysninger, vilkår)

        fjernEgneRettighetsperioder(opplysninger, egne)

        val eksisterende = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
        val kandidatperioder =
            TidslinjeBygger(utfall).lagPeriode(slåSammenLike) { påDato -> alleVilkårOppfylt(vilkår, påDato) }

        RettighetsperiodeUtleder
            .utledNyeRettighetsperioder(eksisterende, kandidatperioder, overskrivingsStrategi)
            .forEach { nyPeriode ->
                loggVilkårsvurdering(vilkår, utfall, kontekst)
                opplysninger.leggTil(
                    Faktum(
                        KravPåDagpenger.harLøpendeRett,
                        nyPeriode.verdi,
                        nyPeriode.gyldighetsperiode,
                        Utledning(this.javaClass.simpleName, utfall),
                    ),
                )
            }
    }

    // Om saksbehandler eller hendelse har pilla, skal vi ikke overstyre med automatikk
    private fun harSaksbehandlerEllerHendelsePillet(egne: LesbarOpplysninger): Boolean {
        val harPerioder = egne.har(KravPåDagpenger.harLøpendeRett)
        return harPerioder && egne.finnOpplysning(KravPåDagpenger.harLøpendeRett).kilde != null
    }

    private fun finnVurdertUtfall(
        opplysninger: LesbarOpplysninger,
        vilkår: List<Opplysningstype<Boolean>>,
    ): List<Opplysning<Boolean>> =
        opplysninger
            .somListe()
            .filter { it.opplysningstype in vilkår }
            .filterIsInstance<Opplysning<Boolean>>()

    private fun alleVilkårOppfylt(
        vilkår: List<Opplysningstype<Boolean>>,
        påDato: Collection<Opplysning<Boolean>>,
    ): Boolean? {
        val harVurdertAlle = påDato.map { it.opplysningstype }.containsAll(vilkår)
        if (!harVurdertAlle) return null
        return påDato.all { it.verdi }
    }

    // Vi bygger hele tidslinja av rettighetsperioder på nytt for hver regelkjøring, så gamle egne
    // perioder må fjernes først - ellers vil de henge igjen som duplikater ved siden av de nye.
    private fun fjernEgneRettighetsperioder(
        opplysninger: Opplysninger,
        egne: LesbarOpplysninger,
    ) {
        egne.finnAlle(KravPåDagpenger.harLøpendeRett).forEach {
            opplysninger.fjern(it.id)
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
                    if (antallIkkeOppfylt > 0) {
                        append(
                            ", $antallIkkeOppfylt ikke oppfylt. Vilkår som ikke er oppfyllt(${
                                vurderte.filter { !it.value.verdi }.keys.joinToString(
                                    ", ",
                                )
                            }).",
                        )
                    }
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

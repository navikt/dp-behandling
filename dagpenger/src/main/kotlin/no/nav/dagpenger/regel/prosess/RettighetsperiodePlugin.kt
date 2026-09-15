package no.nav.dagpenger.regel.prosess

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
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
import java.time.LocalDate

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
        // Vi trenger siste eksisterende periode for å avgjøre om neste periode er kant-i-kant med den
        var forrige = eksisterende.maxByOrNull { it.gyldighetsperiode.fraOgMed }

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

                // Om det lages en ny opplysning med samme verdi som ligger kant-i-kant med forrige verdi skal vi bare utvide
                // rettighetsperioden inne i denne behandlingen, ikke legge til duplikat
                val skalSlåsSammenMedForrige = erTilstøtendeMedLikVerdi(forrige, gyldighetsperiode, periode.verdi)
                val faktiskGyldighetsperiode =
                    if (skalSlåsSammenMedForrige) {
                        Gyldighetsperiode(forrige!!.gyldighetsperiode.fraOgMed, gyldighetsperiode.tilOgMed)
                    } else {
                        gyldighetsperiode
                    }

                loggVilkårsvurdering(vilkår, utfall, kontekst)

                val nyPeriode =
                    Faktum(
                        KravPåDagpenger.harLøpendeRett,
                        periode.verdi,
                        faktiskGyldighetsperiode,
                        Utledning(this.javaClass.simpleName, utfall),
                    )
                opplysninger.leggTil(nyPeriode)

                // Oppdater forrige til den nyeste perioden vi har lagt til, slik at neste periode kan avgjøre om den er kant-i-kant med denne.
                forrige = nyPeriode
            }
    }

    /**
     * Er `forrige` kant-i-kant med den nye, åpne perioden, og har samme verdi? Vi ser bevisst bare på
     * den ene forrige perioden - ikke alle eksisterende - slik at vi aldri hopper bakover forbi en
     * reell historisk overgang (f.eks. en stans) og smelter sammen med en eldre periode som
     * tilfeldigvis også grenser til samme dato.
     */
    private fun erTilstøtendeMedLikVerdi(
        forrige: Opplysning<Boolean>?,
        gyldighetsperiode: Gyldighetsperiode,
        verdi: Boolean,
    ): Boolean {
        if (forrige == null) return false
        if (gyldighetsperiode.tilOgMed != LocalDate.MAX) return false
        return forrige.verdi == verdi && forrige.gyldighetsperiode.tilstøter(gyldighetsperiode)
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

package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Utledning
import kotlin.collections.singleOrNull

abstract class Regel<T : Comparable<T>> internal constructor(
    internal val produserer: Opplysningstype<T>,
    internal val avhengerAv: List<Opplysningstype<*>> = emptyList(),
) {
    init {
        require(avhengerAv.none { it == produserer }) {
            "Regel ${this::class.java.simpleName} kan ikke produsere samme opplysning ${produserer.navn} den er avhengig av"
        }
    }

    internal open fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: MutableSet<Regel<*>>,
        gjeldendeRegler: List<Regel<*>>,
    ) {
        if (opplysninger.har(produserer)) {
            val produkt = opplysninger.finnOpplysning(produserer)
            if (produkt.utledetAv == null) {
                return
            }

            // Underliggende opplysninger må vurdere seg selv
            produkt.utledetAv.opplysninger.forEach { avhengighet ->
                val produsent = gjeldendeRegler.singleOrNull { it.produserer(avhengighet.opplysningstype) }
                produsent?.lagPlan(opplysninger, plan, gjeldendeRegler)
            }

            // Sjekk om regelen har fått nye avhengigheter
            val regelForProdukt = gjeldendeRegler.single { it.produserer(produkt.opplysningstype) }
            if (regelForProdukt.avhengerAv != produkt.utledetAv.opplysninger.map { it.opplysningstype }) {
                regelForProdukt.avhengerAv.map { avhengighet ->
                    val produsent = gjeldendeRegler.singleOrNull { it.produserer(avhengighet) }
                    produsent?.lagPlan(opplysninger, plan, gjeldendeRegler)
                }
                plan.add(this)
                return
            }

            // Om en avhengighet er erstattet, må denne regelen kjøres på nytt
            if (produkt.utledetAv.opplysninger.any { it.erErstattet || it.erFjernet }) {
                plan.add(this)
                return
            }
        } else {
            val avhengigheter = opplysninger.finnAlle(avhengerAv)

            if (avhengigheter.size == avhengerAv.size) {
                plan.add(this)
                return
            } else {
                avhengerAv.forEach { avhengighet ->
                    val produsent =
                        gjeldendeRegler.singleOrNull { it.produserer(avhengighet) }
                            ?: throw IllegalStateException("Fant ikke produsent for $avhengighet")
                    produsent.lagPlan(opplysninger, plan, gjeldendeRegler)
                }
            }
        }
        return
    }

    abstract override fun toString(): String

    protected abstract fun kjør(opplysninger: LesbarOpplysninger): T

    fun produserer(opplysningstype: Opplysningstype<*>) = produserer.er(opplysningstype)

    internal fun lagProdukt(opplysninger: LesbarOpplysninger): Opplysning<T> {
        if (avhengerAv.isEmpty()) return Faktum(produserer, kjør(opplysninger))

        val basertPå = opplysninger.finnAlle(avhengerAv)
        requireAlleAvhengigheter(basertPå)

        val erAlleFaktum = basertPå.all { it is Faktum<*> }
        val utledetAv = Utledning(this, basertPå)
        val gyldig =
            Gyldighetsperiode(
                fom = basertPå.maxOf { it.gyldighetsperiode.fom },
                tom = basertPå.minOf { it.gyldighetsperiode.tom },
            )
        return when (erAlleFaktum) {
            true -> Faktum(opplysningstype = produserer, verdi = kjør(opplysninger), utledetAv = utledetAv, gyldighetsperiode = gyldig)
            false ->
                Hypotese(
                    opplysningstype = produserer,
                    verdi = kjør(opplysninger),
                    utledetAv = utledetAv,
                    gyldighetsperiode = gyldig,
                )
        }
    }

    private fun requireAlleAvhengigheter(basertPå: List<Opplysning<*>>) =
        require(basertPå.size == avhengerAv.size) {
            val manglerAvhengigheter = avhengerAv.toSet() - basertPå.map { it.opplysningstype }.toSet()
            """
            Prøver å kjøre ${this::class.simpleName}($produserer), men mangler avhengigheter.
            Det er mismatch mellom lagPlan() og lagProdukt().
            - Avhengigheter vi mangler: ${manglerAvhengigheter.joinToString { it.id }}
            - Avhengigheter vi trenger: ${avhengerAv.joinToString { it.id }}
            - Avhengigheter vi fant: ${basertPå.joinToString { it.opplysningstype.id }}
            """.trimIndent()
        }
}

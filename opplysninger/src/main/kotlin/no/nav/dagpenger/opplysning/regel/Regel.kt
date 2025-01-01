package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Utledning

abstract class Regel<T : Comparable<T>> internal constructor(
    internal val produserer: Opplysningstype<T>,
    internal val avhengerAv: List<Opplysningstype<*>> = emptyList(),
) {
    internal open fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: MutableSet<Regel<*>>,
        produsenter: Map<Opplysningstype<*>, Regel<*>>,
    ) {
        if (plan.contains(this)) return

        if (opplysninger.har(produserer)) {
            val produkt = opplysninger.finnOpplysning(produserer)
            if (produkt.utledetAv == null) {
                return
            }

            produkt.utledetAv.opplysninger.forEach { avhengighet ->
                val produsent = produsenter[avhengighet.opplysningstype]
                produsent?.lagPlan(opplysninger, plan, produsenter)
            }

            val avhengighetErErstattet = produkt.utledetAv.opplysninger.any { it.erErstattet || it.erFjernet }

            if (avhengighetErErstattet) {
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
                    val produsent = produsenter[avhengighet] ?: throw IllegalStateException("Fant ikke produsent for $avhengighet")
                    produsent.lagPlan(opplysninger, plan, produsenter)
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

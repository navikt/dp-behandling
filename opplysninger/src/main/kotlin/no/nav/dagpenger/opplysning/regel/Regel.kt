package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Utledning
import java.time.LocalDate

abstract class Regel<T : Comparable<T>> internal constructor(
    internal val produserer: Opplysningstype<T>,
    // todo: Bør dette være et Set? Vi er ikke avhengig av rekkefølge
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
        produsenter: Map<Opplysningstype<*>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        if (besøkt.contains(this)) return else besøkt.add(this)

        if (opplysninger.har(produserer)) {
            val produkt = opplysninger.finnOpplysning(produserer)
            if (produkt.utledetAv == null) {
                // Opplysningen er ikke utledet av noe ELLER overstyrt av saksbehandler
                return
            }

            val (erErstattet, ikkeErstattet) = produkt.utledetAv.opplysninger.partition { opplysninger.erErstattet(listOf(it)) }

            // Sjekk om produktet er basert på utdatert informasjon
            val utdaterteOpplysninger = ikkeErstattet.filter { it.erUtdatert }
            if (utdaterteOpplysninger.isNotEmpty()) {
                // Minst en av opplysningene som regelen er avhengig av er utdatert, regelen skal kjøres på nytt
                utdaterteOpplysninger.forEach { utdatert ->
                    val produsent =
                        produsenter[utdatert.opplysningstype] ?: return
                    // ?: throw IllegalStateException("FanVt ikke produsent for $utdatert")

                    produsent.lagPlan(opplysninger, plan, produsenter, besøkt)
                }

                if (this is Ekstern<*>) {
                    plan.add(this)
                }

                return
            }

            // Sjekk om produktet er basert på erstattet informasjon
            if (erErstattet.isNotEmpty()) {
                // Minst en avhengighet er erstattet, må de regelen skal kjøres på nytt
                plan.add(this)
                return
            }

            // Sjekk om regelen har fått nye avhengigheter
            if (harRegelNyeAvhengigheter(produkt.utledetAv)) {
                val mangler = avhengerAv.filter { opplysninger.mangler(it) }
                if (mangler.isNotEmpty()) {
                    mangler.forEach { avhengighet ->
                        // Om en avhengighet mangler, må de regelene kjøres på nytt
                        val avhengigRegel = produsenter[avhengighet]
                        avhengigRegel?.lagPlan(opplysninger, plan, produsenter, besøkt)
                    }
                    // Manger vi noen avhengigheter, så kan vi ikke kjøre denne regelen på nytt enda
                    return
                }

                // Om alle avhengigheter er tilstede, skal denne regelen kjøres på nytt
                plan.add(this)
                return
            }
        } else {
            val avhengigheter = opplysninger.finnFlere(avhengerAv)

            if (avhengigheter.size == avhengerAv.size) {
                plan.add(this)
            } else {
                avhengerAv.forEach { avhengighet ->
                    val produsent =
                        produsenter[avhengighet]
                    if (produsent == null) {
                        manglendeRegler.add(avhengighet)
                        return@forEach
                    } // throw IllegalStateException("Fant ikke produsent for $avhengighet")
                    produsent.lagPlan(opplysninger, plan, produsenter, besøkt)
                }
            }
        }
    }

    private fun harRegelNyeAvhengigheter(utledetAv: Utledning) =
        avhengerAv.toSet() != utledetAv.opplysninger.map { it.opplysningstype }.toSet()

    abstract override fun toString(): String

    protected abstract fun kjør(opplysninger: LesbarOpplysninger): T

    fun produserer(opplysningstype: Opplysningstype<*>) = produserer.er(opplysningstype)

    internal fun lagProdukt(opplysninger: LesbarOpplysninger): Opplysning<T> {
        if (avhengerAv.isEmpty()) {
            val produkt = kjør(opplysninger)
            return Faktum(produserer, produkt)
        }

        val basertPå = opplysninger.finnFlere(avhengerAv)

        val produkt = kjør(opplysninger)
        val erAlleFaktum = basertPå.all { it is Faktum<*> }
        val utledetAv = Utledning(this, basertPå)
        val gyldighetsperiode = produserer.gyldighetsperiode(produkt, basertPå)

        return when (erAlleFaktum) {
            true -> Faktum(opplysningstype = produserer, verdi = produkt, utledetAv = utledetAv, gyldighetsperiode = gyldighetsperiode)
            false -> Hypotese(opplysningstype = produserer, verdi = produkt, utledetAv = utledetAv, gyldighetsperiode = gyldighetsperiode)
        }
    }
}

fun interface GyldighetsperiodeStrategi<T> {
    companion object {
        private val minsteMulige =
            GyldighetsperiodeStrategi<Any> { _, basertPå ->
                if (basertPå.isEmpty()) return@GyldighetsperiodeStrategi Gyldighetsperiode()
                Gyldighetsperiode(
                    fraOgMed = basertPå.maxOf { it.gyldighetsperiode.fraOgMed },
                    tilOgMed = basertPå.minOf { it.gyldighetsperiode.tilOgMed },
                )
            }
        val egenVerdi = GyldighetsperiodeStrategi<LocalDate> { produkt, _ -> Gyldighetsperiode(fom = produkt) }

        @Suppress("UNCHECKED_CAST")
        fun <P> minsteMulige() = minsteMulige as GyldighetsperiodeStrategi<P>
    }

    fun gyldighetsperiode(
        produkt: T,
        basertPå: List<Opplysning<*>>,
    ): Gyldighetsperiode
}

val manglendeRegler = mutableSetOf<Opplysningstype<*>>()

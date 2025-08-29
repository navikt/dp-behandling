package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Utledning
import java.time.LocalDate
import java.time.LocalDateTime

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

            // Sjekk om regelen har fått nye avhengigheter
            val regelForProdukt = produsenter[produkt.opplysningstype]
            if (harRegelNyeAvhengigheter(regelForProdukt, produkt.utledetAv) ||
                opplysninger.erErstattet(produkt.utledetAv.opplysninger) ||
                utledetAvErEndret(produkt.opprettet, produkt.utledetAv)
            ) {
                // Om en avhengighet mangler, må denne regelen kjøres på nytt
                if (regelForProdukt?.avhengerAv?.any { opplysninger.mangler(it) } == true) {
                    regelForProdukt.avhengerAv.map { avhengighet ->
                        val avhengigRegel = produsenter[avhengighet]
                        avhengigRegel?.lagPlan(opplysninger, plan, produsenter, besøkt)
                    }
                } else {
                    // Om alle avhengigheter er tilstede, må denne regelen kjøres på nytt
                    plan.add(this)
                }
            }
        } else {
            val avhengigheter = opplysninger.finnFlere(avhengerAv)

            if (avhengigheter.size == avhengerAv.size) {
                plan.add(this)
            } else {
                avhengerAv.forEach { avhengighet ->
                    val produsent = produsenter[avhengighet] ?: throw IllegalStateException("Fant ikke produsent for $avhengighet")
                    produsent.lagPlan(opplysninger, plan, produsenter, besøkt)
                }
            }
        }
    }

    private fun utledetAvErEndret(
        sistEndret: LocalDateTime,
        utledetAv: Utledning,
    ) = utledetAv.opplysninger.any {
        it.opprettet.isAfter(sistEndret)
    }

    private fun harRegelNyeAvhengigheter(
        regelForProdukt: Regel<*>?,
        utledetAv: Utledning,
    ) = regelForProdukt?.avhengerAv?.toSet() !=
        utledetAv.opplysninger
            .map { it.opplysningstype }
            .toSet()

    abstract override fun toString(): String

    protected abstract fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): T

    fun produserer(opplysningstype: Opplysningstype<*>) = produserer.er(opplysningstype)

    internal fun lagProdukt(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Opplysning<T> {
        if (avhengerAv.isEmpty()) {
            val produkt = kjør(opplysninger, prøvingsdato)
            return Faktum(produserer, produkt, Gyldighetsperiode(fom = prøvingsdato))
        }

        val basertPå = opplysninger.finnFlere(avhengerAv)
        requireAlleAvhengigheter(basertPå)

        val produkt = kjør(opplysninger, prøvingsdato)
        val erAlleFaktum = basertPå.all { it is Faktum<*> }
        val utledetAv = Utledning(this, basertPå)
        val gyldighetsperiode = produserer.gyldighetsperiode(produkt, basertPå)

        // if (gyldighetsperiode.fom.isBefore(prøvingsdato)) throw IllegalStateException("BOOM")
        // val blurp = Gyldighetsperiode(fom = maxOf(gyldighetsperiode.fom, prøvingsdato), tom = gyldighetsperiode.tom)

        return when (erAlleFaktum) {
            true -> Faktum(opplysningstype = produserer, verdi = produkt, utledetAv = utledetAv, gyldighetsperiode = gyldighetsperiode)
            false -> Hypotese(opplysningstype = produserer, verdi = produkt, utledetAv = utledetAv, gyldighetsperiode = gyldighetsperiode)
        }
    }

    private fun requireAlleAvhengigheter(basertPå: List<Opplysning<*>>) =
        require(basertPå.size == avhengerAv.size) {
            val manglerAvhengigheter = avhengerAv.toSet() - basertPå.map { it.opplysningstype }.toSet()
            """
            Prøver å kjøre ${this::class.simpleName}($produserer), men mangler avhengigheter.
            Det er mismatch mellom lagPlan() og lagProdukt().
            - Avhengigheter vi mangler: ${manglerAvhengigheter.joinToString { it.behovId }}
            - Avhengigheter vi trenger: ${avhengerAv.joinToString { it.behovId }}
            - Avhengigheter vi fant: ${basertPå.joinToString { it.opplysningstype.behovId }}
            """.trimIndent()
        }
}

fun interface GyldighetsperiodeStrategi<T> {
    companion object {
        private val minsteMulige =
            GyldighetsperiodeStrategi<Any> { _, basertPå ->
                if (basertPå.isEmpty()) return@GyldighetsperiodeStrategi Gyldighetsperiode()
                Gyldighetsperiode(
                    fom = basertPå.maxOf { it.gyldighetsperiode.fom },
                    tom = basertPå.minOf { it.gyldighetsperiode.tom },
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

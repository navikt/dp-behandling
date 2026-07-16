package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelplanlegger
import no.nav.dagpenger.opplysning.Utledning
import java.time.LocalDate

abstract class Regel<T : Any> internal constructor(
    internal val produserer: Opplysningstype<T>,
    // todo: Bør dette være et Set? Vi er ikke avhengig av rekkefølge
    internal val avhengerAv: List<Opplysningstype<out Any>> = emptyList(),
) {
    init {
        require(avhengerAv.none { it == produserer }) {
            "Regel ${this::class.java.simpleName} kan ikke produsere samme opplysning ${produserer.navn} den er avhengig av"
        }
    }

    internal open fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        if (besøkt.contains(this)) return else besøkt.add(this)

        val produkt = opplysninger.finnNullableOpplysning(produserer)
        when {
            produkt == null -> {
                lagPlanNårProduktMangler(opplysninger, plan, produsenter, besøkt)
            }

            produkt.utledetAv != null -> {
                lagPlanFraUtledning(produkt.utledetAv, opplysninger, plan, produsenter, besøkt)
            }

            else -> {
                // opplysningen er ikke utledet av noe ELLER overstyrt av saksbehandler
            }
        }
    }

    private fun lagPlanNårProduktMangler(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
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

    private fun lagPlanNårUtledereErUtdaterte(
        utdaterteOpplysninger: List<Opplysning<*>>,
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        // Minst en av opplysningene som regelen er avhengig av er utdatert, regelen skal kjøres på nytt
        utdaterteOpplysninger.forEach { utdatert ->
            val produsent =
                produsenter[utdatert.opplysningstype] ?: return

            produsent.lagPlan(opplysninger, plan, produsenter, besøkt)
        }
    }

    private fun lagPlanNårRegelenHarFåttNyeAvhengigheter(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        val mangler = avhengerAv.filter { opplysninger.mangler(it) }

        // Om alle avhengigheter er tilstede, skal denne regelen kjøres på nytt
        if (mangler.isEmpty()) return plan.add(this)

        // Manger vi noen avhengigheter, så kan vi ikke kjøre denne regelen på nytt enda
        mangler.forEach { avhengighet ->
            // Om en avhengighet mangler, må de regelene kjøres på nytt
            val avhengigRegel = produsenter[avhengighet]
            avhengigRegel?.lagPlan(opplysninger, plan, produsenter, besøkt)
        }
    }

    private fun lagPlanFraUtledning(
        utledning: Utledning,
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        val (erErstattet, ikkeErstattet) = utledning.opplysninger.partition { opplysninger.erErstattet(listOf(it)) }
        val utdaterteOpplysninger = ikkeErstattet.filter { it.erUtdatert }

        when {
            // Sjekk om produktet er basert på utdatert informasjon
            utdaterteOpplysninger.isNotEmpty() -> {
                lagPlanNårUtledereErUtdaterte(
                    utdaterteOpplysninger,
                    opplysninger,
                    plan,
                    produsenter,
                    besøkt,
                )
            }

            // Sjekk om produktet er basert på erstattet informasjon
            erErstattet.isNotEmpty() -> {
                lagPlanNårAvhengerErErstattet(opplysninger, plan, produsenter, besøkt)
            }

            // Sjekk om regelen har fått nye avhengigheter
            harRegelNyeAvhengigheter(utledning) -> {
                lagPlanNårRegelenHarFåttNyeAvhengigheter(opplysninger, plan, produsenter, besøkt)
            }
        }
    }

    private fun harRegelNyeAvhengigheter(utledetAv: Utledning) =
        avhengerAv.toSet() != utledetAv.opplysninger.map { it.opplysningstype }.toSet()

    protected open fun lagPlanNårAvhengerErErstattet(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        plan.add(this)
    }

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
        private val størsteMulige =
            GyldighetsperiodeStrategi<Any> { _, basertPå ->
                if (basertPå.isEmpty()) return@GyldighetsperiodeStrategi Gyldighetsperiode()
                Gyldighetsperiode(
                    fraOgMed = basertPå.minOf { it.gyldighetsperiode.fraOgMed },
                    tilOgMed = basertPå.maxOf { it.gyldighetsperiode.tilOgMed },
                )
            }
        val egenVerdi = GyldighetsperiodeStrategi<LocalDate> { produkt, _ -> Gyldighetsperiode(fom = produkt) }

        @Suppress("UNCHECKED_CAST")
        fun <P> minsteMulige() = minsteMulige as GyldighetsperiodeStrategi<P>

        @Suppress("UNCHECKED_CAST")
        fun <P> størsteMulige() = størsteMulige as GyldighetsperiodeStrategi<P>

        fun <P> basertPå(opplysningstype: Opplysningstype<LocalDate>) =
            GyldighetsperiodeStrategi<P> { _, basertPå ->
                val dato = basertPå.single { it.opplysningstype == opplysningstype }.verdi
                require(dato is LocalDate) { "Opplysningstype som skal brukes til å utlede gyldighetsperiode må være LocalDate" }
                Gyldighetsperiode(dato)
            }

        // Arver hele gyldighetsperioden til en navngitt avhengighet, slått opp på opplysningstype
        // (ikke posisjon i avhengerAv-listen). Brukes når en opplysning skal "låne" gyldighetsperioden
        // til én spesifikk avhengighet, uavhengig av hvilke andre opplysninger den også er avhengig av.
        fun <P> arvFra(opplysningstype: Opplysningstype<*>) =
            GyldighetsperiodeStrategi<P> { _, basertPå ->
                basertPå.single { it.opplysningstype == opplysningstype }.gyldighetsperiode
            }

        // Som arvFra, men begrenser tilOgMed til det minste tilOgMed blant *alle* avhengigheter
        // (tilsvarende minsteMulige()). Uten denne begrensningen vil et resultat som strekker seg
        // uendelig frem i tid forstyrre regelmotorens fallback for å utlede prøvingsdato for senere
        // regelkjøringer (se Regelkjøring.medGyldighetsperiode/sisteTilgjengeligeDato), siden en
        // ubegrenset gyldighetsperiode aldri "avsluttes" ved et nytt faktum lenger frem i tid.
        fun <P> arvFraMedGrense(opplysningstype: Opplysningstype<*>) =
            GyldighetsperiodeStrategi<P> { _, basertPå ->
                Gyldighetsperiode(
                    fraOgMed = basertPå.single { it.opplysningstype == opplysningstype }.gyldighetsperiode.fraOgMed,
                    tilOgMed = basertPå.minOf { it.gyldighetsperiode.tilOgMed },
                )
            }

        // Arver gyldighetsperioden til den grenen (hvisSann/hvisUsann) som faktisk ble valgt av
        // en tilhørende hvisSannMedResultat-regel, slått opp på opplysningstype (ikke posisjon i
        // avhengerAv-listen). Brukes når selve resultatverdien skal "låne" gyldighetsperioden til
        // den valgte grenen, i stedet for en periode utledet fra alle avhengighetene samlet.
        fun <P : Any> arvFraValgtGren(
            sjekk: Opplysningstype<Boolean>,
            hvisSann: Opplysningstype<P>,
            hvisUsann: Opplysningstype<P>,
        ) = GyldighetsperiodeStrategi<P> { _, basertPå ->
            val sjekkVerdi = basertPå.single { it.opplysningstype == sjekk }.verdi as Boolean
            val valgtGren = if (sjekkVerdi) hvisSann else hvisUsann
            basertPå.single { it.opplysningstype == valgtGren }.gyldighetsperiode
        }
    }

    fun gyldighetsperiode(
        produkt: T,
        basertPå: List<Opplysning<*>>,
    ): Gyldighetsperiode
}

val manglendeRegler = mutableSetOf<Opplysningstype<*>>()

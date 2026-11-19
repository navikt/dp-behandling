package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Forutsetning
import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.oppfylt
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.vurdert
import java.time.LocalDate

abstract class RegelsettBuilderBase(
    val hjemmel: Hjemmel,
    protected val type: RegelsettType,
) {
    protected val regler: MutableMap<Opplysningstype<*>, TemporalCollection<Regel<*>>> = mutableMapOf()
    protected val avklaringer: MutableSet<Avklaringkode> = mutableSetOf()
    protected val kvoter: MutableList<KvoteDefinisjon> = mutableListOf()
    protected var skalVurderes: (opplysninger: LesbarOpplysninger) -> Boolean = { true }
    protected var skalRevurderes: (opplysninger: LesbarOpplysninger) -> Boolean = { true }
    private val forutsetninger: MutableList<Forutsetning> = mutableListOf()
    protected var aktivOpplysning: Opplysningstype<Boolean>? = null
    protected var relevant: (opplysninger: LesbarOpplysninger) -> Boolean = { true }

    fun avklaring(avklaringkode: Avklaringkode) {
        avklaringer.add(avklaringkode)
    }

    fun kvote(kvote: KvoteDefinisjon) {
        kvoter.add(kvote)
    }

    /**
     * Rå planleggingslambda. Lavnivå; foretrekk [forutsetter] og [aktivNår].
     * Skjuler aldri opplysninger.
     */
    fun skalVurderes(block: (opplysninger: LesbarOpplysninger) -> Boolean) {
        skalVurderes = block
    }

    /** Regelsettet kjører først når alle vilkårene er oppfylt (utfall finnes og er sant). Kun planlegging. */
    fun forutsetter(vararg vilkår: Regelsett) {
        vilkår.forEach { forutsetninger.add(it.oppfylt) }
    }

    /** Regelsettet kjører først når alle forutsetningene er oppfylt. Kun planlegging. */
    fun forutsetter(vararg forutsetning: Forutsetning) {
        forutsetninger.addAll(forutsetning)
    }

    /** Regelsettet kjører først når minst ett av vilkårene er oppfylt. Kun planlegging. */
    fun forutsetterEnAv(vararg vilkår: Regelsett) {
        forutsetninger.add(Forutsetning.enAv(*vilkår.map { it.oppfylt }.toTypedArray()))
    }

    /** Regelsettet kjører først når minst én av forutsetningene er oppfylt. Kun planlegging. */
    fun forutsetterEnAv(vararg forutsetning: Forutsetning) {
        forutsetninger.add(Forutsetning.enAv(*forutsetning))
    }

    /** Regelsettet kjører først når vilkårene er vurdert, uavhengig av om de er oppfylt. Kun planlegging. */
    fun forutsetterVurdert(vararg vilkår: Regelsett) {
        vilkår.forEach { forutsetninger.add(it.vurdert) }
    }

    /**
     * Knytter regelsettet til en periodisert, boolsk opplysning som sier om regelsettet er aktivt.
     * Opplysninger regelsettet produserer skjules i perioder hvor [aktiv] er usann, slik at de ikke
     * overlever (også arvede) når regelsettet skrus av. Skal bare brukes når [aktiv] er et rent
     * scope-flagg, ikke en forutsetning som oppfylles underveis. Regelsettet kjøres heller ikke
     * i perioder hvor [aktiv] ikke er sann.
     */
    fun aktivNår(aktiv: Opplysningstype<Boolean>) {
        aktivOpplysning = aktiv
    }

    fun skalRevurderes(block: (opplysninger: LesbarOpplysninger) -> Boolean) {
        skalRevurderes = block
    }

    fun påvirkerResultat(block: (opplysninger: LesbarOpplysninger) -> Boolean) {
        relevant = block
    }

    fun <T : Any> regel(
        produserer: Opplysningstype<T>,
        gjelderFraOgMed: LocalDate = LocalDate.MIN,
        block: Opplysningstype<T>.() -> Regel<*>,
    ) = leggTil(gjelderFraOgMed, produserer.block())

    private fun leggTil(
        gjelderFra: LocalDate,
        regel: Regel<*>,
    ) = regler.computeIfAbsent(regel.produserer) { TemporalCollection() }.put(gjelderFra, regel)

    // Samlet planleggingsvurdering: forutsetninger, scope og eventuell rå lambda
    protected fun skalKjøres(): (LesbarOpplysninger) -> Boolean {
        val forutsetninger = forutsetninger.toList()
        val aktiv = aktivOpplysning
        val rå = skalVurderes
        return { opplysninger ->
            forutsetninger.all { it.erOppfylt(opplysninger) } &&
                (aktiv == null || opplysninger.erSann(aktiv)) &&
                rå(opplysninger)
        }
    }

    abstract fun build(): Regelsett
}

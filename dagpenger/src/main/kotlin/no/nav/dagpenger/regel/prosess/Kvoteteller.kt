package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forbrukstype.Rettighet
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.sanksjonerSortert
import no.nav.dagpenger.regel.Kvotetelling
import no.nav.dagpenger.regel.Kvotetellingsresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat.Beregningsdag
import java.time.LocalDate
import kotlin.collections.filter

internal fun KvoteDefinisjon.tell(
    opplysninger: LesbarOpplysninger,
    fraOgMed: LocalDate,
    dager: List<LocalDate>,
    beregningsdager: List<Beregningsdag>,
): Kvotetellingsresultat = Kvotetelling.tell(tildeltKapasitet(opplysninger), forrigeForbruk(opplysninger, fraOgMed), dager, beregningsdager)

internal class Kvoteteller private constructor(
    private val kvoter: List<KvoteDefinisjon>,
    private val beregningsdager: List<Beregningsdag>,
    private val rettighetsdager: List<LocalDate>,
    private val sanksjonsdager: List<LocalDate>,
) {
    constructor(kvoter: List<KvoteDefinisjon>, beregningsdager: List<Beregningsdag>) : this(
        kvoter,
        beregningsdager,
        rettighetsdager = beregningsdager.filterIsInstance<Beregningsdag.Forbruksdag>().map { it.dag.dato },
        sanksjonsdager = beregningsdager.filter { it.avviklerSanksjon }.map { it.dag.dato }.sorted(),
    )

    fun beregn(
        opplysninger: LesbarOpplysninger,
        fraOgMed: LocalDate,
    ): Map<KvoteDefinisjon, Kvotetellingsresultat> =
        fordelDagerPåKvoter(opplysninger, fraOgMed).mapValues { (kvote, telledager) ->
            kvote.tell(opplysninger, fraOgMed, telledager, beregningsdager)
        }

    private fun fordelDagerPåKvoter(
        opplysninger: LesbarOpplysninger,
        fraOgMed: LocalDate,
    ): Map<KvoteDefinisjon, List<LocalDate>> {
        // Alle kvoter som teller rettighet får telle alle forbruksdager
        val rettigheter = kvoter.filter { it.teller(Rettighet) }.associateWith { rettighetsdager }

        // Alle kvoter som teller bortfall må telle i rekkefølge
        val sanksjoner = bortfallPerSanksjon(opplysninger, fraOgMed)

        return rettigheter + sanksjoner
    }

    private fun bortfallPerSanksjon(
        opplysninger: LesbarOpplysninger,
        fraOgMed: LocalDate,
    ): Map<KvoteDefinisjon, List<LocalDate>> {
        val kø = ArrayDeque(sanksjonsdager)
        return kvoter
            .sanksjonerSortert(opplysninger)
            .associateWith { kvote ->
                val kapasitet = kvote.gjenståendeVed(opplysninger, fraOgMed)
                kø.trekk(minOf(kapasitet, kø.size))
            }
    }

    private fun <T> ArrayDeque<T>.trekk(antall: Int): List<T> = (1..antall).map { removeFirst() }
}

package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forbrukstype.Rettighet
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.sanksjonerSortert
import no.nav.dagpenger.regel.Kvotetelling
import no.nav.dagpenger.regel.Kvotetellingsresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat.Beregningsdag
import java.time.LocalDate

internal fun KvoteDefinisjon.tell(
    opplysninger: LesbarOpplysninger,
    fraOgMed: LocalDate,
    dager: List<LocalDate>,
    beregningsdager: List<Beregningsdag>,
): Kvotetellingsresultat =
    Kvotetelling.tell(
        tildeltKapasitetOppslag(opplysninger),
        forrigeForbruk(opplysninger, fraOgMed),
        dager,
        beregningsdager,
    )

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
        val rettigheter =
            kvoter
                .filter { it.teller(Rettighet) }
                .filter { it.skalFortsattTelles(opplysninger, fraOgMed) }
                .associateWith { kvotedefinisjon ->
                    rettighetsdager.filter {
                        opplysninger.finnOpplysning(kvotedefinisjon.tellesNår, it).verdi
                    }
                }

        // Alle kvoter som teller bortfall må telle i rekkefølge
        val sanksjoner = bortfallPerSanksjon(opplysninger, fraOgMed)

        return rettigheter + sanksjoner
    }

    /**
     * En rettighetskvote skal fortsatt telles med for denne perioden dersom utløsendeBetingelse enten
     * er oppfylt, eller først ble satt til usann ETTER at perioden startet - altså at kvoten brukes
     * opp midt i DENNE perioden. Den skal telles med helt til overgangen faktisk trer i kraft. Uten
     * dette ville en rekjøring innenfor samme periode ekskludert kvoten fullstendig, og mistet
     * forbruket som allerede var beregnet før overgangen.
     */
    private fun KvoteDefinisjon.skalFortsattTelles(
        opplysninger: LesbarOpplysninger,
        fraOgMed: LocalDate,
    ): Boolean {
        val betingelse = opplysninger.finnNullableOpplysning(utløsendeBetingelse) ?: return false
        return betingelse.verdi || betingelse.gyldighetsperiode.fraOgMed.isAfter(fraOgMed)
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

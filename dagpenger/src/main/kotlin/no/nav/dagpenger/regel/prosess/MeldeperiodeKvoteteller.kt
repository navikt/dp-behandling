package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.allokeringskjede
import no.nav.dagpenger.opplysning.erEksklusivt
import no.nav.dagpenger.opplysning.gjenståendeVed
import no.nav.dagpenger.opplysning.tildeltKapasitet
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.Forbruksdagverdi
import no.nav.dagpenger.regel.Kvotetelling
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat
import java.time.LocalDate

internal class MeldeperiodeKvoteteller(
    private val kvoter: List<KvoteDefinisjon>,
) {
    fun oppgjør(
        forbruksdager: List<Beregningresultat.Forbruksdag>,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
    ): KvoteOppgjør {
        val periodeforbruk = Periodeforbruk.fra(forbruksdager)
        val tildelteSanksjonsdager =
            tildelBortfallsdagerTilSanksjoner(
                bortfallsdager = periodeforbruk.bortfallsdager,
                opplysninger = opplysninger,
                meldeperiode = meldeperiode,
            )
        return KvoteOppgjør(
            kvotetellinger =
                kvoter.map { kvote ->
                    KvoteTelling(
                        kvote = kvote,
                        resultat = tellKvote(kvote, opplysninger, meldeperiode, periodeforbruk, tildelteSanksjonsdager),
                    )
                },
        )
    }

    private fun tellKvote(
        kvote: KvoteDefinisjon,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
        periodeforbruk: Periodeforbruk,
        tildelteSanksjonsdager: Map<KvoteDefinisjon, Set<LocalDate>>,
    ) = Kvotetelling.tell(
        kapasitet = kvote.tildeltKapasitet(opplysninger),
        utgangspunkt = sisteVerdiFørPeriode(kvote, opplysninger, meldeperiode.fraOgMed),
        dager = telledagerForKvote(kvote, meldeperiode, periodeforbruk, tildelteSanksjonsdager),
    )

    private fun telledagerForKvote(
        kvote: KvoteDefinisjon,
        meldeperiode: Periode,
        periodeforbruk: Periodeforbruk,
        tildelteSanksjonsdager: Map<KvoteDefinisjon, Set<LocalDate>>,
    ): List<Forbruksdagverdi> {
        if (!kvote.erEksklusivt()) {
            return meldeperiode.map { dato -> Forbruksdagverdi(dato, dato in periodeforbruk.forbruksdager) }
        }

        val tildelteDager = tildelteSanksjonsdager[kvote] ?: emptySet()
        if (tildelteDager.isEmpty()) return emptyList()

        return meldeperiode.map { dato -> Forbruksdagverdi(dato, dato in tildelteDager) }
    }

    private fun tildelBortfallsdagerTilSanksjoner(
        bortfallsdager: List<LocalDate>,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
    ): Map<KvoteDefinisjon, Set<LocalDate>> {
        val bortfallskø =
            ArrayDeque(
                bortfallsdager.sorted(),
            )

        val tildelteBortfallsdager = mutableMapOf<KvoteDefinisjon, Set<LocalDate>>()
        kvoter.allokeringskjede(opplysninger).forEach { kvote ->
            val kapasitet = kvote.gjenståendeVed(opplysninger, meldeperiode.fraOgMed)
            val tildelt = mutableSetOf<LocalDate>()
            repeat(minOf(kapasitet, bortfallskø.size)) { tildelt.add(bortfallskø.removeFirst()) }
            tildelteBortfallsdager[kvote] = tildelt
        }
        return tildelteBortfallsdager
    }

    private fun sisteVerdiFørPeriode(
        kvote: KvoteDefinisjon,
        opplysninger: Opplysninger,
        fraOgMed: LocalDate,
    ): Int =
        opplysninger
            .finnAlle(kvote.forbruksteller)
            .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(fraOgMed) }
            ?.verdi ?: 0

    private data class Periodeforbruk(
        val forbruksdager: Set<LocalDate>,
        val bortfallsdager: List<LocalDate>,
    ) {
        companion object {
            fun fra(forbruksdager: List<Beregningresultat.Forbruksdag>) =
                Periodeforbruk(
                    forbruksdager = forbruksdager.map { it.dag.dato }.toSet(),
                    bortfallsdager = forbruksdager.filter { it.erBortfall }.map { it.dag.dato },
                )
        }
    }
}

internal data class KvoteOppgjør(
    val kvotetellinger: List<KvoteTelling>,
)

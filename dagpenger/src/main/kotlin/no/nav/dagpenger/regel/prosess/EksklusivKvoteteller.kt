package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forbrukstype
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.allokeringskjede
import no.nav.dagpenger.opplysning.erEksklusivt
import no.nav.dagpenger.opplysning.gjenståendeVed
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.Kvoteteller
import no.nav.dagpenger.regel.Kvotetellingsresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat
import java.time.LocalDate

internal class KvoteOppgjørService(
    private val kvoter: List<KvoteDefinisjon>,
) {
    fun oppgjør(
        forbruksdager: List<Beregningresultat.Forbruksdag>,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
    ): KvoteOppgjør {
        val kjede = KvoteKjede(kvoter.allokeringskjede(opplysninger))
        val allokeringslogg = alloker(forbruksdager, kjede, opplysninger, meldeperiode)
        val opplysningerMedMeldeperiodeData = opplysningerMedMeldeperiodeData(opplysninger, meldeperiode, allokeringslogg)

        return KvoteOppgjør(
            allokeringslogg = allokeringslogg,
            kvotetellinger =
                kvoter.map { kvote ->
                    KvoteTelling(
                        kvote = kvote,
                        resultat = beregn(kvote, opplysningerMedMeldeperiodeData, meldeperiode, allokeringslogg),
                    )
                },
        )
    }

    private fun alloker(
        forbruksdager: List<Beregningresultat.Forbruksdag>,
        kvotekjede: KvoteKjede,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
    ): Allokeringslogg {
        if (kvotekjede.kvoter.isEmpty()) {
            return Allokeringslogg(forbruksdager.sortedBy { it.dag.dato }.map { it.tilAllokertDag() })
        }

        val forbruksdagerPerType =
            forbruksdager
                .sortedBy { it.dag.dato }
                .let { sorterteForbruksdager ->
                    mapOf(
                        Forbrukstype.Rettighet to ArrayDeque(sorterteForbruksdager.filterNot { it.erBortfall }),
                        Forbrukstype.Bortfall to ArrayDeque(sorterteForbruksdager.filter { it.erBortfall }),
                    )
                }

        val kvotePerDato = mutableMapOf<LocalDate, KvoteDefinisjon>()
        kvotekjede.kvoter.forEach { kvote ->
            val kapasitet = kvote.gjenståendeVed(opplysninger, meldeperiode.fraOgMed)
            val allokerbareDager = forbruksdagerPerType[kvote.forbrukstype].orEmptyDeque()
            repeat(minOf(kapasitet, allokerbareDager.size)) {
                kvotePerDato[allokerbareDager.removeFirst().dag.dato] = kvote
            }
        }

        return Allokeringslogg(
            forbruksdager
                .sortedBy { it.dag.dato }
                .map { forbruksdag -> forbruksdag.tilAllokertDag(kvotePerDato[forbruksdag.dag.dato]) },
        )
    }

    private fun opplysningerMedMeldeperiodeData(
        opplysninger: Opplysninger,
        meldeperiode: Periode,
        allokeringslogg: Allokeringslogg,
    ): Opplysninger {
        require(
            allokeringslogg.dager
                .map { it.dato }
                .distinct()
                .size == allokeringslogg.dager.size,
        ) {
            "Allokeringslogg inneholder duplikate datoer"
        }
        val dagPerDato = allokeringslogg.dager.associateBy { it.dato }
        return Opplysninger.basertPå(opplysninger).apply {
            meldeperiode.forEach { dato ->
                val dag = dagPerDato[dato]
                val gyldighetsperiode = Gyldighetsperiode(dato, dato)
                leggTil(Faktum(Beregning.forbruk, dag != null, gyldighetsperiode))
                leggTil(Faktum(Beregning.utbetaling, dag?.tilUtbetaling ?: Beløp(0), gyldighetsperiode))
                leggTil(Faktum(Beregning.erBortfallsdag, dag?.erBortfall ?: false, gyldighetsperiode))
            }
        }
    }

    private fun beregn(
        kvote: KvoteDefinisjon,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
        allokeringslogg: Allokeringslogg,
    ): Kvotetellingsresultat =
        if (kvote.erEksklusivt()) {
            beregnMedTildelteDager(kvote, opplysninger, meldeperiode, allokeringslogg)
        } else {
            Kvoteteller(kvote).beregn(opplysninger)
        }

    private fun beregnMedTildelteDager(
        kvote: KvoteDefinisjon,
        opplysninger: Opplysninger,
        meldeperiode: Periode,
        allokeringslogg: Allokeringslogg,
    ): Kvotetellingsresultat {
        val kvotedager =
            allokeringslogg.dager
                .filter { it.kvote == kvote }
                .map { it.dato }
                .toSet()
        if (kvotedager.isEmpty()) return Kvotetellingsresultat()

        val opplysningerForKvote =
            Opplysninger.basertPå(opplysninger).apply {
                meldeperiode.forEach { dato ->
                    leggTil(
                        Faktum(
                            Beregning.erBortfallsdag,
                            dato in kvotedager,
                            Gyldighetsperiode(dato, dato),
                        ),
                    )
                }
            }

        return Kvoteteller(kvote).beregn(opplysningerForKvote)
    }
}

internal data class KvoteOppgjør(
    val allokeringslogg: Allokeringslogg,
    val kvotetellinger: List<KvoteTelling>,
)

internal data class KvoteKjede(
    val kvoter: List<KvoteDefinisjon>,
)

internal data class Allokeringslogg(
    val dager: List<AllokertDag>,
)

internal data class AllokertDag(
    val dato: LocalDate,
    val erBortfall: Boolean,
    val tilUtbetaling: Beløp,
    val kvote: KvoteDefinisjon? = null,
    val hjemmel: Hjemmel? = kvote?.hjemmel,
)

private fun Beregningresultat.Forbruksdag.tilAllokertDag(kvote: KvoteDefinisjon? = null) =
    AllokertDag(
        dato = dag.dato,
        erBortfall = erBortfall,
        tilUtbetaling = tilUtbetaling,
        kvote = kvote,
    )

private fun <T> ArrayDeque<T>?.orEmptyDeque(): ArrayDeque<T> = this ?: ArrayDeque()

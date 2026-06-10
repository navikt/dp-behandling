package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forbrukstype
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.allokeringskjede
import no.nav.dagpenger.opplysning.gjenståendeVed
import no.nav.dagpenger.opplysning.tildeltKapasitet
import no.nav.dagpenger.regel.Kvotetelling
import no.nav.dagpenger.regel.Kvotetellingsresultat
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat
import java.time.LocalDate

data class KategoriserteDager(
    val rettighetsdager: List<LocalDate>,
    val bortfallsdager: List<LocalDate>,
)

fun List<Beregningresultat.Forbruksdag>.kategoriser(): KategoriserteDager =
    KategoriserteDager(
        rettighetsdager = filter { !it.erBortfall }.map { it.dag.dato },
        bortfallsdager = filter { it.erBortfall }.map { it.dag.dato }.sorted(),
    )

internal class KvotetellerOrkestrering(
    private val kvoter: List<KvoteDefinisjon>,
) {
    fun beregn(
        kategoriserteDager: KategoriserteDager,
        opplysninger: LesbarOpplysninger,
        fraOgMed: LocalDate,
    ): Map<KvoteDefinisjon, Kvotetellingsresultat> {
        val rettighetskvoter = kvoter.filter { it.forbrukstype == Forbrukstype.Rettighet }
        val sanksjonskvoteresultater =
            FifoSanksjonsAllokatør
                .alloker(kvoter.allokeringskjede(opplysninger), kategoriserteDager.bortfallsdager, opplysninger, fraOgMed)

        val rettighetsresultater =
            rettighetskvoter.associate { kvote ->
                kvote to
                    Kvotetelling.tell(
                        kapasitet = kvote.tildeltKapasitet(opplysninger),
                        utgangspunkt = sisteVerdiFørPeriode(kvote, opplysninger, fraOgMed),
                        dager = kategoriserteDager.rettighetsdager,
                    )
            }

        val sanksjonsresultater =
            sanksjonskvoteresultater.entries.associate { (kvote, tildelteDager) ->
                kvote to
                    Kvotetelling.tell(
                        kapasitet = kvote.tildeltKapasitet(opplysninger),
                        utgangspunkt = sisteVerdiFørPeriode(kvote, opplysninger, fraOgMed),
                        dager = tildelteDager,
                    )
            }

        return rettighetsresultater + sanksjonsresultater
    }

    private fun sisteVerdiFørPeriode(
        kvote: KvoteDefinisjon,
        opplysninger: LesbarOpplysninger,
        fraOgMed: LocalDate,
    ): Int =
        opplysninger
            .finnAlle(kvote.forbruksteller)
            .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(fraOgMed) }
            ?.verdi ?: 0
}

internal object FifoSanksjonsAllokatør {
    /**
     * Fordeler [bortfallsdager] til sanksjons-kvoter i FIFO-rekkefølge.
     * Kvoter sortert etter ilagtDato — tidligst ilagt sanksjon forbrukes først.
     * Restkapasitet hentes fra persistert gjenstående-verdi fra forrige periode.
     */
    fun alloker(
        kvoter: List<KvoteDefinisjon>,
        bortfallsdager: List<LocalDate>,
        opplysninger: LesbarOpplysninger,
        fraOgMed: LocalDate,
    ): Map<KvoteDefinisjon, List<LocalDate>> {
        val dagkø = ArrayDeque(bortfallsdager.sorted())
        return kvoter.associateWith { kvote ->
            val gjenstående = kvote.gjenståendeVed(opplysninger, fraOgMed)
            (1..minOf(gjenstående, dagkø.size)).map { dagkø.removeFirst() }
        }
    }
}

package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat
import java.time.LocalDate

object Kvotetelling {
    /** Teller forbruk. Alle datoer i [dager] teller som +1. */
    fun tell(
        kapasitet: Int,
        utgangspunkt: Int,
        dager: List<LocalDate>,
        beregningsdager: List<Beregningresultat.Beregningsdag>,
    ): Kvotetellingsresultat {
        val sortert = beregningsdager.sortedBy { it.dag.dato }
        var teller = utgangspunkt
        val forbruktTeller =
            sortert.map { beregningsdag ->
                val dato = beregningsdag.dag.dato
                if (dato in dager) teller++
                KvotetellingsVerdi(minOf(teller, kapasitet), Gyldighetsperiode(dato, dato))
            }
        val gjenstående =
            forbruktTeller.map {
                val g = maxOf(kapasitet - it.verdi, 0)
                require(g >= 0) {
                    "Gjenstående kan ikke være negativt. Har $g igjen"
                }
                KvotetellingsVerdi(g, it.gyldighetsperiode)
            }

        val sisteForbruksdato = dager.lastOrNull()

        return Kvotetellingsresultat(
            forbruktTeller = forbruktTeller,
            gjenstående = gjenstående,
            sisteDagMedForbruk =
                KvotetellingsVerdi(
                    sisteForbruksdato ?: beregningsdager.last().dag.dato,
                    Gyldighetsperiode(beregningsdager.last().dag.dato),
                ),
            sisteGjenstående =
                KvotetellingsVerdi(
                    gjenstående.lastOrNull()?.verdi ?: kapasitet,
                    Gyldighetsperiode(beregningsdager.last().dag.dato),
                ),
        )
    }
}

class KvotetellingsSkriver(
    private val definisjon: KvoteDefinisjon,
) {
    fun skriv(
        opplysninger: Opplysninger,
        resultat: Kvotetellingsresultat,
    ) {
        val batch =
            buildList {
                resultat.forbruktTeller.forEach { add(Faktum(definisjon.forbruksteller, it.verdi, it.gyldighetsperiode)) }
                resultat.gjenstående.forEach { add(Faktum(definisjon.gjenstående, it.verdi, it.gyldighetsperiode)) }
                resultat.sisteDagMedForbruk?.let { add(Faktum(definisjon.sisteForbruk, it.verdi, it.gyldighetsperiode)) }
                resultat.sisteGjenstående?.let { add(Faktum(definisjon.sisteGjenstående, it.verdi, it.gyldighetsperiode)) }
            }
        opplysninger.leggTilAlle(batch)

        if (resultat.sisteGjenstående?.verdi == 0 && resultat.gjenstående.any { it.verdi != 0 }) {
            val sisteDag = resultat.sisteDagMedForbruk!!.verdi
            opplysninger.leggTil(Faktum(definisjon.utløsendeBetingelse, false, Gyldighetsperiode(sisteDag.plusDays(1))))
        }
    }
}

data class Kvotetellingsresultat(
    val forbruktTeller: List<KvotetellingsVerdi<Int>> = emptyList(),
    val gjenstående: List<KvotetellingsVerdi<Int>> = emptyList(),
    val sisteDagMedForbruk: KvotetellingsVerdi<LocalDate>? = null,
    val sisteGjenstående: KvotetellingsVerdi<Int>? = null,
)

data class KvotetellingsVerdi<T : Any>(
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode,
)

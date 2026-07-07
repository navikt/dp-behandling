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

        return Kvotetellingsresultat(
            forbruktTeller = forbruktTeller,
            gjenstående = gjenstående,
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
        resultat.forbruktTeller.forEach { opplysninger.leggTil(Faktum(definisjon.forbruksteller, it.verdi, it.gyldighetsperiode)) }
        resultat.gjenstående.forEach { opplysninger.leggTil(Faktum(definisjon.gjenstående, it.verdi, it.gyldighetsperiode)) }
        val gjenstående = resultat.gjenstående.firstOrNull { it.verdi == 0 }
        gjenstående?.let {
            opplysninger.leggTil(
                Faktum(
                    definisjon.utløsendeBetingelse,
                    false,
                    Gyldighetsperiode(it.gyldighetsperiode.fraOgMed.plusDays(1)),
                ),
            )
        }
    }
}

data class Kvotetellingsresultat(
    val forbruktTeller: List<KvotetellingsVerdi<Int>> = emptyList(),
    val gjenstående: List<KvotetellingsVerdi<Int>> = emptyList(),
)

data class KvotetellingsVerdi<T : Any>(
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode,
)

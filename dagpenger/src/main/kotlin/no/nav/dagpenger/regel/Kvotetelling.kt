package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
import java.time.LocalDate

object Kvotetelling {
    /** Teller forbruk. Alle datoer i [dager] teller som +1. */
    fun tell(
        kapasitet: Int,
        utgangspunkt: Int,
        dager: List<LocalDate>,
        fraOgMed: LocalDate,
    ): Kvotetellingsresultat {
        if (kapasitet <= 0) {
            return Kvotetellingsresultat(
                forbruktTeller = listOf(KvotetellingsVerdi(0, Gyldighetsperiode(fraOgMed))),
                gjenstående = listOf(KvotetellingsVerdi(0, Gyldighetsperiode(fraOgMed))),
                sisteDagMedForbruk = KvotetellingsVerdi(fraOgMed, Gyldighetsperiode(fraOgMed)),
                sisteGjenstående = KvotetellingsVerdi(0, Gyldighetsperiode(fraOgMed)),
            )
        }
        if (dager.isEmpty()) return Kvotetellingsresultat()
        val sortert = dager.sorted()
        if (sortert.isEmpty()) return Kvotetellingsresultat()
        var teller = utgangspunkt
        val forbruktTeller =
            sortert.map { dato ->
                teller++
                KvotetellingsVerdi(teller, Gyldighetsperiode(dato, dato))
            }
        val gjenstående =
            forbruktTeller.map {
                val g = kapasitet - it.verdi
                require(g >= 0) {
                    "Gjenstående kan ikke være negativt. Har $g igjen"
                }
                KvotetellingsVerdi(g, it.gyldighetsperiode)
            }

        val sisteForbruksdato = sortert.last()

        return Kvotetellingsresultat(
            forbruktTeller = forbruktTeller,
            gjenstående = gjenstående,
            sisteDagMedForbruk = KvotetellingsVerdi(sisteForbruksdato, Gyldighetsperiode(sisteForbruksdato)),
            sisteGjenstående = KvotetellingsVerdi(gjenstående.last().verdi, Gyldighetsperiode(sisteForbruksdato)),
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
        resultat.sisteDagMedForbruk?.let { opplysninger.leggTil(Faktum(definisjon.sisteForbruk, it.verdi, it.gyldighetsperiode)) }
        resultat.sisteGjenstående?.let { opplysninger.leggTil(Faktum(definisjon.sisteGjenstående, it.verdi, it.gyldighetsperiode)) }

        if (resultat.sisteGjenstående?.verdi == 0) {
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

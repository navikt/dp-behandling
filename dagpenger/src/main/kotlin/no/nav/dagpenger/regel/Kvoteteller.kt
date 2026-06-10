package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.tildeltKapasitet
import java.time.LocalDate

class Kvoteteller(
    val definisjon: KvoteDefinisjon,
) {
    fun beregn(opplysninger: LesbarOpplysninger): Kvotetellingsresultat {
        val kapasitet = definisjon.tildeltKapasitet(opplysninger)
        val telledager = lesTelledager(opplysninger)
        val utgangspunkt = lesUtgangspunkt(opplysninger, telledager)
        return Kvotetelling.tell(kapasitet, utgangspunkt, telledager)
    }

    private fun lesTelledager(opplysninger: LesbarOpplysninger): List<LocalDate> =
        opplysninger.kunEgne
            .finnAlle(definisjon.tellesNår)
            .filter { it.verdi }
            .sortedBy { it.gyldighetsperiode.fraOgMed }
            .map { it.gyldighetsperiode.fraOgMed }

    private fun lesUtgangspunkt(
        opplysninger: LesbarOpplysninger,
        telledager: List<LocalDate>,
    ): Int {
        val førsteDag = telledager.firstOrNull() ?: return 0
        return opplysninger
            .finnAlle(definisjon.forbruksteller)
            .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(førsteDag) }
            ?.verdi ?: 0
    }
}

object Kvotetelling {
    /** Teller forbruk. Alle datoer i [dager] teller som +1. */
    fun tell(
        kapasitet: Int,
        utgangspunkt: Int,
        dager: List<LocalDate>,
    ): Kvotetellingsresultat {
        if (kapasitet <= 0) return Kvotetellingsresultat()
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

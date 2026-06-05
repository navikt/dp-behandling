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

    private fun lesTelledager(opplysninger: LesbarOpplysninger): List<Forbruksdagverdi> =
        opplysninger.kunEgne
            .finnAlle(definisjon.tellesNår)
            .sortedBy { it.gyldighetsperiode.fraOgMed }
            .map { Forbruksdagverdi(it.gyldighetsperiode.fraOgMed, it.verdi) }

    private fun lesUtgangspunkt(
        opplysninger: LesbarOpplysninger,
        telledager: List<Forbruksdagverdi>,
    ): Int {
        val førsteDag = telledager.firstOrNull()?.dato ?: return 0
        return opplysninger
            .finnAlle(definisjon.forbruksteller)
            .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(førsteDag) }
            ?.verdi ?: 0
    }
}

data class Forbruksdagverdi(
    val dato: LocalDate,
    val forbruk: Boolean,
)

object Kvotetelling {
    fun tell(
        kapasitet: Int,
        utgangspunkt: Int,
        dager: List<Forbruksdagverdi>,
    ): Kvotetellingsresultat {
        if (kapasitet <= 0) return Kvotetellingsresultat()
        val sortert = dager.sortedBy { it.dato }
        if (sortert.isEmpty()) return Kvotetellingsresultat()

        var teller = utgangspunkt
        val forbruktTeller =
            sortert.map { dag ->
                if (dag.forbruk) teller++
                KvotetellingsVerdi(teller, Gyldighetsperiode(dag.dato, dag.dato))
            }
        val gjenstående =
            forbruktTeller.map {
                val g = kapasitet - it.verdi
                require(g >= 0) {
                    "Gjenstående kan ikke være negativt. Har $g igjen"
                }
                KvotetellingsVerdi(g, it.gyldighetsperiode)
            }
        val sisteForbruksdato = sortert.lastOrNull { it.forbruk }?.dato
        return Kvotetellingsresultat(
            forbruktTeller = forbruktTeller,
            gjenstående = gjenstående,
            sisteDagMedForbruk = sisteForbruksdato?.let { KvotetellingsVerdi(it, Gyldighetsperiode(it)) },
            sisteGjenstående =
                sisteForbruksdato?.let {
                    KvotetellingsVerdi(gjenstående.last().verdi, Gyldighetsperiode(it))
                },
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

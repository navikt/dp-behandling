package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.totalKapasitet
import java.time.LocalDate

/**
 * Generell kvoteteller som teller forbruk av en kvote basert på en boolsk opplysning per dag.
 *
 * Brukes for:
 * - Stønadsdager: teller ned gjenstående dagpengedager
 * - Bortfallsdager: teller ned gjenstående bortfallsdager
 */
class Kvoteteller(
    val definisjon: KvoteDefinisjon,
) {
    fun beregn(opplysninger: LesbarOpplysninger): Kvotetellingsresultat {
        val totalKapasitet = definisjon.totalKapasitet(opplysninger)
        if (totalKapasitet <= 0) return Kvotetellingsresultat()

        val dager = hentDagerMedForbruk(opplysninger)
        if (dager.isEmpty()) return Kvotetellingsresultat()

        val forbruktTeller = beregnForbruktTeller(opplysninger, dager)
        val gjenstående = beregnGjenstående(dager, totalKapasitet, forbruktTeller)
        return Kvotetellingsresultat(
            forbruktTeller = forbruktTeller,
            gjenstående = gjenstående,
            sisteDagMedForbruk = hentSisteDagMedForbruk(dager),
            sisteGjenstående = hentSisteGjenstående(dager, gjenstående),
        )
    }

    private fun hentDagerMedForbruk(opplysninger: LesbarOpplysninger): List<Opplysning<Boolean>> =
        opplysninger.kunEgne.finnAlle(definisjon.tellesNår).sortedBy { it.gyldighetsperiode.fraOgMed }

    private fun beregnForbruktTeller(
        opplysninger: LesbarOpplysninger,
        dager: List<Opplysning<Boolean>>,
    ): List<KvotetellingsVerdi<Int>> {
        var utgangspunkt = hentUtgangspunkt(opplysninger, dager)
        return dager.map {
            if (it.verdi) utgangspunkt++
            KvotetellingsVerdi(utgangspunkt, it.gyldighetsperiode)
        }
    }

    private fun beregnGjenstående(
        dager: List<Opplysning<Boolean>>,
        totalKapasitet: Int,
        forbruktTeller: List<KvotetellingsVerdi<Int>>,
    ): List<KvotetellingsVerdi<Int>> =
        forbruktTeller.mapIndexed { indeks, verdi ->
            val gjenståendeVerdi = totalKapasitet - verdi.verdi
            require(gjenståendeVerdi >= 0) {
                "Gjenstående kan ikke være negativt. Har $gjenståendeVerdi igjen for ${
                    definisjon.tildelingsgrunnlag.kapasitet.navn
                }"
            }

            KvotetellingsVerdi(gjenståendeVerdi, dager[indeks].gyldighetsperiode)
        }

    private fun hentUtgangspunkt(
        opplysninger: LesbarOpplysninger,
        dager: List<Opplysning<Boolean>>,
    ): Int =
        opplysninger
            .finnAlle(definisjon.forbruksteller)
            .lastOrNull {
                it.gyldighetsperiode.fraOgMed.isBefore(dager.first().gyldighetsperiode.fraOgMed)
            }?.verdi ?: 0

    private fun hentSisteDagMedForbruk(dager: List<Opplysning<Boolean>>): KvotetellingsVerdi<LocalDate>? {
        val sisteForbruksdag = dager.lastOrNull { it.verdi }?.gyldighetsperiode?.fraOgMed ?: return null
        return KvotetellingsVerdi(sisteForbruksdag, Gyldighetsperiode(sisteForbruksdag))
    }

    private fun hentSisteGjenstående(
        dager: List<Opplysning<Boolean>>,
        gjenstående: List<KvotetellingsVerdi<Int>>,
    ): KvotetellingsVerdi<Int>? {
        val sisteForbruksdag = dager.lastOrNull { it.verdi }?.gyldighetsperiode?.fraOgMed ?: return null
        val sisteGjenståendeVerdi = gjenstående.lastOrNull()?.verdi ?: return null
        return KvotetellingsVerdi(sisteGjenståendeVerdi, Gyldighetsperiode(sisteForbruksdag))
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

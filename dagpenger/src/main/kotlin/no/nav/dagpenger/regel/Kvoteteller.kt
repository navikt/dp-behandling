package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import java.time.LocalDate

/**
 * Generell kvoteteller som teller forbruk av en kvote basert på en boolsk opplysning per dag.
 *
 * Brukes for:
 * - Stønadsdager: teller ned gjenstående dagpengedager
 * - Bortfallsdager: teller ned gjenstående bortfallsdager
 */
class Kvoteteller(
    private val kapasitet: Opplysningstype<Int>,
    private val forbrukKriterium: Opplysningstype<Boolean>,
    private val forbruktTeller: Opplysningstype<Int>,
    private val gjenstående: Opplysningstype<Int>,
    private val sisteDagMedForbruk: Opplysningstype<LocalDate>? = null,
    private val sisteGjenstående: Opplysningstype<Int>? = null,
) : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        val opplysninger = kontekst.opplysninger

        if (!opplysninger.har(kapasitet)) return
        val totalKapasitet = opplysninger.finnOpplysning(kapasitet).verdi
        if (totalKapasitet <= 0) return

        val dager = opplysninger.kunEgne.finnAlle(forbrukKriterium)

        if (dager.isEmpty()) return

        var utgangspunkt =
            opplysninger
                .finnAlle(forbruktTeller)
                .lastOrNull {
                    it.gyldighetsperiode.fraOgMed.isBefore(dager.first().gyldighetsperiode.fraOgMed)
                }?.verdi ?: 0

        dager.forEach {
            if (it.verdi) utgangspunkt++
            opplysninger.leggTil(Faktum(forbruktTeller, utgangspunkt, it.gyldighetsperiode))

            val gjenståendeVerdi = totalKapasitet - utgangspunkt
            require(gjenståendeVerdi >= 0) { "Gjenstående kan ikke være negativt. Har $gjenståendeVerdi igjen for ${kapasitet.navn}" }

            opplysninger.leggTil(Faktum(gjenstående, gjenståendeVerdi, it.gyldighetsperiode))
        }

        if (sisteDagMedForbruk != null && sisteGjenstående != null) {
            dager.lastOrNull { it.verdi }?.let {
                val sisteForbruksdato = it.gyldighetsperiode.fraOgMed
                opplysninger.leggTil(
                    Faktum(
                        sisteDagMedForbruk,
                        sisteForbruksdato,
                        Gyldighetsperiode(sisteForbruksdato),
                    ),
                )

                val sisteForbruk = opplysninger.finnAlle(gjenstående).last().verdi
                opplysninger.leggTil(
                    Faktum(
                        sisteGjenstående,
                        sisteForbruk,
                        Gyldighetsperiode(sisteForbruksdato),
                    ),
                )
            }
        }
    }
}

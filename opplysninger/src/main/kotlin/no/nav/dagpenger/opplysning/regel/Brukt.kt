package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate

class Brukt<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<String>,
    private val a: Opplysningstype<T>,
) : Regel<String>(produserer, listOf(a)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): String {
        val produsertOpplysning = opplysninger.finnOpplysning(a)
        val muligeKilder = produsertOpplysning.utledetAv?.opplysninger?.filter { it.verdi == produsertOpplysning.verdi }

        return muligeKilder
            ?.first()
            ?.opplysningstype
            ?.navn!!
    }

    override fun toString() = "Hvilken regel har utledet $a"
}

@JvmName("bruktBeløp")
fun Opplysningstype<String>.brukt(a: Opplysningstype<Beløp>) = Brukt(this, a)

@JvmName("bruktTall")
fun Opplysningstype<String>.brukt(a: Opplysningstype<Int>) = Brukt(this, a)

package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate

class StørreEnn<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val a: Opplysningstype<T>,
    private val b: Opplysningstype<T>,
) : Regel<Boolean>(produserer, listOf(a, b)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Boolean {
        val a = opplysninger.finnOpplysning(a).verdi
        val b = opplysninger.finnOpplysning(b).verdi
        return a > b
    }

    override fun toString() = "Sjekker om $a er større enn $b"
}

@JvmName("størreEnnDouble")
fun Opplysningstype<Boolean>.størreEnn(
    er: Opplysningstype<Double>,
    størreEnn: Opplysningstype<Double>,
) = StørreEnn(this, er, størreEnn)

@JvmName("størreEnnBeløp")
fun Opplysningstype<Boolean>.størreEnn(
    er: Opplysningstype<Beløp>,
    størreEnn: Opplysningstype<Beløp>,
) = StørreEnn(this, er, størreEnn)

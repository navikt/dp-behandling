package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate

@Suppress("UNCHECKED_CAST")
class Substraksjon<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<T>,
    private vararg val opplysningstyper: Opplysningstype<T>,
    private val operasjon: (List<T>) -> T,
) : Regel<T>(produserer, opplysningstyper.toList()) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): T {
        val verdier = opplysninger.finnAlle(opplysningstyper.toList()).map { it.verdi }
        return operasjon(verdier)
    }

    override fun toString() = "Substraksjon av ${opplysningstyper.joinToString(", ")}"
}

@JvmName("substraksjonDouble")
fun Opplysningstype<Double>.substraksjon(vararg opplysningstype: Opplysningstype<Double>) =
    Substraksjon(this, *opplysningstype) {
        it.reduce { acc, t -> acc - t }
    }

@JvmName("substraksjonOperatorDouble")
operator fun Opplysningstype<Double>.minus(opplysningstype: Opplysningstype<Double>) = substraksjon(opplysningstype)

@JvmName("substraksjonBeløp")
fun Opplysningstype<Beløp>.substraksjon(vararg opplysningstype: Opplysningstype<Beløp>) =
    Substraksjon(this, *opplysningstype) {
        it.reduce { acc, t -> acc - t }
    }

@JvmName("substraksjonDoubleTilNull")
fun Opplysningstype<Double>.substraksjonTilNull(vararg opplysningstype: Opplysningstype<Double>) =
    Substraksjon(this, *opplysningstype) {
        it.reduce { acc, t -> maxOf(0.0, acc - t) }
    }

@JvmName("substraksjonBeløpTilNull")
fun Opplysningstype<Beløp>.substraksjonTilNull(vararg opplysningstype: Opplysningstype<Beløp>) =
    Substraksjon(this, *opplysningstype) {
        it.reduce { acc, t -> maxOf(Beløp(0.0), acc - t) }
    }

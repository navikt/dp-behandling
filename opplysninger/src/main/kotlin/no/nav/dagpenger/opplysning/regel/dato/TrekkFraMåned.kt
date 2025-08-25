package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class TrekkFraMåned internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
    private val antallMnd: Opplysningstype<Int>,
    private val førsteDagIMåned: Boolean,
) : Regel<LocalDate>(produserer, listOf(dato, antallMnd)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): LocalDate {
        val utgangspunkt = opplysninger.finnOpplysning(dato).verdi
        val minusMnd = opplysninger.finnOpplysning(antallMnd).verdi.toLong()
        val minus = utgangspunkt.minusMonths(minusMnd - 1)

        return when {
            førsteDagIMåned -> minus.withDayOfMonth(1)
            else -> minus
        }
    }

    override fun toString() = "Trekker fra $antallMnd fra $dato" + if (førsteDagIMåned) " til første dag i måneden" else ""
}

fun Opplysningstype<LocalDate>.trekkFraMåned(
    dato: Opplysningstype<LocalDate>,
    antallMnd: Opplysningstype<Int>,
) = TrekkFraMåned(this, dato, antallMnd, false)

fun Opplysningstype<LocalDate>.trekkFraMånedTilFørste(
    dato: Opplysningstype<LocalDate>,
    antallMnd: Opplysningstype<Int>,
) = TrekkFraMåned(this, dato, antallMnd, true)

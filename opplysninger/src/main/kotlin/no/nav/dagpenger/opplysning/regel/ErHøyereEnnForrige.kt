package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate

/**
 * Sjekker om [ny] er høyere enn verdien [forrige] hadde dagen før prøvingsdato.
 * Er det ingen verdi dagen før, regnes ikke [ny] som høyere.
 */
class ErHøyereEnnForrige internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val ny: Opplysningstype<Beløp>,
    private val forrige: Opplysningstype<Beløp>,
) : Regel<Boolean>(produserer, listOf(ny, forrige)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Boolean {
        val dagenFør = prøvingsdato.minusDays(1)
        if (!opplysninger.har(forrige, dagenFør)) return false

        return opplysninger.finnOpplysning(ny).verdi > opplysninger.finnOpplysning(forrige, dagenFør).verdi
    }

    override fun toString() = "Sjekker om $ny er høyere enn $forrige dagen før prøvingsdato"
}

fun Opplysningstype<Boolean>.erHøyereEnnForrige(
    ny: Opplysningstype<Beløp>,
    forrige: Opplysningstype<Beløp>,
) = ErHøyereEnnForrige(this, ny, forrige)

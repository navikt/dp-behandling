package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp

class StørreEnnEllerLik<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val a: Opplysningstype<T>,
    private val b: Opplysningstype<T>,
) : Regel<Boolean>(produserer, listOf(a, b)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean {
        val a = opplysninger.finnOpplysning(a).verdi
        val b = opplysninger.finnOpplysning(b).verdi
        return a >= b
    }

    override fun toString() = "Sjekker om $a er større enn eller lik $b"
}

@JvmName("størreEnnEllerLikBeløp")
fun Opplysningstype<Boolean>.størreEnnEllerLik(
    er: Opplysningstype<Beløp>,
    størreEnn: Opplysningstype<Beløp>,
) = StørreEnnEllerLik(this, er, størreEnn)

@JvmName("størreEnnEllerLikDouble")
fun Opplysningstype<Boolean>.størreEnnEllerLik(
    er: Opplysningstype<Double>,
    størreEnn: Opplysningstype<Double>,
) = StørreEnnEllerLik(this, er, størreEnn)

@JvmName("størreEnnheltall")
fun Opplysningstype<Boolean>.størreEnnEllerLik(
    er: Opplysningstype<Int>,
    størreEnn: Opplysningstype<Int>,
) = StørreEnnEllerLik(this, er, størreEnn)

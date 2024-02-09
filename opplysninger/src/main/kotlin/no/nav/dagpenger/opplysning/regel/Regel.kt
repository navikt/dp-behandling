package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Utledning

abstract class Regel<T : Comparable<T>>(
    internal val produserer: Opplysningstype<T>,
    val avhengerAv: List<Opplysningstype<*>> = emptyList(),
) {
    fun kanKjøre(opplysninger: LesbarOpplysninger): Boolean = opplysninger.finnAlle(avhengerAv).size == avhengerAv.size

    protected abstract fun kjør(opplysninger: LesbarOpplysninger): T

    fun produserer(opplysningstype: Opplysningstype<*>) = produserer.er(opplysningstype)

    fun lagProdukt(opplysninger: LesbarOpplysninger): Opplysning<T> {
        val basertPå = opplysninger.finnAlle(avhengerAv)
        val erAlleFaktum = basertPå.all { it is Faktum<*> }
        val utledetAv = Utledning(this, basertPå)
        val gyldig =
            no.nav.dagpenger.opplysning.Gyldighetsperiode(
                fom = basertPå.maxOf { it.gyldighetsperiode.fom },
                tom = basertPå.minOf { it.gyldighetsperiode.tom },
            )
        return when (erAlleFaktum) {
            true -> Faktum(produserer, kjør(opplysninger), utledetAv = utledetAv, gyldighetsperiode = gyldig)
            false -> Hypotese(produserer, kjør(opplysninger), utledetAv = utledetAv, gyldighetsperiode = gyldig)
        }
    }
}
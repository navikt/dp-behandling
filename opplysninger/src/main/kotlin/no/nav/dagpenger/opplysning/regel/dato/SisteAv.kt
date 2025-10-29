package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class SisteAv internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private vararg val datoer: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, datoer.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): LocalDate {
        val dager = opplysninger.finnAlle(datoer.toList()).map { it.verdi }
        return dager.maxOrNull() ?: throw IllegalStateException("Ingen datoer funnet")
    }

    override fun toString() = "Fastsetter $produserer til siste dato av ${datoer.joinToString()}"
}

class SisteAvGyldighetsperiode internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private vararg val opplysningerTyper: Opplysningstype<*>,
) : Regel<LocalDate>(produserer, opplysningerTyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): LocalDate {
        val dager = opplysninger.finnFlere(opplysningerTyper.toList()).map { it.gyldighetsperiode.tilOgMed }
        return dager.maxOrNull() ?: throw IllegalStateException("Ingen datoer funnet")
    }

    override fun toString() = "Fastsetter $produserer til siste dato av ${opplysningerTyper.joinToString()}"
}

class SisteHeltallVerdi internal constructor(
    produserer: Opplysningstype<Int>,
    private vararg val opplysningerTyper: Opplysningstype<Int>,
) : Regel<Int>(produserer, opplysningerTyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): Int {
        val dager = opplysninger.finnAlle(opplysningerTyper.toList())
        return dager.maxByOrNull { it.gyldighetsperiode.tilOgMed }?.verdi ?: 0
    }

    override fun toString() = "Fastsetter $produserer som siste verdi etter gyldighetsperiode fra ${opplysningerTyper.joinToString()}"
}

fun Opplysningstype<LocalDate>.sisteAv(vararg liste: Opplysningstype<LocalDate>) = SisteAv(this, *liste)

fun Opplysningstype<LocalDate>.sisteAv(vararg liste: Opplysningstype<*>) = SisteAvGyldighetsperiode(this, *liste)

fun Opplysningstype<Int>.sisteAv(vararg liste: Opplysningstype<Int>) = SisteHeltallVerdi(this, *liste)

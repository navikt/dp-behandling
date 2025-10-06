package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import java.time.LocalDate

class AntallBarn(
    produserer: Opplysningstype<Int>,
    private val opplysningstype: Opplysningstype<BarnListe>,
    private val prøvingsdato: Opplysningstype<LocalDate>,
    private val aldersgrense: Opplysningstype<Int>,
    private val filter: Barn.(LocalDate, Int) -> Boolean,
) : Regel<Int>(produserer, listOf(opplysningstype, prøvingsdato, aldersgrense)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Int {
        val dato = opplysninger.finnOpplysning(prøvingsdato).verdi
        val aldersgrense = opplysninger.finnOpplysning(aldersgrense).verdi
        val liste = opplysninger.finnOpplysning(opplysningstype).verdi
        return liste.filter { filter(it, dato, aldersgrense) }.size
    }

    override fun toString() = "Produserer $produserer ved å telle antall barn som oppfyller filteret."
}

fun Opplysningstype<Int>.antallBarn(
    opplysningstype: Opplysningstype<BarnListe>,
    prøvingsdato: Opplysningstype<LocalDate>,
    aldersgrense: Opplysningstype<Int>,
    filter: Barn.(LocalDate, Int) -> Boolean,
) = AntallBarn(this, opplysningstype, prøvingsdato, aldersgrense, filter)

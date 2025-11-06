package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe

class AntallAv<T : Comparable<T>>(
    produserer: Opplysningstype<Int>,
    val opplysningstype: Opplysningstype<BarnListe>,
    val filter: Barn.() -> Boolean,
) : Regel<Int>(produserer, listOf(opplysningstype)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Int {
        val liste = opplysninger.finnOpplysning(opplysningstype).verdi.barn
        return liste.filter { filter(it) }.size
    }

    override fun toString() = "Produserer $produserer ved å telle antall instanser av $opplysningstype som oppfyller filteret."
}

@Suppress("UNCHECKED_CAST")
@JvmName("antallAvBarn")
fun Opplysningstype<Int>.antallAv(
    opplysningstype: Opplysningstype<BarnListe>,
    filter: Barn.() -> Boolean,
) = AntallAv<BarnListe>(this, opplysningstype, filter)

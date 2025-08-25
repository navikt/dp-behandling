package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.ComparableListe
import java.time.LocalDate

class AntallAv<T : Comparable<T>>(
    produserer: Opplysningstype<Int>,
    val opplysningstype: Opplysningstype<ComparableListe<T>>,
    val filter: T.() -> Boolean,
) : Regel<Int>(produserer, listOf(opplysningstype)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Int {
        val liste = opplysninger.finnOpplysning(opplysningstype).verdi
        return liste.filter { filter(it) }.size
    }

    override fun toString() = "Produserer $produserer ved å telle antall instanser av $opplysningstype som oppfyller filteret."
}

fun <T : Comparable<T>> Opplysningstype<Int>.antallAv(
    opplysningstype: Opplysningstype<ComparableListe<T>>,
    filter: T.() -> Boolean,
) = AntallAv(this, opplysningstype, filter)

@Suppress("UNCHECKED_CAST")
@JvmName("antallAvBarn")
fun Opplysningstype<Int>.antallAv(
    opplysningstype: Opplysningstype<BarnListe>,
    filter: Barn.() -> Boolean,
) = AntallAv(this, opplysningstype as Opplysningstype<ComparableListe<Barn>>, filter)

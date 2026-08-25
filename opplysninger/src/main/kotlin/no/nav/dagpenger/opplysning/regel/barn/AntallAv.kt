package no.nav.dagpenger.opplysning.regel.barn

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.verdier.BarnListe
import java.time.LocalDate

class AntallAv(
    produserer: Opplysningstype<Int>,
    val opplysningstype: Opplysningstype<BarnListe>,
) : Regel<Int>(produserer, listOf(opplysningstype)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Int {
        val liste = opplysninger.finnOpplysning(opplysningstype).verdi.barn
        return liste.size
    }

    override fun toString() = "Produserer $produserer ved å se på antall barn som oppfyller filteret."
}

@Suppress("UNCHECKED_CAST")
@JvmName("antallAvBarn")
fun Opplysningstype<Int>.antallAv(opplysningstype: Opplysningstype<BarnListe>) = AntallAv(this, opplysningstype)

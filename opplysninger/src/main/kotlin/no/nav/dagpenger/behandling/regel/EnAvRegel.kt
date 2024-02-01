package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.LesbarOpplysninger
import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett
import java.time.LocalDate

internal class EnAvRegel(
    produserer: Opplysningstype<Boolean>,
    private vararg val opplysningstyper: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean {
        return opplysninger.finnAlle(opplysningstyper.toList()).any { it.verdi as Boolean }
    }

    override fun toString(): String {
        return "En av ${opplysningstyper.joinToString(", ")} er sanne"
    }
}

fun Regelsett.enAvRegel(
    gjelderFra: LocalDate,
    produserer: Opplysningstype<Boolean>,
    vararg opplysningstype: Opplysningstype<Boolean>,
): Regel<Boolean> {
    return EnAvRegel(produserer, *opplysningstype).also { leggTil(gjelderFra, produserer, it) }
}

fun Regelsett.enAvRegel(
    produserer: Opplysningstype<Boolean>,
    vararg opplysningstype: Opplysningstype<Boolean>,
) = enAvRegel(LocalDate.MIN, produserer, *opplysningstype)

package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class Alle internal constructor(
    produserer: Opplysningstype<Boolean>,
    private vararg val opplysningstyper: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, opplysningstyper.toList()) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ) = opplysninger.finnAlle(opplysningstyper.toList()).all { it.verdi }

    override fun toString() = "Sjekker om alle ${opplysningstyper.joinToString(", ")} er sanne"
}

fun Opplysningstype<Boolean>.alle(vararg opplysningstype: Opplysningstype<Boolean>) = Alle(this, *opplysningstype)

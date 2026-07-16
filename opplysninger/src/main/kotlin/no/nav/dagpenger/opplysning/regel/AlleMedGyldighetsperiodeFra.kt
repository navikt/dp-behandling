package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

// Som Alle, men lar gyldighetsperioden arves eksplisitt fra én navngitt avhengighet
// (periodeFra) i stedet for å regnes ut fra alle vilkårene samlet. Nyttig når vilkårene
// selv ikke bærer riktig gyldighetsperiode, men en annen opplysning i regelsettet gjør det.
class AlleMedGyldighetsperiodeFra internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val vilkår: List<Opplysningstype<Boolean>>,
    periodeFra: Opplysningstype<*>,
) : Regel<Boolean>(produserer, vilkår + periodeFra) {
    override fun kjør(opplysninger: LesbarOpplysninger) = opplysninger.finnAlle(vilkår).all { it.verdi }

    override fun toString() = "Sjekker om alle ${vilkår.joinToString(", ")} er sanne"
}

fun Opplysningstype<Boolean>.alleMedGyldighetsperiodeFra(
    vararg vilkår: Opplysningstype<Boolean>,
    periodeFra: Opplysningstype<*>,
) = AlleMedGyldighetsperiodeFra(this, vilkår.toList(), periodeFra)

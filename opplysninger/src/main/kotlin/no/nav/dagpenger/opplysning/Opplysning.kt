package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.Regel
import java.util.UUID

data class Utledning(
    val regel: Regel<*>,
    val opplysninger: List<Opplysning<*>>,
)

data class Kilde(
    val meldingsreferanseId: UUID,
)

sealed class Opplysning<T : Comparable<T>>(
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val gyldighetsperiode: no.nav.dagpenger.opplysning.Gyldighetsperiode,
    val utledetAv: Utledning?,
    val kilde: Kilde?,
) : Klassifiserbart by opplysningstype {
    abstract fun bekreft(): Faktum<T>

    override fun toString() = "${javaClass.simpleName} om ${opplysningstype.navn} har verdi: $verdi som er $gyldighetsperiode"

    fun sammeSom(opplysning: Opplysning<*>): Boolean {
        return opplysningstype == opplysning.opplysningstype && gyldighetsperiode.overlapp(opplysning.gyldighetsperiode)
    }
}

class Hypotese<T : Comparable<T>>(
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: no.nav.dagpenger.opplysning.Gyldighetsperiode = no.nav.dagpenger.opplysning.Gyldighetsperiode(),
    utledetAv: Utledning? = null,
    kilde: Kilde? = null,
) : Opplysning<T>(opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde) {
    override fun bekreft() = Faktum(super.opplysningstype, verdi, gyldighetsperiode, utledetAv)
}

class Faktum<T : Comparable<T>>(
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: no.nav.dagpenger.opplysning.Gyldighetsperiode = no.nav.dagpenger.opplysning.Gyldighetsperiode(),
    utledetAv: Utledning? = null,
    kilde: Kilde? = null,
) : Opplysning<T>(opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde) {
    override fun bekreft() = this
}
package no.nav.dagpenger.opplysning

/**
 * Noe som må være oppfylt før et regelsett er aktuelt å kjøre. Brukes kun til planlegging:
 * en forutsetning som ikke er oppfylt hindrer at regelsettet kjører, men skjuler aldri opplysninger.
 */
fun interface Forutsetning {
    fun erOppfylt(opplysninger: LesbarOpplysninger): Boolean

    companion object {
        /** Navngitt gruppe av forutsetninger som kan gjenbrukes på tvers av regelsett. */
        fun port(vurdering: (LesbarOpplysninger) -> Boolean) = Forutsetning(vurdering)

        fun alle(vararg forutsetninger: Forutsetning) = Forutsetning { o -> forutsetninger.all { it.erOppfylt(o) } }

        fun enAv(vararg forutsetninger: Forutsetning) = Forutsetning { o -> forutsetninger.any { it.erOppfylt(o) } }

        /** Opplysningen finnes og er sann. */
        fun erSann(opplysning: Opplysningstype<Boolean>) = Forutsetning { it.erSann(opplysning) }
    }
}

/** Vilkåret er oppfylt: utfallet finnes og er sant. Ukjent utfall gir at forutsetningen ikke er oppfylt. */
val Regelsett.oppfylt: Forutsetning
    get() {
        val utfall = vilkårsutfall()
        return Forutsetning { it.erSann(utfall) }
    }

/** Vilkåret er vurdert: utfallet finnes, uavhengig av verdi. */
val Regelsett.vurdert: Forutsetning
    get() {
        val utfall = vilkårsutfall()
        return Forutsetning { it.har(utfall) }
    }

private fun Regelsett.vilkårsutfall(): Opplysningstype<Boolean> {
    require(type == RegelsettType.Vilkår) { "Kun vilkår kan brukes som forutsetning, $navn er $type" }
    return requireNotNull(utfall) { "Vilkåret $navn mangler utfall" }
}

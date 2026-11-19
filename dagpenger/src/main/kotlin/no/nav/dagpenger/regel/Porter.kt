package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Forutsetning
import no.nav.dagpenger.opplysning.oppfylt
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt

/** Navngitte forutsetninger som brukes av flere regelsett via `forutsetter(...)`. */
object Porter {
    /** Alle vilkår som påvirker resultatet er oppfylt. */
    val kravPåDagpenger: Forutsetning = Forutsetning { kravPåDagpenger(it) }

    /** Minsteinntekt eller verneplikt er oppfylt. */
    val minsteinntektEllerVerneplikt: Forutsetning by lazy {
        Forutsetning.enAv(Minsteinntekt.regelsett.oppfylt, Verneplikt.regelsett.oppfylt)
    }

    val skalVernepliktVurderes: Forutsetning = Forutsetning.erSann(Rettighetstype.skalVernepliktVurderes)
}

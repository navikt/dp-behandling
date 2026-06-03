package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.KvotetellingsSkriver
import no.nav.dagpenger.regel.Kvotetellingsresultat

internal class KvoteLagring {
    fun lagre(
        opplysninger: Opplysninger,
        kvotetellinger: List<KvoteTelling>,
    ) {
        kvotetellinger.forEach { kvotetelling ->
            KvotetellingsSkriver(kvotetelling.kvote).skriv(opplysninger, kvotetelling.resultat)
        }
    }
}

internal data class KvoteTelling(
    val kvote: KvoteDefinisjon,
    val resultat: Kvotetellingsresultat,
)

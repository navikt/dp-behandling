package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.Regel

internal class Avhengighetsgraf(
    regler: Set<Regel<*>>,
) {
    // Mapper hver regel sin output-type til den tilhørende regelen for rask oppslag
    private val reglerEtterOutput = regler.associateBy { it.produserer }

    /**
     * Finner alle mulige opplysninger som blir brukt for å produsere ønsket resultat.
     *
     * Gitt ett regeltre hvor A -> B -> C, og vi ønsker C, vil denne metoden returnere A, B og C.
     * Opplysninger som ikke brukes for å produsere C vil bli ikke bli med.
     */
    fun nødvendigeOpplysninger(
        opplysninger: Opplysninger,
        ønsketResultat: List<Opplysningstype<*>>,
    ): Set<Opplysningstype<*>> {
        val nødvendigeOpplysninger = mutableSetOf<Opplysningstype<*>>()

        // Identifiser eksisterende opplysninger som ikke produseres av noen regel
        val opplysningerUtenRegel = opplysninger.finnAlle().map { it.opplysningstype }.filter { it !in reglerEtterOutput }
        nødvendigeOpplysninger.addAll(opplysningerUtenRegel)

        // Traverser fra ønskede outputs og legg til avhengigheter
        for (ønsket in ønsketResultat) {
            val regel = reglerEtterOutput[ønsket] ?: throw IllegalStateException("Fant ikke regel for $ønsket")
            nødvendigeOpplysninger.add(regel.produserer)
            leggTilAvhengigheter(regel, nødvendigeOpplysninger)
        }

        return nødvendigeOpplysninger
    }

    /**
     * Legger rekursivt til avhengigheter for en gitt regel i mengden av nødvendige opplysninger.
     */
    private fun leggTilAvhengigheter(
        regel: Regel<*>,
        nødvendigeOpplysninger: MutableSet<Opplysningstype<*>>,
    ) {
        for (avhengighet in regel.avhengerAv) {
            val avhengigRegel = reglerEtterOutput[avhengighet] ?: throw IllegalStateException("Fant ikke regel for $avhengighet")

            if (nødvendigeOpplysninger.add(avhengigRegel.produserer)) {
                leggTilAvhengigheter(avhengigRegel, nødvendigeOpplysninger)
            }
        }
    }
}

package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.Regel

internal class Avhengighetsgraf(
    regler: Set<Regel<*>>,
) {
    // Mapper hver regel sin output-type til den tilhørende regelen for rask oppslag
    private val produsenter = regler.associateBy { it.produserer }

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
        nødvendigeOpplysninger.addAll(ønsketResultat)

        // Identifiser eksisterende opplysninger som ikke produseres av noen regel
        val opplysningerUtenRegel = opplysninger.somListe().map { it.opplysningstype }.filter { it !in produsenter }
        nødvendigeOpplysninger.addAll(opplysningerUtenRegel)

        // Traverser fra ønskede outputs og legg til avhengigheter
        for (ønsket in ønsketResultat) {
            val regel = produsenter[ønsket] ?: throw IllegalStateException("Fant ikke regel for $ønsket")
            nødvendigeOpplysninger.add(regel.produserer)
            leggTilAvhengigheter(regel, nødvendigeOpplysninger)
        }

        return nødvendigeOpplysninger
    }

    fun finnAlleProdusenter(
        ønsket: List<Opplysningstype<*>>,
        opplysninger: LesbarOpplysninger,
    ): Set<Regel<*>> {
        val besøkt = mutableSetOf<Opplysningstype<*>>()
        val resultat = mutableSetOf<Regel<*>>()
        val queue = ArrayDeque<Opplysningstype<*>>()

        queue.addAll(ønsket)

        while (queue.isNotEmpty()) {
            val opplysning = queue.removeFirst()
            if (!besøkt.add(opplysning)) continue // allerede besøkt

            // Ikke drill ned i overstyrte opplysninger
            if (opplysninger.erOverstyrt(opplysning)) continue

            val produsent = produsenter[opplysning] ?: continue
            resultat.add(produsent)

            queue.addAll(produsent.avhengerAv)
        }

        return resultat
    }

    /**
     * Legger rekursivt til avhengigheter for en gitt regel i mengden av nødvendige opplysninger.
     */
    private fun leggTilAvhengigheter(
        regel: Regel<*>,
        nødvendigeOpplysninger: MutableSet<Opplysningstype<*>>,
    ) {
        for (avhengighet in regel.avhengerAv) {
            val avhengigRegel = produsenter[avhengighet] ?: throw IllegalStateException("Fant ikke regel for $avhengighet")

            if (nødvendigeOpplysninger.add(avhengigRegel.produserer)) {
                leggTilAvhengigheter(avhengigRegel, nødvendigeOpplysninger)
            }
        }
    }
}

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
     * Betingede regler bidrar kun med avhengighetene som er aktive for [gjeldende].
     * Opplysninger som ikke brukes for å produsere C vil bli ikke bli med.
     */
    fun nødvendigeOpplysninger(
        opplysninger: Opplysninger,
        ønsketResultat: Collection<Opplysningstype<*>>,
        gjeldende: LesbarOpplysninger = opplysninger,
    ): Set<Opplysningstype<*>> {
        // Identifiser eksisterende opplysninger som ikke produseres av noen regel
        val opplysningerUtenRegel = opplysninger.typerMedOpplysning().filter { it !in reglerEtterOutput }
        return aktivGraf(ønsketResultat, gjeldende).noder + opplysningerUtenRegel
    }

    /**
     * Opplysninger som kan nås fra [ønsketResultat] via en gren som er valgt bort for [gjeldende],
     * og som ikke også nås via en aktiv sti. Typer utenfor ønsket resultat er ikke lukket, bare ikke ønsket.
     */
    fun lukkedeOpplysninger(
        ønsketResultat: Collection<Opplysningstype<*>>,
        gjeldende: LesbarOpplysninger,
    ): Set<Opplysningstype<*>> {
        val mulige = mutableSetOf<Opplysningstype<*>>()
        val kø = ArrayDeque(ønsketResultat)
        while (kø.isNotEmpty()) {
            val type = kø.removeFirst()
            if (!mulige.add(type)) continue
            reglerEtterOutput[type]?.let { kø.addAll(it.avhengerAv) }
        }
        return mulige - aktivGraf(ønsketResultat, gjeldende).noder
    }

    /**
     * Bygger grafen av avhengigheter som er aktive for [gjeldende], fra [ønsketResultat] og bakover.
     * Sammenlign grafer fra to runder med [AktivGraf.lukketSammenlignetMed] for å finne stier som er lukket.
     */
    fun aktivGraf(
        ønsketResultat: Collection<Opplysningstype<*>>,
        gjeldende: LesbarOpplysninger,
    ): AktivGraf {
        val kanter = linkedMapOf<Opplysningstype<*>, List<Opplysningstype<*>>>()
        val kø = ArrayDeque(ønsketResultat)
        while (kø.isNotEmpty()) {
            val type = kø.removeFirst()
            if (type in kanter) continue
            val regel = reglerEtterOutput[type] ?: throw IllegalStateException("Fant ikke regel for $type")
            val aktive = regel.aktiveAvhengigheter(gjeldende)
            kanter[type] = aktive
            kø.addAll(aktive)
        }
        return AktivGraf(kanter)
    }

    class AktivGraf(
        val kanter: Map<Opplysningstype<*>, List<Opplysningstype<*>>>,
    ) {
        val noder: Set<Opplysningstype<*>> get() = kanter.keys

        // Opplysninger som var nådbare før og ikke lenger er det, f.eks. fordi en gren er valgt bort
        fun lukketSammenlignetMed(forrige: AktivGraf): Set<Opplysningstype<*>> = forrige.noder - noder
    }
}

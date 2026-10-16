package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Tidslinje
import java.time.LocalDate

/**
 * Ny rettighetsperiode utledet fra kandidatperiodene, klar til å bli til en `Faktum` og lagt til
 * i `Opplysninger`. Rent domenedata - ingen kjennskap til hvordan den skal lagres eller logges.
 */
data class NyRettighetsperiode(
    val gyldighetsperiode: Gyldighetsperiode,
    val verdi: Boolean,
)

/**
 * Domenelogikken for hvilke rettighetsperioder som faktisk skal bli til `harLøpendeRett`, gitt
 * eksisterende perioder og nye kandidatperioder fra `TidslinjeBygger`. Ren funksjon - verken
 * `Opplysninger` eller `Prosesskontekst` er involvert her.
 */
object RettighetsperiodeUtleder {
    fun utledNyeRettighetsperioder(
        eksisterende: List<Opplysning<Boolean>>,
        kandidatperioder: Tidslinje<Boolean>,
        overskrivingsStrategi: PeriodeOverskrivingsStrategi,
    ): List<NyRettighetsperiode> {
        // Vi trenger siste eksisterende periode for å avgjøre om første kandidatperiode er kant-i-kant med den
        val sisteEksisterende =
            eksisterende
                .maxByOrNull { it.gyldighetsperiode.fraOgMed }
                ?.let { NyRettighetsperiode(it.gyldighetsperiode, it.verdi) }

        return kandidatperioder
            .fold(emptyList<NyRettighetsperiode>() to sisteEksisterende) { (resultat, forrige), periode ->
                val gyldighetsperiode = Gyldighetsperiode(periode.fraOgMed, periode.tilOgMed)
                require(gyldighetsperiode.harStartdato) { "Rettighetsperioder kan ikke begynne fra LocalDate.MIN" }

                // Ikke legg til perioder som har lik fra- og med eksisterende perioder med samme verdi
                // Denne unngår at vi legger til en forkortet rettighetsperiode men lener oss på "uterstatning" logikk i opplysninger.
                if (overskrivingsStrategi.skalIkkeLeggesTil(eksisterende, gyldighetsperiode, periode)) {
                    return@fold resultat to forrige
                }

                // Kant-i-kant-kandidat med lik verdi som forrige skal utvide den perioden, ikke bli
                // et eget duplikat. TidslinjeBygger slår normalt sammen slike kant-i-kant-segmenter
                // selv (slåSammenLike=true), så i praksis er det bare Søknadsprosess
                // (slåSammenLike=false) som treffer denne grenen.
                val faktiskGyldighetsperiode =
                    when {
                        erTilstøtendeMedLikVerdi(forrige, gyldighetsperiode, periode.verdi) ->
                            Gyldighetsperiode(forrige!!.gyldighetsperiode.fraOgMed, gyldighetsperiode.tilOgMed)
                        else -> gyldighetsperiode
                    }

                val ny = NyRettighetsperiode(faktiskGyldighetsperiode, periode.verdi)
                (resultat + ny) to ny
            }.first
    }

    /**
     * Er `forrige` kant-i-kant med den nye, åpne perioden, og har samme verdi? Vi ser bevisst bare på
     * den ene forrige perioden - ikke alle eksisterende - slik at vi aldri hopper bakover forbi en
     * reell historisk overgang (f.eks. en stans) og smelter sammen med en eldre periode som
     * tilfeldigvis også grenser til samme dato.
     *
     * For prosesser med slåSammenLike=true (alle unntatt Søknadsprosess) vil TidslinjeBygger
     * allerede ha slått sammen kant-i-kant-kandidater med lik verdi før de når hit, så
     * sammenslåing mot en arvet periode fra en tidligere behandling er i praksis uoppnåelig der
     * (verifisert empirisk i GjenopptakTest og KjedescenarioTest). Denne funksjonen er dermed
     * reelt bare i spill for Søknadsprosess (slåSammenLike=false), samt for å slå sammen flere nye
     * kandidatperioder innad i samme kjøring.
     */
    private fun erTilstøtendeMedLikVerdi(
        forrige: NyRettighetsperiode?,
        gyldighetsperiode: Gyldighetsperiode,
        verdi: Boolean,
    ): Boolean {
        if (forrige == null) return false
        if (gyldighetsperiode.tilOgMed != LocalDate.MAX) return false
        return forrige.verdi == verdi && forrige.gyldighetsperiode.tilstøter(gyldighetsperiode)
    }
}

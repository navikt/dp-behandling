package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelplanlegger
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class Prøvingsdato internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, listOf(dato)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ) = opplysninger.finnOpplysning(dato).verdi

    override fun toString() = "Fastsetter $produserer med verdi $dato og gyldighetsperiode fom $dato"

    // Hvis avhengigheten (f.eks. Søknadstidspunkt) er
    // erstattet, betyr ikke det nødvendigvis at en replanlegging faktisk vil endre noe for DENNE
    // dagen. Siden produktet får sin gyldighetsperiode fra egen verdi (GyldighetsperiodeStrategi.
    // egenVerdi, dvs. fom = produsert verdi), vil en ny kandidatverdi som ligger ETTER dagen som
    // evalueres ALDRI dekke den dagen - og regelen ville da bli replanlagt for alltid uten å
    // konvergere (se RegelkjøringLoopException). Vi "kikker" derfor på hva regelen faktisk ville
    // produsert, og lar replanleggingen gå sin vante gang bare hvis kandidaten kan dekke dagen.
    override fun lagPlanNårAvhengerErErstattet(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        besøkt: MutableSet<Regel<*>>,
    ) {
        val gjelderFor = opplysninger.gjelderFor
        if (gjelderFor != null) {
            val kandidat = kjør(opplysninger, gjelderFor)
            if (kandidat.isAfter(gjelderFor)) {
                // Kandidaten kan uansett aldri dekke dagen som evalueres - behold eksisterende,
                // fortsatt gyldige produkt i stedet for å replanlegge i det uendelige.
                return
            }
        }
        super.lagPlanNårAvhengerErErstattet(opplysninger, plan, produsenter, besøkt)
    }
}

fun Opplysningstype<LocalDate>.prøvingsdato(dato: Opplysningstype<LocalDate>) = Prøvingsdato(this, dato)

package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.Forbrukstype.Sanksjon
import java.time.LocalDate

enum class Forbrukstype {
    Rettighet,
    Sanksjon,
}

data class KvoteDefinisjon(
    val hjemmel: Hjemmel,
    val tildelingsgrunnlag: Tildelingsgrunnlag,
    val tellesNår: Opplysningstype<Boolean>,
    val forbruksteller: Opplysningstype<Int>,
    val gjenstående: Opplysningstype<Int>,
    val sisteForbruk: Opplysningstype<LocalDate>,
    val sisteGjenstående: Opplysningstype<Int>,
    val forbrukstype: Forbrukstype = Forbrukstype.Rettighet,
    val utløsendeBetingelse: Opplysningstype<Boolean>,
) {
    val navn get() = hjemmel.kortnavn

    fun teller(type: Forbrukstype) = type == forbrukstype

    /**
     * Henter tildelt kapasitet slik den var gyldig på [gjelderFor].
     *
     * Kapasiteten kan endres over tid (f.eks. ved nedjustering av tildelingsgrunnlaget i en
     * revurdering/omgjøring). Vi finner derfor den siste kapasiteten som var trådt i kraft senest
     * på [gjelderFor], i stedet for alltid å hente den nyeste/gjeldende verdien - det ville fått
     * historiske meldeperioder til å bli regnet om med feil kapasitet ved en omgjøring.
     *
     * Meldeperioder kan starte noen dager før selve vedtaket/kapasiteten formelt ble gyldig
     * (uke-justerte meldeperioder), så hvis [gjelderFor] er tidligere enn noen kjent kapasitet,
     * faller vi tilbake på den først tildelte kapasiteten.
     */
    fun tildeltKapasitet(
        opplysninger: LesbarOpplysninger,
        gjelderFor: LocalDate,
    ): Int = tildeltKapasitetFra(opplysninger.finnAlle(tildelingsgrunnlag.kapasitet), gjelderFor)

    /**
     * Som [tildeltKapasitet], men slår opp kapasitetslisten kun én gang og returnerer en lett
     * oppslagsfunksjon. Nyttig når kapasiteten trengs for mange datoer (f.eks. dag for dag i en
     * meldeperiode), slik at vi slipper å gjenta det (relativt sett) dyre [LesbarOpplysninger.finnAlle]-
     * oppslaget for hver dag.
     */
    fun tildeltKapasitetOppslag(opplysninger: LesbarOpplysninger): (LocalDate) -> Int {
        val kapasiteter = opplysninger.finnAlle(tildelingsgrunnlag.kapasitet)
        return { gjelderFor -> tildeltKapasitetFra(kapasiteter, gjelderFor) }
    }

    private fun tildeltKapasitetFra(
        kapasiteter: List<Opplysning<Int>>,
        gjelderFor: LocalDate,
    ): Int {
        if (kapasiteter.isEmpty()) return 0
        return kapasiteter
            .lastOrNull { !it.gyldighetsperiode.fraOgMed.isAfter(gjelderFor) }
            ?.verdi
            ?: kapasiteter.minBy { it.gyldighetsperiode.fraOgMed }.verdi
    }

    fun gjenståendeVed(
        opplysninger: LesbarOpplysninger,
        førsteDag: LocalDate,
    ): Int {
        val sisteGjenståendeVerdi =
            opplysninger
                .finnAlle(gjenstående)
                .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(førsteDag) }
                ?.verdi
        return sisteGjenståendeVerdi ?: tildeltKapasitet(opplysninger, førsteDag)
    }

    fun forrigeForbruk(
        opplysninger: LesbarOpplysninger,
        før: LocalDate,
    ): Int =
        opplysninger
            .finnAlle(forbruksteller)
            .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(før) }
            ?.verdi ?: 0
}

data class Tildelingsgrunnlag(
    val kapasitet: Opplysningstype<Int>,
) {
    fun ilagtDato(opplysninger: LesbarOpplysninger): LocalDate? =
        opplysninger
            .finnAlle(kapasitet)
            .filter { it.verdi > 0 }
            .minOfOrNull { it.gyldighetsperiode.fraOgMed }
}

fun List<KvoteDefinisjon>.sanksjonerSortert(opplysninger: LesbarOpplysninger): List<KvoteDefinisjon> =
    filter { it.teller(Sanksjon) }.sortertEtterIlagtDato(opplysninger)

fun List<KvoteDefinisjon>.sortertEtterIlagtDato(opplysninger: LesbarOpplysninger): List<KvoteDefinisjon> =
    sortedWith(compareBy(nullsLast()) { kvote -> kvote.tildelingsgrunnlag.ilagtDato(opplysninger) })

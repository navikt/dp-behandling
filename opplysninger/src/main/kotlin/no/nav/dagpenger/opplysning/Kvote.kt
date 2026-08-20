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
    ): Int = gjeldendeKapasitetFra(opplysninger.finnAlle(tildelingsgrunnlag.kapasitet), gjelderFor).verdi

    /**
     * Som [tildeltKapasitet], men slår opp kapasitetslisten kun én gang og returnerer en lett
     * oppslagsfunksjon som gir gjeldende kapasitet OG virkningsdatoen den ble tildelt fra.
     *
     * Virkningsdatoen brukes til å nullstille forbruksteller når man beveger seg inn i en ny
     * kapasitetsperiode (se [forrigeForbruk] og [no.nav.dagpenger.regel.Kvotetelling.tell]) - en
     * nedjustering/omjustering av tildelingsgrunnlaget starter tellingen på nytt fra og med sin
     * egen virkningsdato, den endrer ikke bare kapasitetstaket.
     *
     * Nyttig når kapasiteten trengs for mange datoer (f.eks. dag for dag i en meldeperiode),
     * slik at vi slipper å gjenta det (relativt sett) dyre [LesbarOpplysninger.finnAlle]-oppslaget
     * for hver dag.
     */
    fun tildeltKapasitetOppslag(opplysninger: LesbarOpplysninger): (LocalDate) -> GjeldendeKapasitet {
        val kapasiteter = opplysninger.finnAlle(tildelingsgrunnlag.kapasitet)
        return { gjelderFor -> gjeldendeKapasitetFra(kapasiteter, gjelderFor) }
    }

    private fun gjeldendeKapasitetFra(
        kapasiteter: List<Opplysning<Int>>,
        gjelderFor: LocalDate,
    ): GjeldendeKapasitet {
        if (kapasiteter.isEmpty()) return GjeldendeKapasitet(0, gjelderFor)
        val sortert = kapasiteter.sortedBy { it.gyldighetsperiode.fraOgMed }
        val gjeldendeIndeks =
            sortert
                .indexOfLast { !it.gyldighetsperiode.fraOgMed.isAfter(gjelderFor) }
                .takeIf { it >= 0 } ?: 0
        val gjeldende = sortert[gjeldendeIndeks]

        // En revurdering kan produsere et nytt faktum med samme verdi som forrige (f.eks. fordi
        // regelmotoren kjøres på nytt av en helt annen grunn enn en endring i kapasiteten selv).
        // Virkningsdatoen for tellingen skal da IKKE flyttes - vi går derfor bakover så lenge
        // verdien er uendret, og bruker den tidligste fraOgMed i den sammenhengende serien med
        // samme verdi som den reelle virkningsdatoen for gjeldende kapasitet.
        var reellVirkningsdato = gjeldende.gyldighetsperiode.fraOgMed
        for (i in gjeldendeIndeks - 1 downTo 0) {
            if (sortert[i].verdi != gjeldende.verdi) break
            reellVirkningsdato = sortert[i].gyldighetsperiode.fraOgMed
        }

        return GjeldendeKapasitet(gjeldende.verdi, reellVirkningsdato)
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

    /**
     * Henter forbruk registrert før [før], men KUN forbruk som er talt innenfor samme
     * kapasitetsperiode som [før] tilhører. Forbruk fra en tidligere kapasitetsperiode (dvs. før
     * gjeldende kapasitets virkningsdato) skal ikke telle med - en ny/endret tildeling av
     * kapasitet starter tellingen på nytt fra sin egen virkningsdato.
     */
    fun forrigeForbruk(
        opplysninger: LesbarOpplysninger,
        før: LocalDate,
    ): Int {
        val virkningsdato = tildeltKapasitetOppslag(opplysninger)(før).virkningsdato
        return opplysninger
            .finnAlle(forbruksteller)
            .lastOrNull { it.gyldighetsperiode.fraOgMed.isBefore(før) && !it.gyldighetsperiode.fraOgMed.isBefore(virkningsdato) }
            ?.verdi ?: 0
    }
}

data class GjeldendeKapasitet(
    val verdi: Int,
    val virkningsdato: LocalDate,
)

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

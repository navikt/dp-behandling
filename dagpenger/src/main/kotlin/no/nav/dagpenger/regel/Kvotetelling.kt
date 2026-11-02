package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.GjeldendeKapasitet
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.regel.regelsett.beregning.Beregningresultat
import java.time.LocalDate

object Kvotetelling {
    /**
     * Teller forbruk. Alle datoer i [dager] teller som +1.
     *
     * [kapasitet] slås opp per dag (ikke som en fast verdi for hele batchen), slik at en
     * nedjustering av tildelingsgrunnlaget med virkning midt i en løpende meldeperiode slår ut
     * fra og med virkningsdatoen, og ikke først fra neste meldeperiode.
     *
     * Når en dags kapasitet har en annen virkningsdato enn foregående dags (dvs. vi beveger oss
     * inn i en ny kapasitetsperiode, f.eks. pga. en nedjustering/omjustering av tildelingsgrunnlaget
     * i en revurdering), starter tellingen på nytt fra 0 - en ny tildeling gjelder for seg selv,
     * og "arver" ikke forbruk fra den forrige kapasitetsperioden.
     */
    fun tell(
        kapasitet: (LocalDate) -> GjeldendeKapasitet,
        utgangspunkt: Int,
        dager: List<LocalDate>,
        beregningsdager: List<Beregningresultat.Beregningsdag>,
    ): Kvotetellingsresultat {
        val sortert = beregningsdager.sortedBy { it.dag.dato }
        var teller = utgangspunkt
        var gjeldendeVirkningsdato: LocalDate? = null
        val forbruktTeller =
            sortert.map { beregningsdag ->
                val dato = beregningsdag.dag.dato
                val (kapasitetPåDato, virkningsdato) = kapasitet(dato)
                if (gjeldendeVirkningsdato != null && virkningsdato != gjeldendeVirkningsdato) {
                    // Krysser inn i en ny kapasitetsperiode - forbruket telles på nytt fra 0.
                    teller = 0
                }
                gjeldendeVirkningsdato = virkningsdato
                if (dato in dager) teller++
                KvotetellingsVerdi(minOf(teller, kapasitetPåDato), Gyldighetsperiode(dato, dato))
            }
        val gjenstående =
            forbruktTeller.map {
                val kapasitetPåDato = kapasitet(it.gyldighetsperiode.fraOgMed).verdi
                val g = maxOf(kapasitetPåDato - it.verdi, 0)
                require(g >= 0) {
                    "Gjenstående kan ikke være negativt. Har $g igjen"
                }
                KvotetellingsVerdi(g, it.gyldighetsperiode)
            }

        val sisteForbruksdato = dager.lastOrNull()

        return Kvotetellingsresultat(
            forbruktTeller = forbruktTeller,
            gjenstående = gjenstående,
            sisteDagMedForbruk =
                KvotetellingsVerdi(
                    sisteForbruksdato ?: beregningsdager.last().dag.dato,
                    Gyldighetsperiode(beregningsdager.last().dag.dato),
                ),
            sisteGjenstående =
                KvotetellingsVerdi(
                    gjenstående.lastOrNull()?.verdi ?: kapasitet(beregningsdager.last().dag.dato).verdi,
                    Gyldighetsperiode(beregningsdager.last().dag.dato),
                ),
        )
    }
}

class KvotetellingsSkriver(
    private val definisjon: KvoteDefinisjon,
) {
    fun skriv(
        kontekst: Prosesskontekst,
        resultat: Kvotetellingsresultat,
    ) {
        val batchensFørsteDag = resultat.forbruktTeller.minOfOrNull { it.gyldighetsperiode.fraOgMed }

        skrivForbruk(kontekst.opplysninger, resultat)

        if (batchensFørsteDag != null) synkroniserUtløsendeBetingelse(kontekst, resultat, batchensFørsteDag)
    }

    private fun skrivForbruk(
        opplysninger: Opplysninger,
        resultat: Kvotetellingsresultat,
    ) {
        opplysninger.leggTil { fakta ->
            resultat.forbruktTeller.forEach { fakta.add(Faktum(definisjon.forbruksteller, it.verdi, it.gyldighetsperiode)) }
            resultat.gjenstående.forEach { fakta.add(Faktum(definisjon.gjenstående, it.verdi, it.gyldighetsperiode)) }
            resultat.sisteDagMedForbruk?.let { fakta.add(Faktum(definisjon.sisteForbruk, it.verdi, it.gyldighetsperiode)) }
            resultat.sisteGjenstående?.let { fakta.add(Faktum(definisjon.sisteGjenstående, it.verdi, it.gyldighetsperiode)) }
        }
    }

    /**
     * Utløsende betingelse er alltid et resultat av siste telling: usann fra dagen etter at kvoten
     * er tom, ellers sann. Verdien skrives kun når den avviker fra det som gjelder fra før.
     * Svinger verdien for alltid stopper rekjøringsløkka i Behandling (MAKS_ITERASJONER).
     */
    private fun synkroniserUtløsendeBetingelse(
        kontekst: Prosesskontekst,
        resultat: Kvotetellingsresultat,
        batchensFørsteDag: LocalDate,
    ) {
        val opplysninger = kontekst.opplysninger
        val betingelse = definisjon.utløsendeBetingelse
        val erOppbrukt = resultat.erOppbrukt
        val ønsketVerdi = !erOppbrukt

        val fraOgMed =
            if (erOppbrukt) {
                resultat.datoOppbrukt.plusDays(1)
            } else {
                gjenåpningsdato(opplysninger, betingelse, batchensFørsteDag) ?: return
            }

        val gjeldende = opplysninger.forDato(fraOgMed).finnNullableOpplysning(betingelse)?.verdi
        if (gjeldende == ønsketVerdi) return
        opplysninger.leggTil(Faktum(betingelse, ønsketVerdi, Gyldighetsperiode(fraOgMed)))

        // Slik at konsekvensene av endret kvotestatus utledes i samme pass, ikke først i neste meldeperiode.
        kontekst.beOmRekjøring()
    }

    // Datoen der en eksisterende usann betingelse skal overstyres med sann. Null hvis kvoten aldri var oppbrukt.
    private fun gjenåpningsdato(
        opplysninger: Opplysninger,
        betingelse: Opplysningstype<Boolean>,
        batchensFørsteDag: LocalDate,
    ): LocalDate? {
        if (opplysninger.forDato(batchensFørsteDag).finnNullableOpplysning(betingelse)?.verdi == false) return batchensFørsteDag
        return opplysninger
            .finnAlle(betingelse)
            .filter { !it.verdi && it.gyldighetsperiode.fraOgMed.isAfter(batchensFørsteDag) }
            .minOfOrNull { it.gyldighetsperiode.fraOgMed }
    }
}

data class Kvotetellingsresultat(
    val forbruktTeller: List<KvotetellingsVerdi<Int>> = emptyList(),
    val gjenstående: List<KvotetellingsVerdi<Int>> = emptyList(),
    val sisteDagMedForbruk: KvotetellingsVerdi<LocalDate>? = null,
    val sisteGjenstående: KvotetellingsVerdi<Int>? = null,
) {
    val erOppbrukt get() = sisteGjenstående?.verdi == 0

    // Hentes fra denne batchens EGET resultat, ikke via opplysninger.finnAlle(...) - sistnevnte ville
    // funnet den første nullverdien i HELE historikken, som blir feil dato dersom kvoten tidligere er
    // brukt opp og siden fått en ny tildeling (f.eks. ved revurdering) som nå er brukt opp på nytt.
    val datoOppbrukt get() = gjenstående.first { it.verdi == 0 }.gyldighetsperiode.fraOgMed
}

data class KvotetellingsVerdi<T : Any>(
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode,
)

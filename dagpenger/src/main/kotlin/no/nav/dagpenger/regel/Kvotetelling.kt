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
        val opplysninger = kontekst.opplysninger
        val gjenståendeFørDenneBatchen = opplysninger.finnNullableOpplysning(definisjon.gjenstående)?.verdi

        skrivForbruk(opplysninger, resultat)

        if (bleNettoppBruktOpp(resultat, gjenståendeFørDenneBatchen)) {
            settUtløsendeBetingelseTilUsann(kontekst, resultat)
        }
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
     * Kvoten er brukt opp først når den faktisk gikk fra >0 til 0 - ikke bare fordi denne batchen
     * *ender* på 0. Om forrige batch etterlot 1 dag igjen, og denne batchen kun inneholder den
     * siste dagen, består gjenstående-lista av ett enkelt 0-element - da må vi vite hva som gjaldt
     * FØR denne batchen for å avgjøre at kvoten faktisk ble brukt opp akkurat nå.
     */
    private fun bleNettoppBruktOpp(
        resultat: Kvotetellingsresultat,
        gjenståendeFørDenneBatchen: Int?,
    ): Boolean {
        val haddeNoeIgjenFørDenneBatchen = gjenståendeFørDenneBatchen == null || gjenståendeFørDenneBatchen > 0
        return resultat.sisteGjenstående?.verdi == 0 && haddeNoeIgjenFørDenneBatchen
    }

    private fun settUtløsendeBetingelseTilUsann(
        kontekst: Prosesskontekst,
        resultat: Kvotetellingsresultat,
    ) {
        val opplysninger = kontekst.opplysninger
        val fraOgMed = datoKvotenBleBruktOpp(resultat).plusDays(1)

        if (opplysninger.erAlleredeSattTilUsannFra(definisjon.utløsendeBetingelse, fraOgMed)) return

        // Skrives bevisst UTEN utledetAv, slik at regelmotoren behandler verdien som ferdig avgjort
        // (på samme måte som en saksbehandleroverstyring), og ikke prøver å avlede den på nytt fra
        // utløsendeBetingelses egen regel ved neste regelkjøring.
        opplysninger.leggTil(Faktum(definisjon.utløsendeBetingelse, false, Gyldighetsperiode(fraOgMed)))

        // Slik at konsekvensene av at kvoten nå er brukt opp (f.eks. § 4-3 sitt krav til tap av
        // arbeidsinntekt for permittering) utledes i samme pass, ikke først i neste meldeperiode.
        kontekst.beOmRekjøring()
    }

    // Hentes fra denne batchens EGET resultat, ikke via opplysninger.finnAlle(...) - sistnevnte ville
    // funnet den første nullverdien i HELE historikken, som blir feil dato dersom kvoten tidligere er
    // brukt opp og siden fått en ny tildeling (f.eks. ved revurdering) som nå er brukt opp på nytt.
    private fun datoKvotenBleBruktOpp(resultat: Kvotetellingsresultat) =
        resultat.gjenstående
            .first { it.verdi == 0 }
            .gyldighetsperiode.fraOgMed

    private fun Opplysninger.erAlleredeSattTilUsannFra(
        utløsendeBetingelse: Opplysningstype<Boolean>,
        fraOgMed: LocalDate,
    ): Boolean = har(utløsendeBetingelse, fraOgMed) && !finnOpplysning(utløsendeBetingelse, fraOgMed).verdi
}

data class Kvotetellingsresultat(
    val forbruktTeller: List<KvotetellingsVerdi<Int>> = emptyList(),
    val gjenstående: List<KvotetellingsVerdi<Int>> = emptyList(),
    val sisteDagMedForbruk: KvotetellingsVerdi<LocalDate>? = null,
    val sisteGjenstående: KvotetellingsVerdi<Int>? = null,
)

data class KvotetellingsVerdi<T : Any>(
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode,
)

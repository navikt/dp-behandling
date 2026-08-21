package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.GjeldendeKapasitet
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysninger
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
        opplysninger: Opplysninger,
        resultat: Kvotetellingsresultat,
    ) {
        opplysninger.leggTil { fakta ->
            resultat.forbruktTeller.forEach { fakta.add(Faktum(definisjon.forbruksteller, it.verdi, it.gyldighetsperiode)) }
            resultat.gjenstående.forEach { fakta.add(Faktum(definisjon.gjenstående, it.verdi, it.gyldighetsperiode)) }
            resultat.sisteDagMedForbruk?.let { fakta.add(Faktum(definisjon.sisteForbruk, it.verdi, it.gyldighetsperiode)) }
            resultat.sisteGjenstående?.let { fakta.add(Faktum(definisjon.sisteGjenstående, it.verdi, it.gyldighetsperiode)) }

            if (resultat.sisteGjenstående?.verdi == 0 && resultat.gjenstående.any { it.verdi != 0 }) {
                val sisteDag = resultat.sisteDagMedForbruk!!.verdi
                fakta.add(Faktum(definisjon.utløsendeBetingelse, false, Gyldighetsperiode(sisteDag.plusDays(1))))
            }
        }
    }
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

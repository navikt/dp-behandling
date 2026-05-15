package no.nav.dagpenger.regel.prosess
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import java.time.LocalDate

/**
 * Utleder prøvingsdato fra opplysninger tilgjengelig i behandlingen.
 *
 * Prøvingsdato bestemmer hvilken dato regelkjøringen evaluerer vilkår på.
 * Den utledes gjennom en fallback-kjede, avhengig av hvor langt behandlingen har kommet:
 *
 * 1. **Prøvingsdato** — utledet fra søknadstidspunkt, kan overstyres av PrøvingsdatoPlugin eller saksbehandler
 * 2. **Søknadstidspunkt** — max(søknadsdato, ønsketdato), utledet av regelmotor
 * 3. **max(søknadsdato, ønsketdato)** — beregnet manuelt før søknadstidspunkt er utledet
 * 4. **Siste opplysnings gyldighetsperiode** — tidlig i prosessen, før datoer er innhentet
 *
 * Bruker kun EGNE opplysninger (ikke arvede fra tidligere behandlinger) for å
 * unngå at gjenopptak/kjede-behandlinger arver forrige behandlings prøvingsdato.
 */
object PrøvingsdatoUtleder {
    private val logger = KotlinLogging.logger { }

    fun utled(opplysninger: LesbarOpplysninger): LocalDate {
        val egne = opplysninger.kunEgne
        return fastsattPrøvingsdato(egne)
            ?: utledetSøknadstidspunkt(egne)
            ?: beregnetSøknadstidspunkt(egne)
            ?: sisteTilgjengeligeDato(egne)
            ?: throw IllegalStateException("Ingen opplysninger med startdato tilgjengelig for å bestemme prøvingsdato")
    }

    /**
     * Nivå 1: Prøvingsdato er allerede fastsatt (utledet av regelmotoren, plugin, eller saksbehandler).
     */
    private fun fastsattPrøvingsdato(egne: LesbarOpplysninger): LocalDate? =
        egne.hvisHar(Søknadstidspunkt.prøvingsdato)?.also {
            logger.info { "Prøvingsdato utledet fra fastsatt prøvingsdato: $it" }
        }

    /**
     * Nivå 2: Søknadstidspunkt er utledet = max(søknadsdato, ønsketdato).
     */
    private fun utledetSøknadstidspunkt(egne: LesbarOpplysninger): LocalDate? =
        egne.hvisHar(Søknadstidspunkt.søknadstidspunkt)?.also {
            logger.info { "Prøvingsdato utledet fra søknadstidspunkt: $it" }
        }

    /**
     * Nivå 3: Søknadsdato og/eller ønsketdato finnes, men søknadstidspunkt er ikke utledet ennå.
     * Beregner max(søknadsdato, ønsketdato) manuelt — tilsvarer det søknadstidspunkt-regelen ville gjort.
     */
    private fun beregnetSøknadstidspunkt(egne: LesbarOpplysninger): LocalDate? {
        val søknadsdato = egne.hvisHar(Søknadstidspunkt.søknadsdato)
        val ønsketdato = egne.hvisHar(Søknadstidspunkt.ønsketdato)
        val kandidater = listOfNotNull(søknadsdato, ønsketdato)
        return kandidater.maxOrNull()?.also {
            logger.info { "Prøvingsdato beregnet fra søknadsdato=$søknadsdato, ønsketdato=$ønsketdato → $it" }
        }
    }

    /**
     * Nivå 4: Svært tidlig i prosessen — ingen datoer er innhentet ennå.
     * Faller tilbake til gyldighetsperioden til siste egne opplysning (typisk søknadId).
     */
    private fun sisteTilgjengeligeDato(egne: LesbarOpplysninger): LocalDate? =
        egne
            .somListe()
            .lastOrNull { it.gyldighetsperiode.harStartdato }
            ?.gyldighetsperiode
            ?.fraOgMed
            ?.also { logger.info { "Prøvingsdato fra siste opplysnings gyldighetsperiode: $it" } }

    private fun LesbarOpplysninger.hvisHar(opplysningstype: no.nav.dagpenger.opplysning.Opplysningstype<LocalDate>): LocalDate? =
        if (har(opplysningstype)) finnOpplysning(opplysningstype).verdi else null
}

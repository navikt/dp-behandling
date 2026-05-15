package no.nav.dagpenger.regel.prosess
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt

class PrøvingsdatoPlugin : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        val opplysninger = kontekst.opplysninger
        val egne = opplysninger.kunEgne

        // Om saksbehandler har pilla, skal vi ikke overstyre med automatikk
        val harPerioder = egne.har(KravPåDagpenger.harLøpendeRett)
        val harPilla = harPerioder && egne.finnOpplysning(KravPåDagpenger.harLøpendeRett).kilde is Saksbehandlerkilde
        if (harPilla) return

        // Finn første innvilgelsesperiode (støtter multi-periode: vi bryr oss om den første)
        val førsteInnvilgelse =
            egne.finnAlle(KravPåDagpenger.harLøpendeRett).firstOrNull { it.verdi }
                ?: return

        val gjeldende = opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato)
        val nyDato = førsteInnvilgelse.gyldighetsperiode.fraOgMed

        // Allerede korrekt — ingen endring nødvendig
        if (gjeldende.verdi.isEqual(nyDato)) return

        // Monotoni-garanti: plugin kan bare flytte prøvingsdato fremover.
        // Å flytte bakover krever eksplisitt saksbehandler-handling.
        // Dette sikrer konvergens: datoen beveger seg monotont fremover og stabiliseres.
        if (nyDato.isBefore(gjeldende.verdi)) {
            logger.info { "PrøvingsdatoPlugin: Ignorerer flytting bakover fra ${gjeldende.verdi} til $nyDato" }
            return
        }

        logger.info { "PrøvingsdatoPlugin: Flytter prøvingsdato fra ${gjeldende.verdi} til $nyDato" }
        opplysninger.leggTil(
            Faktum(
                Søknadstidspunkt.prøvingsdato,
                nyDato,
                Gyldighetsperiode(nyDato),
            ),
        )

        kontekst.beOmRekjøring()
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

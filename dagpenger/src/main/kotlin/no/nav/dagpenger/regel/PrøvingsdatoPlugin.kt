package no.nav.dagpenger.regel

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Saksbehandlerkilde

class PrøvingsdatoPlugin : ProsessPlugin {
    override fun underveis(opplysninger: Opplysninger) {
        val egne = opplysninger.kunEgne

        // Om saksbehandler har pilla, skal vi ikke overstyre med automatikk
        val harPerioder = egne.har(KravPåDagpenger.harLøpendeRett)
        val harPilla = harPerioder && egne.finnOpplysning(KravPåDagpenger.harLøpendeRett).kilde is Saksbehandlerkilde
        if (harPilla) return

        // Automatisk setter prøvingsdato til første mulige innvilgelse
        egne.finnAlle(KravPåDagpenger.harLøpendeRett).firstOrNull { it.verdi }?.let {
            val gjeldende = opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato)
            // Hvis prøvingsdato allerede er satt til riktig dato, gjør ingenting
            if (gjeldende.verdi.isEqual(it.gyldighetsperiode.fraOgMed)) return@let

            opplysninger.leggTil(
                Faktum(
                    Søknadstidspunkt.prøvingsdato,
                    it.gyldighetsperiode.fraOgMed,
                    Gyldighetsperiode(it.gyldighetsperiode.fraOgMed),
                ),
            )
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.tekst
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi.Companion.egenVerdi
import no.nav.dagpenger.opplysning.regel.dato.sisteAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.Behov
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.forskriftTilFolketrygden
import no.nav.dagpenger.regel.kravPåDagpenger
import java.time.LocalDate

object Søknadstidspunkt {
    // § 3A-1.Søknadstidspunkt https://lovdata.no/forskrift/1998-09-16-890/§3a-1
    val søknadsdato = dato(OpplysningsTyper.søknadsdatoId, "Søknadsdato", behovId = Behov.Søknadsdato)
    val ønsketdato =
        dato(
            OpplysningsTyper.ønskerDagpengerFraDatoId,
            "Ønsker dagpenger fra dato",
            behovId = Behov.ØnskerDagpengerFraDato,
        )

    val søknadstidspunkt =
        dato(
            OpplysningsTyper.søknadstidspunktId,
            "Søknadstidspunkt",
            synlig = Opplysningstype.Companion.aldriSynlig,
        )
    val prøvingsdato =
        dato(
            OpplysningsTyper.prøvingsdatoId,
            "Prøvingsdato",
            behovId = Behov.Prøvingsdato,
            gyldighetsperiode = egenVerdi,
        )
    val søknadIdOpplysningstype = tekst(OpplysningsTyper.søknadId, "søknadId")

    val regelsett =
        fastsettelse(
            forskriftTilFolketrygden.hjemmel(3, 1, "Søknadstidspunkt", "Søknadstidspunkt"),
        ) {
            regel(søknadIdOpplysningstype) { innhentes }
            regel(søknadsdato) { innhentMed(søknadIdOpplysningstype) }
            regel(ønsketdato) { innhentMed(søknadIdOpplysningstype) }
            regel(søknadstidspunkt) { sisteAv(søknadsdato, ønsketdato) }
            regel(prøvingsdato) { sisteAv(søknadstidspunkt) }

            avklaring(Avklaringspunkter.VirkningstidspunktForLangtFramITid)
        }

    val VirkningstidspunktForLangtFremITid =
        Kontrollpunkt(Avklaringspunkter.VirkningstidspunktForLangtFramITid) {
            it.har(prøvingsdato) &&
                it.finnOpplysning(prøvingsdato).verdi.isAfter(
                    LocalDate.now().plusDays(14),
                )
        }

    val SjekkPrøvingsdato =
        Kontrollpunkt(Avklaringspunkter.SjekkPrøvingsdato) {
            it.har(prøvingsdato) && kravPåDagpenger(it) &&
                it.finnOpplysning(prøvingsdato).kilde !is Saksbehandlerkilde
        }
}

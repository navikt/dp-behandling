package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.permitteringsperiodeId
import no.nav.dagpenger.regel.Permittering.oppfyllerKravetTilPermittering
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.folketrygden

object PermitteringFastsetting {
    val permitteringsperiode =
        heltall(permitteringsperiodeId, "Periode som gis ved permittering", synlig = { it.erSann(oppfyllerKravetTilPermittering) })

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 7, "Dagpenger til permitterte", "Permittering"),
        ) {
            skalVurderes { it.erSann(oppfyllerKravetTilPermittering) }

            regel(permitteringsperiode) { oppslag(prøvingsdato) { 26 } }

            påvirkerResultat { it.erSann(oppfyllerKravetTilPermittering) }
        }
}

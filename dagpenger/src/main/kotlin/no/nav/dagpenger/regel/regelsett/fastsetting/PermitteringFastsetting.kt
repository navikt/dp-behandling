package no.nav.dagpenger.regel.regelsett.fastsetting
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper.permitteringsperiodeId
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering.oppfyllerKravetTilPermittering
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato

object PermitteringFastsetting {
    val permitteringsperiode =
        heltall(permitteringsperiodeId, "Periode som gis ved permittering", synlig = {
            it.erSann(oppfyllerKravetTilPermittering)
        }, enhet = Enhet.Uker)

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 7, "Dagpenger til permitterte", "Permittering"),
        ) {
            skalVurderes { it.erSann(oppfyllerKravetTilPermittering) }

            regel(permitteringsperiode) { oppslag(prøvingsdato) { 26 } }

            ønsketResultat(permitteringsperiode)
            påvirkerResultat { it.erSann(oppfyllerKravetTilPermittering) }
        }
}

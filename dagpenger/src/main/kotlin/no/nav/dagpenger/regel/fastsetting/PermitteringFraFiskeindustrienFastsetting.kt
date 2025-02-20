package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.permitteringFraFiskeindustriPeriodeId
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.oppfyllerKravetTilPermitteringFiskeindustri
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.folketrygden

object PermitteringFraFiskeindustrienFastsetting {
    val permitteringFraFiskeindustriPeriode =
        heltall(
            id = permitteringFraFiskeindustriPeriodeId,
            beskrivelse = "Periode som gis ved permittering fra fiskeindustrien",
            synlig = {
                it.erSann(
                    oppfyllerKravetTilPermitteringFiskeindustri,
                )
            },
        )

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(
                kapittel = 6,
                paragraf = 7,
                tittel = "Permittering i fiskeforedlingsindustrien, sjømatindustrien og fiskeoljeindustrien",
                kortnavn = "Permittering fiskeindustri",
            ),
        ) {
            skalVurderes { it.erSann(oppfyllerKravetTilPermitteringFiskeindustri) }

            regel(permitteringFraFiskeindustriPeriode) { oppslag(prøvingsdato) { 52 } }

            ønsketResultat(permitteringFraFiskeindustriPeriode)
            påvirkerResultat { it.erSann(oppfyllerKravetTilPermitteringFiskeindustri) }
        }
}

package no.nav.dagpenger.regel.regelsett.fastsetting

import no.nav.dagpenger.opplysning.Forbrukstype
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.Tildelingsgrunnlag
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.tomRegel
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper.antallPermitteringsdagerId
import no.nav.dagpenger.regel.OpplysningsTyper.forbruktPermitteringId
import no.nav.dagpenger.regel.OpplysningsTyper.gjenståendePermitteringId
import no.nav.dagpenger.regel.OpplysningsTyper.permitteringsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.permitteringsperiodeId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteGjenståendePermitteringId
import no.nav.dagpenger.regel.OpplysningsTyper.sistePermitteringsdagId
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.dagerIUka
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering.oppfyllerKravetTilPermittering
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato

object PermitteringFastsetting {
    val permitteringsdag = Opplysningstype.boolsk(permitteringsdagId, "Arbeidsdag hvor bruker har vært permittert")

    val permitteringsperiode =
        heltall(permitteringsperiodeId, "Uker med fritak fra arbeidsplikt", enhet = Enhet.Uker)

    private val antallPermitteringsdager =
        heltall(antallPermitteringsdagerId, "Dager med fritak fra arbeidsplikt", synlig = aldriSynlig, enhet = Enhet.Dager)

    val forbruktPermittering = heltall(forbruktPermitteringId, "Antall fritaksperiodedager som er forbrukt", enhet = Enhet.Dager)
    val gjenståendePermittering = heltall(gjenståendePermitteringId, "Antall fritaksperiodedager som gjenstår", enhet = Enhet.Dager)
    private val sistePermitteringsdag = dato(sistePermitteringsdagId, "Siste forbruksdato av fritaksperiode", synlig = aldriSynlig)
    private val sisteGjenståendePermittering =
        heltall(sisteGjenståendePermitteringId, "Siste antall fridagsperiodedager som gjenstår", enhet = Enhet.Dager, synlig = aldriSynlig)

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 7, "Dagpenger til permitterte", "Permittering"),
        ) {
            skalVurderes { it.erSann(oppfyllerKravetTilPermittering) }

            regel(permitteringsperiode) { oppslag(prøvingsdato) { 26 } }
            regel(antallPermitteringsdager) { multiplikasjon(permitteringsperiode, dagerIUka) }

            // regel(permitteringsdag) { erSann(arbeidsdag) }
            regel(permitteringsdag) { tomRegel }

            regel(forbruktPermittering) { tomRegel }
            regel(gjenståendePermittering) { tomRegel }
            regel(sistePermitteringsdag) { tomRegel }
            regel(sisteGjenståendePermittering) { tomRegel }

            kvote(
                KvoteDefinisjon(
                    hjemmel = hjemmel,
                    forbrukstype = Forbrukstype.Rettighet,
                    tildelingsgrunnlag = Tildelingsgrunnlag(antallPermitteringsdager),
                    tellesNår = permitteringsdag,
                    forbruksteller = forbruktPermittering,
                    gjenstående = gjenståendePermittering,
                    sisteForbruk = sistePermitteringsdag,
                    sisteGjenstående = sisteGjenståendePermittering,
                    utløsendeBetingelse = oppfyllerKravetTilPermittering,
                ),
            )

            ønsketResultat(antallPermitteringsdager)

            påvirkerResultat { it.erSann(oppfyllerKravetTilPermittering) }
        }
}

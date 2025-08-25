package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.beløp
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.OpplysningsTyper.AntallDagsatsForEgenandelId
import no.nav.dagpenger.regel.OpplysningsTyper.EgenandelId
import no.nav.dagpenger.regel.OpplysningsTyper.IngenEgenandelId
import no.nav.dagpenger.regel.OpplysningsTyper.TreGangerDagsatsId
import no.nav.dagpenger.regel.Rettighetstype.permitteringFiskeforedling
import no.nav.dagpenger.regel.folketrygden
import no.nav.dagpenger.regel.kravPåDagpenger

object Egenandel {
    val egenandel = beløp(EgenandelId, "Egenandel")
    private val treGangerDagsats = beløp(TreGangerDagsatsId, "Tre ganger dagsats", synlig = aldriSynlig)
    private val ingenEgenandel = beløp(IngenEgenandelId, "Ingen egenandel", synlig = aldriSynlig)
    private val sats = DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
    private val antallDagsatsIEgenandel = desimaltall(AntallDagsatsForEgenandelId, "Antall dagsats for egenandel", synlig = aldriSynlig)

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 9, "Egenandel", "Egenandel"),
        ) {
            skalVurderes { kravPåDagpenger(it) }

            regel(antallDagsatsIEgenandel) { oppslag { 3.0 } }
            regel(treGangerDagsats) { multiplikasjon(sats, antallDagsatsIEgenandel) }
            regel(ingenEgenandel) { somUtgangspunkt(Beløp(0.0)) }

            regel(egenandel) { hvisSannMedResultat(permitteringFiskeforedling, ingenEgenandel, treGangerDagsats) }

            påvirkerResultat { kravPåDagpenger(it) }

            ønsketResultat(egenandel)
        }
}

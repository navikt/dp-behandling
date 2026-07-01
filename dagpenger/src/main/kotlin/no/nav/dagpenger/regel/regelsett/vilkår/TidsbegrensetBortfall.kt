package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Forbrukstype
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Tildelingsgrunnlag
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.kravPåDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.dagerIUka

object TidsbegrensetBortfall {
    val harTidsbegrensetBortfall = Opplysningstype.boolsk(OpplysningsTyper.harBortfallId, "Er ilagt tidsbegrenset bortfall av dagpenger")

    val antallBortfallsuker =
        Opplysningstype.heltall(OpplysningsTyper.antallBortfallsukerId, "Antall uker med tidsbegrenset bortfall", enhet = Enhet.Uker)
    private val beregnetAntallBortfallsdager =
        Opplysningstype.heltall(
            OpplysningsTyper.beregnetAntallBortfallsdagerId,
            "Beregnet antall dager med tidsbegrenset bortfall",
            enhet = Enhet.Dager,
            synlig = aldriSynlig,
        )
    private val ingenBortfallsdager =
        Opplysningstype.heltall(
            OpplysningsTyper.ingenBortfallsdagerId,
            "Ingen dager med tidsbegrenset bortfall",
            enhet = Enhet.Dager,
            synlig = aldriSynlig,
        )
    val antallBortfallsdager =
        Opplysningstype.heltall(OpplysningsTyper.antallBortfallsdagerId, "Antall dager med tidsbegrenset bortfall", enhet = Enhet.Dager)

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 20, "Tidsbegrenset bortfall av dagpenger", "Tidsbegrenset bortfall")) {
            skalVurderes { kravPåDagpenger(it) }

            utfall(harTidsbegrensetBortfall) { somUtgangspunkt(false) }
            regel(antallBortfallsuker) { somUtgangspunkt(18) }
            regel(beregnetAntallBortfallsdager) { multiplikasjon(antallBortfallsuker, dagerIUka) }
            regel(ingenBortfallsdager) { somUtgangspunkt(0) }
            regel(antallBortfallsdager) {
                hvisSannMedResultat(harTidsbegrensetBortfall, beregnetAntallBortfallsdager, ingenBortfallsdager)
            }

            kvote(
                KvoteDefinisjon(
                    hjemmel = hjemmel,
                    tildelingsgrunnlag = Tildelingsgrunnlag(antallBortfallsdager),
                    tellesNår = Beregning.erSanksjonsdag,
                    forbruksteller = Beregning.forbruktBortfallsdager,
                    gjenstående = Beregning.gjenståendeBortfallsdager,
                    sisteForbruk = Beregning.sisteBortfallsdagMedForbruk,
                    sisteGjenstående = Beregning.sisteGjenståendeBortfallsdager,
                    forbrukstype = Forbrukstype.Sanksjon,
                ),
            )

            ønsketResultat(antallBortfallsdager)

            påvirkerResultat { it.erSann(harTidsbegrensetBortfall) }
        }
}

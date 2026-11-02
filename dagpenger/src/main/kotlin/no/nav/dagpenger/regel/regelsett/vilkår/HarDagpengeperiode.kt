package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.harDagpengerIgjenId
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.antallStønadsdager

object HarDagpengeperiode {
    val harDagpengerIgjen = boolsk(harDagpengerIgjenId, "Har gjenstående periode av dagpenger", synlig = aldriSynlig)
    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 15, "Antall stønadsuker (stønadsperiode)", "Periode"),
        ) {
            skalVurderes { it.har(antallStønadsdager) }

            utfall(harDagpengerIgjen) { somUtgangspunkt(true) }

            påvirkerResultat { it.har(harDagpengerIgjen) && !it.erSann(harDagpengerIgjen) }
        }
}

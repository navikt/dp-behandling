package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.forskriftTilFolketrygden
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravetTilTreMeldeperioderUtenTilstrekkeligTapAvArbeidstidId
import no.nav.dagpenger.regel.regelsett.beregning.Beregning

object TreMeldePerioderUtentilstrekkeligTapAvArbeidstid {
    val trePåfølgendePerioderUtenTilstrekkeligTap =
        Opplysningstype.boolsk(
            OppfyllerKravetTilTreMeldeperioderUtenTilstrekkeligTapAvArbeidstidId,
            "Oppfyller kravene til tre meldeperioder uten tilstrekkelig tap av arbeidstid",
        )
    val regelsett =
        vilkår(
            forskriftTilFolketrygden.hjemmel(
                10,
                4,
                tittel = "Tre påfølgende meldeperioder uten tilstrekkelig tap av arbeidstid § 10-4 annet ledd",
                kortnavn = "Tre meldeperioder uten tilstrekkelig tap",
            ),
        ) {
            skalVurderes { it.har(Beregning.meldeperiode) || it.har(Gjenopptak.skalGjenopptas) }
            utfall(
                trePåfølgendePerioderUtenTilstrekkeligTap,
            ) { somUtgangspunkt(true, Søknadstidspunkt.søknadsdato) }

            avklaring(Avklaringspunkter.JobbetOverTerskel)
            påvirkerResultat { it.har(trePåfølgendePerioderUtenTilstrekkeligTap) }
        }

    val OverTerskelKontroll =
        Kontrollpunkt(Avklaringspunkter.JobbetOverTerskel) {
            it.har(
                trePåfølgendePerioderUtenTilstrekkeligTap,
            ) &&
                !it.oppfyller(trePåfølgendePerioderUtenTilstrekkeligTap)
        }
}

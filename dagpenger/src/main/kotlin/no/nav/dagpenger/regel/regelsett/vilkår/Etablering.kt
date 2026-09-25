package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.forskriftTilFolketrygden
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.egenVirksomhetId
import no.nav.dagpenger.regel.OpplysningsTyper.etableringGodkjentId
import no.nav.dagpenger.regel.OpplysningsTyper.godkjentNæringsfagligId
import no.nav.dagpenger.regel.OpplysningsTyper.ikkeSelvforskyldtArbeidsledigId
import no.nav.dagpenger.regel.OpplysningsTyper.nyVirksomhetId
import no.nav.dagpenger.regel.OpplysningsTyper.påvirkerUtfalletId
import no.nav.dagpenger.regel.OpplysningsTyper.selvforsørgetId
import no.nav.dagpenger.regel.OpplysningsTyper.sluttDatoId
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalEtableringVurderes
import java.time.LocalDate

object Etablering {
    val nyVirksomhet = boolsk(nyVirksomhetId, "Ny virksomhet")
    val selvforsørget = boolsk(selvforsørgetId, "Antas å føre til selvforsørgelse")
    val godkjentNæringsfaglig = boolsk(godkjentNæringsfagligId, "Godkjent næringsfaglig vurdering")
    val ikkeSelvforskyldtArbeidsledig = boolsk(ikkeSelvforskyldtArbeidsledigId, "Ikke selvforskyldt arbeidsledig")
    val egenVirksomhet = boolsk(egenVirksomhetId, "Egen Virksomhet")
    val påvirkerUtfallet = boolsk(påvirkerUtfalletId, "Skal påvirke løpende rett")
    val sluttDato = dato(sluttDatoId, "Siste dato for etablering")
    val etableringGodkjent = boolsk(etableringGodkjentId, "Oppfyller vilkårene til etablering av egen virksomhet")

    val regelsett =
        vilkår(
            forskriftTilFolketrygden.hjemmel(
                kapittel = 4,
                paragraf = 6,
                tittel = "Etablering",
                kortnavn = "Etablering",
            ),
        ) {
            skalVurderes { opplysninger -> opplysninger.erSann(skalEtableringVurderes) }

            regel(nyVirksomhet) { somUtgangspunkt(false) }
            regel(selvforsørget) { somUtgangspunkt(false) }
            regel(godkjentNæringsfaglig) { somUtgangspunkt(false) }
            regel(ikkeSelvforskyldtArbeidsledig) { somUtgangspunkt(false) }
            regel(egenVirksomhet) { somUtgangspunkt(false) }
            regel(påvirkerUtfallet) { somUtgangspunkt(true) }
            regel(sluttDato) { somUtgangspunkt(LocalDate.now().plusMonths(12)) }

            utfall(etableringGodkjent) {
                alle(
                    nyVirksomhet,
                    selvforsørget,
                    godkjentNæringsfaglig,
                    egenVirksomhet,
                    ikkeSelvforskyldtArbeidsledig,
                )
            }
            ønsketResultat(påvirkerUtfallet)

            påvirkerResultat { it.erSann(skalEtableringVurderes) && it.erSann(påvirkerUtfallet) }
        }
}

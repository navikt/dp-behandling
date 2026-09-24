package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.forskriftTilFolketrygden
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
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
    val selvforsørget = boolsk(selvforsørgetId, "Antas selvforørget")
    val godkjentNæringsfaglig = boolsk(godkjentNæringsfagligId, "Godkjent Næringsfaglig")
    val ikkeSelvforskyldtArbeidsledig = boolsk(ikkeSelvforskyldtArbeidsledigId, "Ikke selvforskyldt arbeidsledig")
    val påvirkerUtfallet = boolsk(påvirkerUtfalletId, "Skal påvirker utfallet")
    val sluttDato = dato(sluttDatoId, "Ikke selvforskyldt arbeidsledig")
    val etableringGodkjent = boolsk(etableringGodkjentId, "Etablering godkjent")

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
            regel(påvirkerUtfallet) { somUtgangspunkt(true) }
            regel(sluttDato) { somUtgangspunkt(LocalDate.now().plusMonths(12)) }

            utfall(etableringGodkjent) { alle(nyVirksomhet, selvforsørget, godkjentNæringsfaglig, ikkeSelvforskyldtArbeidsledig) }
            ønsketResultat(påvirkerUtfallet, sluttDato)

            påvirkerResultat { it.erSann(påvirkerUtfallet) }
        }
}

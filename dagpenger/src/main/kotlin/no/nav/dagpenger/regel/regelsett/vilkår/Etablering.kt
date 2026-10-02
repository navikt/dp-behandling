package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.RettighetsperiodeStrategi.SettTilOgMedVedOppfylt
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.forskriftTilFolketrygden
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi
import no.nav.dagpenger.opplysning.regel.alleMedGyldighetsperiodeFra
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.egenVirksomhetId
import no.nav.dagpenger.regel.OpplysningsTyper.etableringGodkjentId
import no.nav.dagpenger.regel.OpplysningsTyper.godkjentNæringsfagligId
import no.nav.dagpenger.regel.OpplysningsTyper.ikkeSelvforskyldtArbeidsledigId
import no.nav.dagpenger.regel.OpplysningsTyper.nyVirksomhetId
import no.nav.dagpenger.regel.OpplysningsTyper.overFemtiProsentEierandelId
import no.nav.dagpenger.regel.OpplysningsTyper.påvirkerUtfalletId
import no.nav.dagpenger.regel.OpplysningsTyper.selvforsørgetId
import no.nav.dagpenger.regel.OpplysningsTyper.sluttDatoId
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalEtableringVurderes
import java.time.LocalDate

object Etablering {
    val nyVirksomhet = boolsk(nyVirksomhetId, "Ny virksomhet")
    val sluttDato = dato(sluttDatoId, "Siste dato for dagpenger under etablering")
    val selvforsørget = boolsk(selvforsørgetId, "Antas å føre til selvforsørgelse")
    val godkjentNæringsfaglig = boolsk(godkjentNæringsfagligId, "Godkjent næringsfaglig vurdering")
    val ikkeSelvforskyldtArbeidsledig = boolsk(ikkeSelvforskyldtArbeidsledigId, "Ikke selvforskyldt arbeidsledig")
    val egenVirksomhet = boolsk(egenVirksomhetId, "Egen Virksomhet")
    val påvirkerUtfallet = boolsk(påvirkerUtfalletId, "Skal påvirke løpende rett")

    // En person som mottar dagpenger og etablerer virksomhet sammen med andre som ikke mottar dagpenger eller arbeidsavklaringspenger fra Nav, må ha en eierandel i virksomheten på over 50 prosent.
    val overFemtiProsentEierandel = boolsk(overFemtiProsentEierandelId, "Over 50 prosent eierandel i virksomheten")
    val etableringGodkjent =
        boolsk(
            etableringGodkjentId,
            "Oppfyller vilkårene til etablering av egen virksomhet",
            gyldighetsperiode = GyldighetsperiodeStrategi.basertPåTilOgMed(sluttDato),
        )

    val regelsett =
        vilkår(
            forskriftTilFolketrygden.hjemmel(
                kapittel = 4,
                paragraf = 6,
                tittel = "Etablering",
                kortnavn = "Etablering",
            ),
        ) {
            påvirkningPåRettighetsperiode(SettTilOgMedVedOppfylt)
            skalVurderes { opplysninger -> opplysninger.erSann(skalEtableringVurderes) }

            regel(nyVirksomhet) { somUtgangspunkt(false) }
            regel(selvforsørget) { somUtgangspunkt(false) }
            regel(godkjentNæringsfaglig) { somUtgangspunkt(false) }
            regel(ikkeSelvforskyldtArbeidsledig) { somUtgangspunkt(false) }
            regel(egenVirksomhet) { somUtgangspunkt(false) }
            regel(overFemtiProsentEierandel) { somUtgangspunkt(false) }
            regel(påvirkerUtfallet) { somUtgangspunkt(true) }
            regel(sluttDato) { somUtgangspunkt(LocalDate.now().plusMonths(12)) }

            utfall(etableringGodkjent) {
                alleMedGyldighetsperiodeFra(
                    nyVirksomhet,
                    selvforsørget,
                    godkjentNæringsfaglig,
                    egenVirksomhet,
                    ikkeSelvforskyldtArbeidsledig,
                    overFemtiProsentEierandel,
                    periodeFra = sluttDato,
                )
            }
            ønsketResultat(påvirkerUtfallet, sluttDato)

            påvirkerResultat { it.erSann(skalEtableringVurderes) && it.erSann(påvirkerUtfallet) }
        }
}

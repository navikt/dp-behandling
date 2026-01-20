package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.ingenAv
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.harAvgittUriktigeOpplysningerId
import no.nav.dagpenger.regel.OpplysningsTyper.holderTilbakeInfoId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerVilkårManglendeEllerUriktigeOpplysningerId
import no.nav.dagpenger.regel.OpplysningsTyper.`unnlaterÅEtterkommePåleggId`

object Uriktigeopplysninger {
    val uriktigeOpplysninger = boolsk(harAvgittUriktigeOpplysningerId, "Mot bedre vitende har gitt uriktige opplysninger")
    val holderTilbake =
        boolsk(holderTilbakeInfoId, "Holder tilbake opplysninger som er viktige for rettigheter eller plikter etter denne loven")
    val unnlateråEtterkommePålegg =
        boolsk(`unnlaterÅEtterkommePåleggId`, "Uten rimelig grunn unnlater å etterkomme pålegg som er gitt med hjemmel i denne loven")

    val oppfyllerVilkårManglendeEllerUriktigeOpplysninger =
        boolsk(oppfyllerVilkårManglendeEllerUriktigeOpplysningerId, "Har gitt fullstendige og riktige opplysninger")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(
                21,
                7,
                "Uriktige opplysninger m.m. - følger for ytelser fra trygden",
                "Manglende og uriktige opplysninger",
            ),
        ) {
            regel(uriktigeOpplysninger) { somUtgangspunkt(false) }
            regel(holderTilbake) { somUtgangspunkt(false) }
            regel(`unnlateråEtterkommePålegg`) { somUtgangspunkt(false) }

            utfall(oppfyllerVilkårManglendeEllerUriktigeOpplysninger) {
                ingenAv(
                    uriktigeOpplysninger,
                    holderTilbake,
                    unnlateråEtterkommePålegg,
                )
            }
        }
}

package no.nav.dagpenger.regel.prosessvilkår

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.ingenAv
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.folketrygden

object Uriktigeopplysninger {
    val uriktigeOpplysninger =
        boolsk(
            OpplysningsTyper.harAvgittUriktigeOpplysningerId,
            "Mot bedre vitende har gitt uriktige opplysninger",
        )
    val holderTilbake =
        boolsk(
            OpplysningsTyper.holderTilbakeInfoId,
            "Holder tilbake opplysninger som er viktige for rettigheter eller plikter etter denne loven",
        )
    val unnlateråEtterkommePålegg =
        boolsk(
            OpplysningsTyper.`unnlaterÅEtterkommePåleggId`,
            "Uten rimelig grunn unnlater å etterkomme pålegg som er gitt med hjemmel i denne loven",
        )

    val oppfyllerVilkårManglendeEllerUriktigeOpplysninger =
        boolsk(
            OpplysningsTyper.`oppfyllerVilkårManglendeEllerUriktigeOpplysningerId`,
            "Har gitt fullstendige og riktige opplysninger",
        )

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

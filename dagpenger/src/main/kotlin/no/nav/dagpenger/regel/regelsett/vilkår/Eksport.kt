package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi.Companion.basertPå
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilDager
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.fraOgMed
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper.blurpId
import no.nav.dagpenger.regel.OpplysningsTyper.fristForRegistreringId
import no.nav.dagpenger.regel.OpplysningsTyper.fristdatoForRegistreringId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerVilkårForEksportId
import no.nav.dagpenger.regel.OpplysningsTyper.registrertIVertslandFraId
import no.nav.dagpenger.regel.OpplysningsTyper.registrertIVertslandId
import no.nav.dagpenger.regel.OpplysningsTyper.registrertInnenFristId
import no.nav.dagpenger.regel.OpplysningsTyper.skalHaEksportFraId
import no.nav.dagpenger.regel.OpplysningsTyper.skalHaEksportId
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport.fristForRegistrering
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport.fristdatoForRegistrering
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport.registrertInnenFrist
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport.skalHaEksport
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport.skalHaEksportFra

object Eksport {
    val skalHaEksport =
        Opplysningstype.boolsk(
            skalHaEksportId,
            "Bruker er skal ha eksport til EØS",
        )
    val skalHaEksportFra =
        Opplysningstype.dato(
            skalHaEksportFraId,
            "Dato eksport skal løpe fra",
        )
    val fristForRegistrering =
        Opplysningstype.heltall(
            fristForRegistreringId,
            "Antall dagers frist for å registrere seg i vertsland",
            enhet = Enhet.Dager,
        )
    val fristdatoForRegistrering =
        Opplysningstype.dato(
            fristdatoForRegistreringId,
            "Frist for å registrere seg i vertsland",
        )
    val registrertInnenFrist =
        Opplysningstype.boolsk(
            registrertInnenFristId,
            "Bruker er registrert innen fristen i vertsland",
            gyldighetsperiode = basertPå(),
        )
    val blurp =
        Opplysningstype.boolsk(
            blurpId,
            "blurp",
            gyldighetsperiode =
                GyldighetsperiodeStrategi { _, utledetAv ->
                    when (utledetAv.first().verdi as Boolean) {
                        true -> utledetAv[1].gyldighetsperiode
                        false -> utledetAv[2].gyldighetsperiode
                    }
                },
        )
    val registrertIVertsland =
        Opplysningstype.boolsk(
            registrertIVertslandId,
            "Bruker er registrert i vertsland",
        )
    val registrertIVertslandFra =
        Opplysningstype.dato(
            registrertIVertslandFraId,
            "Bruker er registrert i vertsland fra og med",
        )
    val oppfyllerVilkårForEksport =
        Opplysningstype.boolsk(
            oppfyllerVilkårForEksportId,
            "Bruker oppfyller vilkåret for eksport",
        )

    val regelsett =
        vilkår(
            folketrygden.hjemmel(0, 0, "Eksport", "Eksport"),
        ) {
            skalVurderes { it.erSann(Rettighetstype.skalEksportVurderes) }

            regel(skalHaEksport) { erSann(Rettighetstype.skalEksportVurderes) }
            regel(skalHaEksportFra) { fraOgMed(Rettighetstype.skalEksportVurderes) }

            regel(fristForRegistrering) { somUtgangspunkt(7) }
            regel(fristdatoForRegistrering) { leggTilDager(skalHaEksportFra, fristForRegistrering) }

            regel(registrertIVertsland) { somUtgangspunkt(false) }
            regel(registrertIVertslandFra) { fraOgMed(registrertIVertsland) }

            regel(registrertInnenFrist) { førEllerLik(registrertIVertslandFra, fristdatoForRegistrering) }

            // regel(blurp) { hvisSannMedResultat(registrertInnenFrist, skalHaEksport, registrertIVertsland) }

            utfall(oppfyllerVilkårForEksport) { alle(skalHaEksport, registrertIVertsland) }

            påvirkerResultat { it.erSann(Rettighetstype.skalEksportVurderes) }
        }
}

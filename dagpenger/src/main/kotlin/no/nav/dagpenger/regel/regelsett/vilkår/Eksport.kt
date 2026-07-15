package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi.Companion.arvFraMedGrense
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi.Companion.arvFraValgtGren
import no.nav.dagpenger.opplysning.regel.alleMedGyldighetsperiodeFra
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilDager
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.fraOgMed
import no.nav.dagpenger.opplysning.regel.hvisSannMedResultat
import no.nav.dagpenger.opplysning.regel.ikke
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper.eksportGjenopptakId
import no.nav.dagpenger.regel.OpplysningsTyper.fristForRegistreringId
import no.nav.dagpenger.regel.OpplysningsTyper.fristdatoForRegistreringId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerVilkårForEksportId
import no.nav.dagpenger.regel.OpplysningsTyper.registrertEtterFristId
import no.nav.dagpenger.regel.OpplysningsTyper.registrertIVertslandFraId
import no.nav.dagpenger.regel.OpplysningsTyper.registrertIVertslandId
import no.nav.dagpenger.regel.OpplysningsTyper.registrertInnenFristId
import no.nav.dagpenger.regel.OpplysningsTyper.skalHaEksportFraId
import no.nav.dagpenger.regel.OpplysningsTyper.skalHaEksportId

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
    val registrertEtterFrist =
        Opplysningstype.boolsk(
            registrertEtterFristId,
            "Bruker er registrert etter fristen i vertsland",
        )
    val registrertInnenFrist =
        Opplysningstype.boolsk(
            registrertInnenFristId,
            "Bruker er registrert innen fristen i vertsland",
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
    val eksportGjenopptakFraOgMed =
        Opplysningstype.dato(
            eksportGjenopptakId,
            "eksportGjenopptakId",
            gyldighetsperiode = arvFraValgtGren(registrertInnenFrist, skalHaEksportFra, registrertIVertslandFra),
        )
    val oppyllerVilkårForEksport =
        Opplysningstype.boolsk(
            oppfyllerVilkårForEksportId,
            "Oppfyller kravet til eksport",
            gyldighetsperiode = arvFraMedGrense(eksportGjenopptakFraOgMed),
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

            regel(registrertIVertsland) { somUtgangspunkt(false, skalHaEksport) }
            regel(registrertIVertslandFra) { fraOgMed(registrertIVertsland) }

            regel(registrertInnenFrist) { førEllerLik(registrertIVertslandFra, fristdatoForRegistrering) }
            regel(registrertEtterFrist) { ikke(registrertInnenFrist) }

            regel(eksportGjenopptakFraOgMed) { hvisSannMedResultat(registrertInnenFrist, skalHaEksportFra, registrertIVertslandFra) }

            utfall(oppyllerVilkårForEksport) {
                alleMedGyldighetsperiodeFra(skalHaEksport, registrertIVertsland, periodeFra = eksportGjenopptakFraOgMed)
            }

            påvirkerResultat { it.erSann(Rettighetstype.skalEksportVurderes) }
        }
}

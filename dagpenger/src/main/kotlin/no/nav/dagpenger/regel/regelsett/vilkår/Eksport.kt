package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.dsl.vilkår
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
import no.nav.dagpenger.opplysning.trygdeforordningen
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
            "Bruker skal ha eksport av dagpenger til EØS-land",
        )
    val skalHaEksportFra =
        Opplysningstype.dato(
            skalHaEksportFraId,
            "Dato eksport skal gjelde fra",
            synlig = aldriSynlig,
        )
    val antallDagerFristForRegistrering =
        Opplysningstype.heltall(
            fristForRegistreringId,
            "Antall dagers frist for å registrere seg i vertsland",
            enhet = Enhet.Dager,
        )
    val fristdatoForRegistrering =
        Opplysningstype.dato(
            fristdatoForRegistreringId,
            "Fristdato for å registrere seg i vertsland",
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
            "Dato bruker registrerte seg i vertsland",
            synlig = aldriSynlig,
        )
    val eksportGjenopptakFraOgMed =
        Opplysningstype.dato(
            eksportGjenopptakId,
            "Dato for gjenopptak av dagpenger",
            gyldighetsperiode = arvFraValgtGren(registrertInnenFrist, skalHaEksportFra, registrertIVertslandFra),
            synlig = aldriSynlig,
        )
    val oppfyllerVilkårForEksport =
        Opplysningstype.boolsk(
            oppfyllerVilkårForEksportId,
            "Oppfyller vilkåret om eksport av dagpenger til EØS-land",
            gyldighetsperiode = arvFraMedGrense(eksportGjenopptakFraOgMed),
        )

    val regelsett =
        vilkår(
            trygdeforordningen.hjemmel(64, 0, "Eksport av dagpenger til EØS-land", "Eksport til EØS-land"),
        ) {
            skalVurderes { it.erSann(Rettighetstype.skalEksportVurderes) }

            regel(skalHaEksport) { erSann(Rettighetstype.skalEksportVurderes) }
            regel(skalHaEksportFra) { fraOgMed(Rettighetstype.skalEksportVurderes) }

            regel(antallDagerFristForRegistrering) { somUtgangspunkt(7) }
            regel(fristdatoForRegistrering) { leggTilDager(skalHaEksportFra, antallDagerFristForRegistrering) }

            regel(registrertIVertsland) { somUtgangspunkt(false, skalHaEksport) }
            regel(registrertIVertslandFra) { fraOgMed(registrertIVertsland) }

            regel(registrertInnenFrist) { førEllerLik(registrertIVertslandFra, fristdatoForRegistrering) }
            regel(registrertEtterFrist) { ikke(registrertInnenFrist) }

            regel(eksportGjenopptakFraOgMed) { hvisSannMedResultat(registrertInnenFrist, skalHaEksportFra, registrertIVertslandFra) }

            utfall(oppfyllerVilkårForEksport) {
                alleMedGyldighetsperiodeFra(skalHaEksport, registrertIVertsland, periodeFra = eksportGjenopptakFraOgMed)
            }

            påvirkerResultat { it.erSann(Rettighetstype.skalEksportVurderes) }
        }
}

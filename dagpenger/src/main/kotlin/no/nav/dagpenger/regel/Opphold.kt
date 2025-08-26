package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål.Bruker
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Avklaringspunkter.Bostedsland
import no.nav.dagpenger.regel.Behov.BostedslandErNorge
import no.nav.dagpenger.regel.Behov.OppholdINorge
import no.nav.dagpenger.regel.OpplysningsTyper.BostedslandId
import no.nav.dagpenger.regel.OpplysningsTyper.MedlemFolketrygdenId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravetOppholdId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravetTilOppholdId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerMedlemskapId
import no.nav.dagpenger.regel.OpplysningsTyper.OppholdINorgeId
import no.nav.dagpenger.regel.OpplysningsTyper.UnntakForOppholdId
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype

object Opphold {
    val bostedsland = boolsk(BostedslandId, "Bostedsland er Norge", behovId = BostedslandErNorge, formål = Bruker)
    var oppholdINorge =
        boolsk(OppholdINorgeId, beskrivelse = "Bruker oppholder seg i Norge", behovId = OppholdINorge, formål = Bruker)
    var unntakForOpphold =
        boolsk(UnntakForOppholdId, "Oppfyller unntak for opphold i Norge", synlig = { it.erSann(oppholdINorge) == false })

    val oppfyllerKravetTilOpphold =
        boolsk(OppfyllerKravetTilOppholdId, "Oppfyller kravet til opphold i Norge eller unntak", synlig = aldriSynlig)
    val medlemFolketrygden = boolsk(MedlemFolketrygdenId, "Bruker er medlem av folketrygden")

    val oppfyllerMedlemskap = boolsk(OppfyllerMedlemskapId, "Oppfyller kravet til medlemskap", synlig = aldriSynlig)
    val oppfyllerKravet = boolsk(OppfyllerKravetOppholdId, "Kravet til opphold i Norge er oppfylt")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 2, "Opphold i Norge", "Opphold"),
        ) {
            skalVurderes { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }

            regel(bostedsland) { innhentMed(søknadIdOpplysningstype) }
            regel(oppholdINorge) { erSann(bostedsland) }
            regel(unntakForOpphold) { somUtgangspunkt(false) }

            utfall(oppfyllerKravetTilOpphold) { enAv(oppholdINorge, unntakForOpphold) }

            regel(medlemFolketrygden) { somUtgangspunkt(true) }
            utfall(oppfyllerMedlemskap) { erSann(medlemFolketrygden) }

            utfall(oppfyllerKravet) { alle(oppfyllerKravetTilOpphold, oppfyllerMedlemskap) }

            avklaring(Bostedsland)

            påvirkerResultat { oppfyllerKravetTilMinsteinntektEllerVerneplikt(it) }
        }

    val BostedslandKontroll =
        Kontrollpunkt(Bostedsland) {
            it.har(bostedsland) && !it.finnOpplysning(bostedsland).verdi
        }
}

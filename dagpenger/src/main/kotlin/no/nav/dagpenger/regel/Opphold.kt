package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.MedlemFolketrygdenId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravetOppholdId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravetTilOppholdId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerMedlemskapId
import no.nav.dagpenger.regel.OpplysningsTyper.OppholdINorgeId
import no.nav.dagpenger.regel.OpplysningsTyper.UnntakForOppholdId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object Opphold {
    var oppholdINorge = boolsk(OppholdINorgeId, beskrivelse = "Opphold i Norge", behovId = "OppholdINorge")
    var unntakForOpphold =
        boolsk(UnntakForOppholdId, "Oppfyller unntak for opphold i Norge", synlig = { it.erSann(oppholdINorge) == false })
    val oppfyllerKravetTilOpphold =
        boolsk(OppfyllerKravetTilOppholdId, "Oppfyller kravet til opphold i Norge eller unntak", synlig = aldriSynlig)

    val medlemFolketrygden = boolsk(MedlemFolketrygdenId, "Er personen medlem av folketrygden")
    val oppfyllerMedlemskap = boolsk(OppfyllerMedlemskapId, "Oppfyller kravet til medlemskap", synlig = aldriSynlig)

    val oppfyllerKravet = boolsk(OppfyllerKravetOppholdId, "Oppfyller kravet til opphold i Norge")

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 2, "Opphold i Norge", "Opphold"),
        ) {
            skalKjøres { kravetTilMinsteinntektEllerVerneplikt(it) }

            regel(oppholdINorge) { oppslag(prøvingsdato) { true } }
            regel(unntakForOpphold) { oppslag(prøvingsdato) { false } }
            regel(oppfyllerKravetTilOpphold) { enAv(oppholdINorge, unntakForOpphold) }

            regel(medlemFolketrygden) { oppslag(prøvingsdato) { true } }
            regel(oppfyllerMedlemskap) { erSann(medlemFolketrygden) }

            utfall(oppfyllerKravet) { alle(oppfyllerKravetTilOpphold, oppfyllerMedlemskap) }

            relevantHvis { kravetTilMinsteinntektEllerVerneplikt(it) }
        }
}

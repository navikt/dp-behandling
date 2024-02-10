package no.nav.dagpenger.opplysning.nrh

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik

object BliPåPlassVilkår {
    val bliPåPlassVilkår = Opplysningstype<Boolean>("Bli på plass med forstyrrelser ")
    val hundenFlytterSegIkke =
        Opplysningstype<Boolean>("For å få bestått skal hunden ikke forflytte seg mer enn én hundelengde før øvelsen er slutt.")
    val hundeførerUtAvSynet = Opplysningstype<Boolean>("Hundefører er ute av synet for hunden ")
    val minutterHundeførerSkalVæreUtAvSynet = Opplysningstype<Double>("Tid hundefører skal være ut av synet for hunden")
    val minutterHundeførerHarVærtUtAvSynet = Opplysningstype<Double>("Tid hundefører har vært ut av synet for hunden")
    val lengePåØvelse = Opplysningstype<Double>("Tid fra hunden dekkes ned/settes til øvelsen avsluttes")
    val tidenHundenLigger = Opplysningstype<Double>("Tiden hunden ligger")
    val hundenLiggerHeleØvelsen = Opplysningstype<Boolean>("Hunden ligger hele øvelsen")
    val oppgaveForHundeførerErGjennomført =
        Opplysningstype<Boolean>("Oppgavene for hundefører skal omfatte samtale med annen person, henting eller flytting av utstyr")

    val regelsett =
        Regelsett("Bli på plass med forstyrrelser") {
            regel(hundeførerUtAvSynet) {
                størreEnnEllerLik(
                    er = minutterHundeførerHarVærtUtAvSynet,
                    størreEnn = minutterHundeførerSkalVæreUtAvSynet,
                )
            }
            regel(hundenLiggerHeleØvelsen) {
                størreEnnEllerLik(
                    er = tidenHundenLigger,
                    størreEnn = lengePåØvelse,
                )
            }
            regel(bliPåPlassVilkår) {
                alle(
                    hundenFlytterSegIkke,
                    hundeførerUtAvSynet,
                    hundenLiggerHeleØvelsen,
                    oppgaveForHundeførerErGjennomført,
                )
            }
        }
}

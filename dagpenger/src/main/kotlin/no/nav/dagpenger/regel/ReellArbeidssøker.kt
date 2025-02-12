package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål.Bruker
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Avklaringspunkter.IkkeRegistrertSomArbeidsøker
import no.nav.dagpenger.regel.Avklaringspunkter.ReellArbeidssøkerUnntak
import no.nav.dagpenger.regel.Behov.HelseTilAlleTyperJobb
import no.nav.dagpenger.regel.Behov.KanJobbeDeltid
import no.nav.dagpenger.regel.Behov.KanJobbeHvorSomHelst
import no.nav.dagpenger.regel.Behov.RegistrertSomArbeidssøker
import no.nav.dagpenger.regel.Behov.VilligTilÅBytteYrke
import no.nav.dagpenger.regel.Behov.ØnsketArbeidstid
import no.nav.dagpenger.regel.OpplysningsTyper.ErArbeidsførId
import no.nav.dagpenger.regel.OpplysningsTyper.GodkjentArbeidsuførId
import no.nav.dagpenger.regel.OpplysningsTyper.GodkjentDeltidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.GodkjentLokalArbeidssøker
import no.nav.dagpenger.regel.OpplysningsTyper.KanJobbeDeltidId
import no.nav.dagpenger.regel.OpplysningsTyper.KanJobbeHvorSomHelstId
import no.nav.dagpenger.regel.OpplysningsTyper.KravTilArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravTilArbeidsførId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravTilArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravTilMobilitetId
import no.nav.dagpenger.regel.OpplysningsTyper.OppfyllerKravetTilEthvertArbeidId
import no.nav.dagpenger.regel.OpplysningsTyper.OppyllerKravTilRegistrertArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.RegistrertSomArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.VilligTilEthvertArbeidId
import no.nav.dagpenger.regel.OpplysningsTyper.minimumVanligArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.villigTilMinimumArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.ønsketArbeidstidId
import no.nav.dagpenger.regel.Samordning.uføre
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype

object ReellArbeidssøker {
    // c.	å ta arbeid uavhengig av om det er på heltid eller deltid,
    val kanJobbeDeltid = boolsk(KanJobbeDeltidId, beskrivelse = "Kan jobbe heltid og deltid", Bruker, behovId = KanJobbeDeltid)
    val godkjentDeltidssøker =
        boolsk(GodkjentDeltidssøkerId, "Det er godkjent at bruker kun søker deltidsarbeid", synlig = {
            it.erSann(kanJobbeDeltid) == false
        })
    val oppfyllerKravTilArbeidssøker =
        boolsk(OppfyllerKravTilArbeidssøkerId, "Oppfyller kravet til heltid- og deltidsarbeid", synlig = aldriSynlig)

    // b.	å ta arbeid hvor som helst i Norge,
    val kanJobbeHvorSomHelst =
        boolsk(KanJobbeHvorSomHelstId, beskrivelse = "Kan jobbe i hele Norge", Bruker, behovId = KanJobbeHvorSomHelst)
    val godkjentLokalArbeidssøker =
        boolsk(GodkjentLokalArbeidssøker, "Det er godkjent at bruker kun søk arbeid lokalt", synlig = {
            it.erSann(kanJobbeHvorSomHelst) == false
        })
    val oppfyllerKravTilMobilitet = boolsk(OppfyllerKravTilMobilitetId, "Oppfyller kravet til mobilitet", synlig = aldriSynlig)

    //  Som reell arbeidssøker regnes den som er arbeidsfør,
    val erArbeidsfør = boolsk(ErArbeidsførId, beskrivelse = "Kan ta alle typer arbeid", Bruker, behovId = HelseTilAlleTyperJobb)
    val godkjentArbeidsufør =
        boolsk(GodkjentArbeidsuførId, "Har helsemessige begrensninger og kan ikke ta alle typer arbeid", synlig = {
            it.erSann(erArbeidsfør) == false
        })
    val oppfyllerKravTilArbeidsfør = boolsk(OppfyllerKravTilArbeidsførId, "Oppfyller kravet til å være arbeidsfør", synlig = aldriSynlig)

    // a.	å ta ethvert arbeid som er lønnet etter tariff eller sedvane,
    val villigTilEthvertArbeid =
        boolsk(VilligTilEthvertArbeidId, beskrivelse = "Villig til å bytte yrke", Bruker, behovId = VilligTilÅBytteYrke)
    val oppfyllerKravetTilEthvertArbeid =
        boolsk(OppfyllerKravetTilEthvertArbeidId, "Oppfyller kravet til å ta ethvert arbeid", synlig = aldriSynlig)

    // Registrert som arbeidssøker
    internal val registrertArbeidssøker =
        boolsk(RegistrertSomArbeidssøkerId, beskrivelse = "Registrert som arbeidssøker", behovId = RegistrertSomArbeidssøker)
    val oppyllerKravTilRegistrertArbeidssøker =
        boolsk(OppyllerKravTilRegistrertArbeidssøkerId, "Registrert som arbeidssøker på søknadstidspunktet", synlig = aldriSynlig)

    val kravTilArbeidssøker = boolsk(KravTilArbeidssøkerId, "Krav til arbeidssøker")

    val ønsketArbeidstid =
        desimaltall(
            ønsketArbeidstidId,
            "Ønsket arbeidstid",
            Bruker,
            behovId = ØnsketArbeidstid,
            synlig = { it.erSann(kanJobbeDeltid) == false },
        )
    val minimumVanligArbeidstid = desimaltall(minimumVanligArbeidstidId, "Minimum vanlig arbeidstid", synlig = { it.erSann(uføre) })
    private val villigTilMinimumArbeidstid =
        boolsk(villigTilMinimumArbeidstidId, "Villig til å jobbe minimum arbeidstid", synlig = { it.erSann(kanJobbeDeltid) == false })

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 5, "Reelle arbeidssøkere", "Reell arbeidssøker")) {
            skalVurderes { it.oppfyller(kravTilAlder) }

            regel(ønsketArbeidstid) { innhentMed(søknadIdOpplysningstype) }
            regel(minimumVanligArbeidstid) { oppslag(prøvingsdato) { 18.75 } }
            regel(villigTilMinimumArbeidstid) { størreEnnEllerLik(ønsketArbeidstid, minimumVanligArbeidstid) }

            regel(kanJobbeDeltid) { innhentes }
            regel(godkjentDeltidssøker) { oppslag(prøvingsdato) { false } }

            regel(kanJobbeHvorSomHelst) { innhentes }
            regel(godkjentLokalArbeidssøker) { oppslag(prøvingsdato) { false } }

            regel(erArbeidsfør) { innhentes }
            regel(godkjentArbeidsufør) { oppslag(prøvingsdato) { false } }

            regel(villigTilEthvertArbeid) { innhentes }

            utfall(oppfyllerKravTilArbeidssøker) { enAv(kanJobbeDeltid, godkjentDeltidssøker) }
            utfall(oppfyllerKravTilMobilitet) { enAv(kanJobbeHvorSomHelst, godkjentLokalArbeidssøker) }
            utfall(oppfyllerKravTilArbeidsfør) { enAv(erArbeidsfør, godkjentArbeidsufør) }
            utfall(oppfyllerKravetTilEthvertArbeid) { enAv(villigTilEthvertArbeid) }

            regel(registrertArbeidssøker) { innhentMed(prøvingsdato) }
            utfall(oppyllerKravTilRegistrertArbeidssøker) { erSann(registrertArbeidssøker) }

            utfall(kravTilArbeidssøker) {
                alle(
                    villigTilMinimumArbeidstid,
                    oppfyllerKravTilArbeidssøker,
                    oppfyllerKravTilMobilitet,
                    oppfyllerKravTilArbeidsfør,
                    oppfyllerKravetTilEthvertArbeid,
                    oppyllerKravTilRegistrertArbeidssøker,
                )
            }

            avklaring(ReellArbeidssøkerUnntak)
            avklaring(IkkeRegistrertSomArbeidsøker)

            påvirkerResultat { it.oppfyller(kravTilAlder) }
        }

    val ReellArbeidssøkerKontroll =
        Kontrollpunkt(ReellArbeidssøkerUnntak) {
            (it.har(kanJobbeDeltid) && it.finnOpplysning(kanJobbeDeltid).verdi == false) ||
                (it.har(kanJobbeHvorSomHelst) && it.finnOpplysning(kanJobbeHvorSomHelst).verdi == false) ||
                (it.har(erArbeidsfør) && it.finnOpplysning(erArbeidsfør).verdi == false) ||
                (it.har(villigTilEthvertArbeid) && it.finnOpplysning(villigTilEthvertArbeid).verdi == false)
        }

    val IkkeRegistrertSomArbeidsøkerKontroll =
        Kontrollpunkt(IkkeRegistrertSomArbeidsøker) {
            it.har(oppyllerKravTilRegistrertArbeidssøker) && it.finnOpplysning(oppyllerKravTilRegistrertArbeidssøker).verdi == false
        }
}

package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Avklaringspunkter.ReellArbeidssøkerUnntak
import no.nav.dagpenger.regel.Behov.HelseTilAlleTyperJobb
import no.nav.dagpenger.regel.Behov.KanJobbeDeltid
import no.nav.dagpenger.regel.Behov.KanJobbeHvorSomHelst
import no.nav.dagpenger.regel.Behov.VilligTilÅBytteYrke
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object ReellArbeidssøker {
    // c.	å ta arbeid uavhengig av om det er på heltid eller deltid,
    internal val kanJobbeDeltid = Opplysningstype.somBoolsk("Kan jobbe heltid og deltid".id(KanJobbeDeltid))
    val godkjentDeltidssøker = Opplysningstype.somBoolsk("Det er godkjent at bruker kun søker deltidsarbeid")
    private val oppfyllerKravTilArbeidssøker = Opplysningstype.somBoolsk("Oppfyller kravet til heltid- og deltidsarbeid")

    // b.	å ta arbeid hvor som helst i Norge,
    internal val kanJobbeHvorSomHelst = Opplysningstype.somBoolsk("Kan jobbe i hele Norge".id(KanJobbeHvorSomHelst))
    val godkjentLokalArbeidssøker = Opplysningstype.somBoolsk("Det er godkjent at bruker kun søk arbeid lokalt")
    private val oppfyllerKravTilMobilitet = Opplysningstype.somBoolsk("Oppfyller kravet til mobilitet")

    //  Som reell arbeidssøker regnes den som er arbeidsfør,
    internal val erArbeidsfør = Opplysningstype.somBoolsk("Kan ta alle typer arbeid".id(HelseTilAlleTyperJobb))
    private val oppfyllerKravTilArbeidsfør = Opplysningstype.somBoolsk("Oppfyller kravet til å være arbeidsfør")

    // a.	å ta ethvert arbeid som er lønnet etter tariff eller sedvane,
    internal val villigTilEthvertArbeid = Opplysningstype.somBoolsk("Villig til å bytte yrke".id(VilligTilÅBytteYrke))
    private val oppfyllerKravetTilEthvertArbeid = Opplysningstype.somBoolsk("Oppfyller kravet til å ta ethvert arbeid")

    val kravTilArbeidssøker = Opplysningstype.somBoolsk("Krav til arbeidssøker")

    val regelsett =
        Regelsett("Reell arbeidssøker") {
            regel(kanJobbeDeltid) { innhentes }
            regel(kanJobbeHvorSomHelst) { innhentes }
            regel(erArbeidsfør) { innhentes }
            regel(villigTilEthvertArbeid) { innhentes }

            regel(godkjentDeltidssøker) { oppslag(prøvingsdato) { false } }
            regel(godkjentLokalArbeidssøker) { oppslag(prøvingsdato) { false } }

            regel(oppfyllerKravTilArbeidssøker) { enAv(kanJobbeDeltid, godkjentDeltidssøker) }
            regel(oppfyllerKravTilMobilitet) { enAv(kanJobbeHvorSomHelst, godkjentLokalArbeidssøker) }
            regel(oppfyllerKravTilArbeidsfør) { enAv(erArbeidsfør) }
            regel(oppfyllerKravetTilEthvertArbeid) { enAv(villigTilEthvertArbeid) }

            regel(kravTilArbeidssøker) {
                alle(
                    oppfyllerKravTilArbeidssøker,
                    oppfyllerKravTilMobilitet,
                    oppfyllerKravTilArbeidsfør,
                    oppfyllerKravetTilEthvertArbeid,
                )
            }
        }

    val ReellArbeidssøkerKontroll =
        Kontrollpunkt(ReellArbeidssøkerUnntak) {
            it.har(kravTilArbeidssøker) && !it.finnOpplysning(kravTilArbeidssøker).verdi
        }
}

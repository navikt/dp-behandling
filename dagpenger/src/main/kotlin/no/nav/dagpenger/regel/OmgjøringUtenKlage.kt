package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Avklaringspunkter.HarSvartPåOmgjøringUtenKlage
import no.nav.dagpenger.regel.OpplysningsTyper.ansesUgyldigVedtakId
import no.nav.dagpenger.regel.OpplysningsTyper.endringIkkeTilSkadeId
import no.nav.dagpenger.regel.OpplysningsTyper.ikkeUnderretningId
import no.nav.dagpenger.regel.OpplysningsTyper.kanOmgjøresUtenKlageId
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype

object OmgjøringUtenKlage {
    val endringIkkeTilSkade =
        boolsk(endringIkkeTilSkadeId, "Endringen ikke er til skade for noen som vedtaket retter seg mot eller direkte tilgodeser")
    val ikkeUnderretning =
        boolsk(
            ikkeUnderretningId,
            "Underretning om vedtaket ikke er kommet fram til vedkommende og vedtaket heller ikke er offentlig kunngjort",
        )
    val ansesUgyldigVedtak = boolsk(ansesUgyldigVedtakId, "Vedtaket må anses ugyldig")

    val kanOmgjøresUtenKlage = boolsk(kanOmgjøresUtenKlageId, "Et forvaltningsorgan kan omgjøre sitt eget vedtak uten at det er påklaget")

    val regelsett =
        fastsettelse(forvaltningsloven.hjemmel(6, 35, "Omgjøring av vedtak uten klage", "Omgjøring uten klage")) {
            skalVurderes { hendelseTypeOpplysningstype.toString() == "Omgjøring" }

            regel(endringIkkeTilSkade) { somUtgangspunkt(false) }
            regel(ikkeUnderretning) { somUtgangspunkt(false) }
            regel(ansesUgyldigVedtak) { somUtgangspunkt(false) }

            regel(kanOmgjøresUtenKlage) { enAv(endringIkkeTilSkade, ikkeUnderretning, ansesUgyldigVedtak) }

            påvirkerResultat { false }
            avklaring(HarSvartPåOmgjøringUtenKlage)
        }

    val OmgjøringUtenKlageKontroll =
        Kontrollpunkt(HarSvartPåOmgjøringUtenKlage) {
            if (it.har(kanOmgjøresUtenKlage) && it.finnOpplysning(kanOmgjøresUtenKlage).verdi) {
                return@Kontrollpunkt false
            }
            true
        }
}

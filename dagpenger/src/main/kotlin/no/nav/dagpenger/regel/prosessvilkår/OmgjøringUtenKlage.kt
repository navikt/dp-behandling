package no.nav.dagpenger.regel.prosessvilkår

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.forvaltningsloven
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype

object OmgjøringUtenKlage {
    val endringIkkeTilSkade =
        boolsk(
            OpplysningsTyper.endringIkkeTilSkadeId,
            "Endringen ikke er til skade for noen som vedtaket retter seg mot eller direkte tilgodeser",
        )
    val ikkeUnderretning =
        boolsk(
            OpplysningsTyper.ikkeUnderretningId,
            "Underretning om vedtaket ikke er kommet fram til vedkommende og vedtaket heller ikke er offentlig kunngjort",
        )
    val ansesUgyldigVedtak =
        boolsk(OpplysningsTyper.ansesUgyldigVedtakId, "Vedtaket må anses ugyldig")

    val kanOmgjøresUtenKlage =
        boolsk(
            OpplysningsTyper.kanOmgjøresUtenKlageId,
            "Et forvaltningsorgan kan omgjøre sitt eget vedtak uten at det er påklaget",
        )

    val regelsett =
        fastsettelse(forvaltningsloven.hjemmel(6, 35, "Omgjøring av vedtak uten klage", "Omgjøring uten klage")) {
            skalVurderes { opplysninger ->
                opplysninger.har(hendelseTypeOpplysningstype) &&
                    opplysninger.finnOpplysning(hendelseTypeOpplysningstype).verdi == "OmgjøringHendelse"
            }

            regel(endringIkkeTilSkade) { somUtgangspunkt(false) }
            regel(ikkeUnderretning) { somUtgangspunkt(false) }
            regel(ansesUgyldigVedtak) { somUtgangspunkt(false) }

            regel(kanOmgjøresUtenKlage) { enAv(endringIkkeTilSkade, ikkeUnderretning, ansesUgyldigVedtak) }

            ønsketResultat(kanOmgjøresUtenKlage)
            avklaring(Avklaringspunkter.HarSvartPåOmgjøringUtenKlage)
        }

    val OmgjøringUtenKlageKontroll =
        Kontrollpunkt(Avklaringspunkter.HarSvartPåOmgjøringUtenKlage) {

            if (!it.har(kanOmgjøresUtenKlage)) {
                return@Kontrollpunkt false
            }

            if (it.erSann(kanOmgjøresUtenKlage)) {
                return@Kontrollpunkt false
            }
            true
        }
}

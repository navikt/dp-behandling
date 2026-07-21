package no.nav.dagpenger.regel.regelsett.prosessvilkår

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.prosess
import no.nav.dagpenger.opplysning.forvaltningsloven
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.OpplysningsTyper.skalOmgjøringUtenKlageVurderesId
import no.nav.dagpenger.regel.hendelse.OmgjøringHendelse
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage.kanOmgjøresUtenKlage
import no.nav.dagpenger.regelverk.hendelseTypeOpplysningstype

object OmgjøringUtenKlageValg {
    val skalOmgjøringUtenKlageVurderes = boolsk(skalOmgjøringUtenKlageVurderesId, "Skal omgjøring uten klage vurderes?")

    val regelsett =
        prosess(
            forvaltningsloven.hjemmel(6, 35, "Omgjøring av vedtak uten klage", "Skal det gjøres omgjøring uten klage?"),
        ) {
            skalVurderes { opplysninger ->
                opplysninger.har(hendelseTypeOpplysningstype) &&
                    opplysninger.finnOpplysning(hendelseTypeOpplysningstype).verdi == OmgjøringHendelse::class.java.simpleName
            }
            regel(skalOmgjøringUtenKlageVurderes) { somUtgangspunkt(false) }
            ønsketResultat(skalOmgjøringUtenKlageVurderes)
            avklaring(Avklaringspunkter.SkalOmgjøringUtenKlageVurderes)
        }

    val SkalOmgjøringUtenKlageVurderesKontroll =
        Kontrollpunkt(Avklaringspunkter.SkalOmgjøringUtenKlageVurderes) {
            if (it.har(skalOmgjøringUtenKlageVurderes) && !it.oppfyller(skalOmgjøringUtenKlageVurderes)) {
                return@Kontrollpunkt true
            }
            if (it.har(kanOmgjøresUtenKlage) && it.erSann(kanOmgjøresUtenKlage)) {
                return@Kontrollpunkt false
            }
            it.har(skalOmgjøringUtenKlageVurderes) && it.oppfyller(skalOmgjøringUtenKlageVurderes)
        }
}

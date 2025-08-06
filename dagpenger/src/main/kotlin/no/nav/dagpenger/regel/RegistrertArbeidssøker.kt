package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål.Register
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Avklaringspunkter.IkkeRegistrertSomArbeidsøker
import no.nav.dagpenger.regel.Behov.RegistrertSomArbeidssøker
import no.nav.dagpenger.regel.OpplysningsTyper.OppyllerKravTilRegistrertArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.RegistrertSomArbeidssøkerId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object RegistrertArbeidssøker {
    // Registrert som arbeidssøker
    internal val registrertArbeidssøker =
        boolsk(
            RegistrertSomArbeidssøkerId,
            beskrivelse = "Registrert som arbeidssøker",
            behovId = RegistrertSomArbeidssøker,
            formål = Register,
        )
    val oppyllerKravTilRegistrertArbeidssøker =
        boolsk(OppyllerKravTilRegistrertArbeidssøkerId, "Oppfyller kravet til å være registrert som arbeidssøker")

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 5, "Reelle arbeidssøkere - registrert som arbeidssøker", "Registrert som arbeidssøker")) {
            skalVurderes { it.har(kravTilAlder) }

            regel(registrertArbeidssøker) { innhentMed(prøvingsdato) }
            utfall(oppyllerKravTilRegistrertArbeidssøker) { erSann(registrertArbeidssøker) }

            påvirkerResultat { it.har(kravTilAlder) }
        }

    val IkkeRegistrertSomArbeidsøkerKontroll =
        Kontrollpunkt(IkkeRegistrertSomArbeidsøker) {
            it.har(oppyllerKravTilRegistrertArbeidssøker) && it.finnOpplysning(oppyllerKravTilRegistrertArbeidssøker).verdi == false
        }
}

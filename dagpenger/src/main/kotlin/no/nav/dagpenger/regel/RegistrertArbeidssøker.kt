package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Avklaringspunkter.IkkeRegistrertSomArbeidsøker
import no.nav.dagpenger.regel.Behov.RegistrertSomArbeidssøker
import no.nav.dagpenger.regel.OpplysningsTyper.OppyllerKravTilRegistrertArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.RegistrertSomArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.UnntakForArbeidssøkerId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object RegistrertArbeidssøker {
    // Registrert som arbeidssøker
    internal val registrertArbeidssøker =
        boolsk(RegistrertSomArbeidssøkerId, beskrivelse = "Registrert som arbeidssøker", behovId = RegistrertSomArbeidssøker)
    val unntakForArbeidssøker =
        boolsk(UnntakForArbeidssøkerId, beskrivelse = "Har rimelig grunn til å ikke være registrert som arbeidssøker")
    val oppyllerKravTilRegistrertArbeidssøker =
        boolsk(OppyllerKravTilRegistrertArbeidssøkerId, "Registrert som arbeidssøker på søknadstidspunktet", synlig = aldriSynlig)

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 5, "Reelle arbeidssøkere - registrert som arbeidssøker", "Registrert som arbeidssøker")) {
            skalVurderes { it.har(kravTilAlder) }

            regel(registrertArbeidssøker) { innhentMed(prøvingsdato) }
            regel(unntakForArbeidssøker) { somUtgangspunkt(false) }
            utfall(oppyllerKravTilRegistrertArbeidssøker) { enAv(registrertArbeidssøker, unntakForArbeidssøker) }

            påvirkerResultat { it.har(kravTilAlder) }
        }

    val IkkeRegistrertSomArbeidsøkerKontroll =
        Kontrollpunkt(IkkeRegistrertSomArbeidsøker) {
            it.har(oppyllerKravTilRegistrertArbeidssøker) && it.finnOpplysning(oppyllerKravTilRegistrertArbeidssøker).verdi == false
        }
}

package no.nav.dagpenger.regel.regelsett.vilkår
import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål.Register
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.regel.Avklaringspunkter.IkkeRegistrertSomArbeidsøker
import no.nav.dagpenger.regel.Behov.RegistrertSomArbeidssøker
import no.nav.dagpenger.regel.OpplysningsTyper.OppyllerKravTilRegistrertArbeidssøkerId
import no.nav.dagpenger.regel.OpplysningsTyper.RegistrertSomArbeidssøkerId
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalEksportVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.tidligsteVurderingsdato

object RegistrertArbeidssøker {
    // Registrert som arbeidssøker
    val registrertArbeidssøker =
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

            regel(registrertArbeidssøker) { innhentMed(tidligsteVurderingsdato) }
            utfall(oppyllerKravTilRegistrertArbeidssøker) { alle(registrertArbeidssøker) }

            påvirkerResultat { it.har(kravTilAlder) && !it.erSann(skalEksportVurderes) }
        }

    val IkkeRegistrertSomArbeidsøkerKontroll =
        Kontrollpunkt(IkkeRegistrertSomArbeidsøker) {
            it.har(oppyllerKravTilRegistrertArbeidssøker) && it.finnOpplysning(oppyllerKravTilRegistrertArbeidssøker).verdi == false
        }
}

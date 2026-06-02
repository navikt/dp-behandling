package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.OpplysningsTyper.AldersgrenseId
import no.nav.dagpenger.regel.OpplysningsTyper.FødselsdatoId
import no.nav.dagpenger.regel.OpplysningsTyper.KravTilAlderId
import no.nav.dagpenger.regel.OpplysningsTyper.SisteDagIMånedId
import no.nav.dagpenger.regel.OpplysningsTyper.SisteMånedId
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadsdato

object Alderskrav {
    val fødselsdato = dato(FødselsdatoId, "Fødselsdato", Opplysningsformål.Bruker)

    private val aldersgrense = heltall(AldersgrenseId, "Aldersgrense", synlig = aldriSynlig, enhet = Enhet.År)
    private val sisteMåned = dato(SisteMånedId, "Dato søker når maks alder", synlig = aldriSynlig)
    val sisteDagIMåned = dato(SisteDagIMånedId, "Siste mulige dag bruker kan oppfylle alderskrav")

    val kravTilAlder = boolsk(KravTilAlderId, "Oppfyller kravet til alder")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 23, "Bortfall på grunn av alder", "Alder"),
        ) {
            regel(fødselsdato) { innhentes }
            regel(aldersgrense) { somUtgangspunkt(67) }
            regel(sisteMåned) { leggTilÅr(fødselsdato, aldersgrense) }
            regel(sisteDagIMåned) { sisteDagIMåned(sisteMåned) }
            utfall(kravTilAlder) { førEllerLik(prøvingsdato, sisteDagIMåned) }
        }

    val MuligGjenopptakKontroll =
        Kontrollpunkt(Avklaringspunkter.MuligGjenopptak) { it.har(kravTilAlder) && it.finnOpplysning(kravTilAlder).verdi }

    val HattLukkedeSakerSiste8UkerKontroll =
        Kontrollpunkt(Avklaringspunkter.HattLukkedeSakerSiste8Uker) { it.har(kravTilAlder) && it.finnOpplysning(kravTilAlder).verdi }

    val Under18Kontroll =
        Kontrollpunkt(Avklaringspunkter.BrukerUnder18) {
            if (it.mangler(fødselsdato) || it.mangler(søknadsdato)) {
                return@Kontrollpunkt false
            }
            val søknadsdato = it.finnOpplysning(søknadsdato).verdi
            val fødselsdato = it.finnOpplysning(fødselsdato).verdi

            søknadsdato.minusYears(18).isBefore(fødselsdato)
        }

    val TilleggsopplysningsKontroll = Kontrollpunkt(Avklaringspunkter.HarTilleggsopplysninger) { it.har(søknadIdOpplysningstype) }

    val MuligForGammel =
        Kontrollpunkt(
            Avklaringkode(
                kode = "StansAlder",
                tittel = "Bruker er over alderskravet på dagpenger",
                beskrivelse =
                    """
                    Bruker oppfyller ikke '${kravTilAlder.navn}' i løpet av meldeperioden, vurder stans av dagpenger. 
                    """.trimIndent(),
            ),
        ) { opplysninger ->
            val meldeperiode = opplysninger.finnOpplysning(Beregning.meldeperiode).verdi
            return@Kontrollpunkt opplysninger.har(kravTilAlder, gjelderFor = meldeperiode.tilOgMed)
        }
}
